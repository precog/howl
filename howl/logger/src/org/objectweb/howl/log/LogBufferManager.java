/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */
package org.objectweb.howl.log;


import org.objectweb.howl.log.LogRecordSizeException;

import java.io.IOException;

import java.lang.InterruptedException;

/**
 * Provides a generalized buffer manager for journals and loggers.
 *
 * <p>log records are written to disk as blocks
 * of data.  Block size is a multiple of 512 bytes
 * to assure optimum disk performance.
 */
class LogBufferManager
{
  /**
   * mutex for synchronizing access to buffers list
   */
  private final Object bufferManagerLock = new Object();

  /**
   * mutex for synchronizing writes to log
   */
  private final Object forceManagerLock = new Object();
  
  /**
   * mutex for synchronizing BSN increment
   */
  private final Object bsnManagerLock = new Object();
  
  /**
   * reference to LogFileManager that owns this Buffer Manager instance.
   * 
   * @see LogFileManager#getLogFile(LogBuffer)
   */
  private LogFileManager lfm = null;
  
  /**
   * The LogBuffer that is currently being filled.
   */
  private LogBuffer fillBuffer = null;
  
  /**
   * array of LogBuffer objects available for filling
   */
  private LogBuffer[] freeBuffer = null;

  /**
   * index into freeBuffer list maintained in getBuffer.
   */
  short nextIndex = 0;
  
  /**
   * buffer size specified by log -- default 4kb
   */
  private int bufferSize = 4;

  /**
   * number of buffers in pool
   */
  short bufferPoolSize = 2;

  /**
   * time interval between log force()
   */
  private int flushSleepTime = 50;

  // maximum size of a callers data record
  private int maxRecordSize = 0; // initialized in open()

  /**
   * number of log Write forces
   */
  private long forceCounter = 0;

  /**
   * number of times there were no buffers available
   */
  private long waitForBuffer = 0;
  
  /**
   * number of times any task waited for BSN to increment
   */
  private long waitForBSN = 0;

  /**
   * number of times buffer was forced because it is full
   */
  private long noRoomInBuffer = 0;

  /**
   * number of fatal errors in buffer management logic
   */
  private int bufferManagerError = 0;

  /**
   * number of times fillBuffer forced for timeout
   */
  private long flushOnTimeout = 0;

  /**
   * next block sequence number for fillBuffer
   */
  int nextFillBSN = 0;

  /**
   * next BSN to be written to log
   */
  volatile int nextWriteBSN = 1;
  
  /**
   * last BSN forced to log
   */
  volatile int lastForceBSN = 0;
  
  /**
   * number of active threads waiting for BSN increment
   */
  volatile short waitingForBSN = 0;
  
  /**
   * number of times force() called
   */
  private long forceCount = 0;
  
  /**
   * number of threads waiting for a force
   */
  private int threadsWaitingForce = 0;
  private int maxThreadsWaitingForce = 0;
  private long totalThreadsWaitingForce = 0;
  int threadsWaitingForceThreshold = Integer.MAX_VALUE;
  
  // reasons for doing force
  long forceNoWaitingThreads = 0;
  long forceHalfOfBuffers = 0;
  long forceMaxWaitingThreads = 0;



  /**
   * thread used to flush long waiting buffers
   */
  Thread flushManager = null;
  
  /**
   * name of LogBuffer implementation used by this LogBufferManager instance.
   */
  String bufferClassName = null;
  
  LogBufferManager(LogFileManager lfm)
  {
    assert lfm != null;
    
    this.lfm = lfm;
  }

  /**
   * forces buffer to disk.
   *
   * <p>batches multiple buffers into a single force
   * when possible.
   */
  private void force(LogBuffer buffer)
    throws IOException, InterruptedException
  {
    // make sure buffers are written in ascending BSN sequence
    synchronized(bsnManagerLock)
    {
      if (buffer.bsn != nextWriteBSN)
      {
        ++waitingForBSN;
        while (buffer.bsn != nextWriteBSN)
        {
          ++waitForBSN;
          bsnManagerLock.wait();
        }
        --waitingForBSN;
      }
    }

    // write the buffer to disk (hopefully non-blocking)
    try
    {
      buffer.write(false);  
    }
    catch (IOException ioe)
    {
      // ignore for now. buffer.iostatus is checked later for errors.
    }
    
    synchronized(bsnManagerLock)
    {
      ++nextWriteBSN;
      bsnManagerLock.notifyAll();
    }
    
    threadsWaitingForce += buffer.getWaitingThreads();
    if (threadsWaitingForce > maxThreadsWaitingForce)
      maxThreadsWaitingForce = threadsWaitingForce;

    /*
     * if there are buffers waitingForBSN then we
     * wait for them to complete their writes
     * before we force.
     * 
     * The lastForceBSN member is updated by the thread
     * that actually does a force().  All threads
     * waiting for the force will detect the change
     * in lastForceBsn and notify any waiting threads.
     */
    synchronized(forceManagerLock)
    {
      boolean doforce = true;
      if (buffer.bsn < lastForceBSN)
      {
        doforce = false;
      }
      else if (waitingForBSN == 0)
      {
        // no other threads waiting so force now
        ++forceNoWaitingThreads;
      }
      else if ((buffer.bsn - lastForceBSN) > (freeBuffer.length/2))
      {
        // one half of the buffers are waiting on the force
        ++forceHalfOfBuffers;
      }
      else if (threadsWaitingForce > threadsWaitingForceThreshold)
      {
        // number of waiting threads exceeds configured limit
        ++forceMaxWaitingThreads;
      }
      else
      {
        doforce = false;
      }
      
      if (doforce)
      {
        // force() is guaranteed to have forced everything that
        // has been written prior to the force, so get the
        // bsn for the last known write prior to the force
        int forcebsn = 0;
        synchronized(bsnManagerLock) { forcebsn = nextWriteBSN - 1; }
        
        ++forceCount;
        buffer.lf.force(false);
        
        lastForceBSN = forcebsn;
        totalThreadsWaitingForce += threadsWaitingForce;
        threadsWaitingForce = 0;
        forceManagerLock.notifyAll();
      }

      // wait for write to be forced
      while (lastForceBSN < buffer.bsn)
      {
        forceManagerLock.wait();
      }
     }
      
    if (buffer.iostatus == LogBufferStatus.WRITING)
      buffer.iostatus = LogBufferStatus.COMPLETE;
    
    // notify threads waiting for this buffer to force
    synchronized(buffer) { buffer.notifyAll(); }

    releaseBuffer(buffer);
  }
  /**
   * Waits for buffer to be forced to disk.
   *
   * <p>no monitors are owned when routine is entered.
   */
  private void sync(LogBuffer buffer)
    throws IOException, InterruptedException
  {
    try
    {
      buffer.sync();
    }
    finally
    {
      releaseBuffer(buffer);
    }
  }

  /**
   * decrements count of waiting threads and returns buffer
   * to freeBuffer list if count goes to zero 
   * @param buffer LogBuffer to be released
   */
  private void releaseBuffer(LogBuffer buffer)
  {
    if (buffer.release() == 0)
    {
      synchronized(bufferManagerLock)
      {
        freeBuffer[buffer.index] = buffer;
        bufferManagerLock.notifyAll();
      }
    }
  }
  
  /**
   * returns a LogBuffer to be filled.
   * 
   * <p>PRECONDITION: caller holds bufferManagerLock monitor.
   * 
   * @return a LogBuffer to be filled.
   */
  private LogBuffer getFillBuffer() throws LogFileOverflowException
  {
    if (fillBuffer == null) // slight optimization when fillBuffer != null
    {
      int fbl = freeBuffer.length;
      for(int i=0; fillBuffer == null && i < fbl; ++i)
      {
        nextIndex %= fbl;
        if (freeBuffer[nextIndex] != null)
        {
          LogBuffer b = freeBuffer[nextIndex];
          freeBuffer[nextIndex] = null;
          fillBuffer = b.init(++nextFillBSN, lfm);
        }
        ++nextIndex;
      }
    }
    return fillBuffer;
  }

  /**
   * writes byte[] to log.
   * <p>waits for IO to complete if sync is true.
   *
   * @return token reference to block
   * @throws LogClosedException
   *   if the log has been closed
   * @throws LogRecordSizeException
   *   when size of byte[] is larger than the maximum possible
   *   record for the configured buffer size. 
   */
  long put(short type, byte[] data, boolean sync)
    throws LogClosedException, LogRecordSizeException, LogFileOverflowException, 
                InterruptedException, IOException
  {
    long token = 0;
    LogBuffer currentBuffer = null;

    // make sure data fits into a buffer
    if (data.length > maxRecordSize)
      throw new LogRecordSizeException();

    do {
      // allocate the current fillBuffer
      synchronized(bufferManagerLock)
      {
        while((currentBuffer = getFillBuffer()) == null)
        {
          ++waitForBuffer;
          bufferManagerLock.wait();
        }
        
        token = currentBuffer.put(type, data, sync);
        if (token == 0)
        {
          // buffer is full -- make it unavailable to other threads until force() completes
          fillBuffer = null;
        }
      }

      if (token == 0)
      {
        // force current buffer if there was no room for data
        ++noRoomInBuffer;
        force(currentBuffer);
        if (currentBuffer.iostatus == LogBufferStatus.ERROR)
          throw currentBuffer.ioexception;
      }
      else if (sync)  // otherwise sync as requested by caller
      {
        sync(currentBuffer);
      }

    } while (token == 0);

    return token;
  }

  /**
   * opens designated log file and allocates IO buffers.
   * 
   * TODO: move open and close to the Logger class
   */
  void open()
    throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException
  {
    configure();
    
    Class lbcls = null;
    lbcls = this.getClass().getClassLoader().loadClass(bufferClassName);

    freeBuffer = new LogBuffer[bufferPoolSize];
    for (short i=0; i< bufferPoolSize; ++i)
    {
      // freeBuffer[i] = new BlockLogBuffer(bufferSize, i);
      freeBuffer[i] = (LogBuffer)lbcls.newInstance();
      freeBuffer[i].config(bufferSize, i);
    }

    try
    {
      fillBuffer = getFillBuffer();
    }
    catch (LogFileOverflowException e)
    {
      // should not happen on open so ignore it for now

      // QUESTION: can this happen if we are doing recovery?
      
      // QUESTION: do we need a separate openForRecovery or open(mode) for read and rw?
    }
    assert fillBuffer != null : "open(): unexpected null pointer returned by getFillBuffer()";
    assert fillBuffer.buffer != null : "open(): null ByteBuffer pointer; fillBuffer.buffer";
    
    // remember maximum record size so put() can reject records that
    // are too big without causing a BufferOverflowException
    maxRecordSize = fillBuffer.buffer.remaining();

    // start a thread to flush buffers that have been waiting more than 50 ms.
    flushManager = new FlushManager("FlushManager");
    flushManager.start();

  }

  /**
   * flush active buffers to disk and shut down the FlushManager
   * thread.
   */
  void stop() throws IOException
  {
    try
    {
      // wait until all buffers are returned to the freeBuffer pool
      for (int i=0; i < freeBuffer.length; ++i)
      {
        synchronized(bufferManagerLock)
        {
          while(freeBuffer[i] == null)
          {
            bufferManagerLock.wait();
          }
        }
      }

    }
    catch (InterruptedException e)
    {
      ; // ignore it
    }

    // shutdown the flush manager
    if (flushManager != null)
    {
      flushManager.interrupt();
    }
  }

  /**
   * Returns an XML node containing statistics for the LogBufferManager.
   * <p>The nested <LogBufferPool> element contains entries for each
   * LogBuffer object in the buffer pool.
   * 
   * @return a String containing statistics.
   */
  String getStats()
  {
    double avgThreadsWaitingForce = (totalThreadsWaitingForce / (double)forceCount);
    String name = this.getClass().getName();
    StringBuffer stats = new StringBuffer(
           "\n<LogBufferManager  class='" + name + "'>" + 
           "\n  <poolsize    value='" + freeBuffer.length + "'></poolsize>" + 
           "\n  <bufferwait  value='" + waitForBuffer     + "'>Wait for available buffer</bufferwait>" +
           "\n  <bsnwait     value='" + waitForBSN        + "'>Waits for BSN</bsnwait>" +
           "\n  <forcecount  value='" + forceCount        + "'>Number of force() calls</forcecount>" +
           "\n  <bufferfull  value='" + noRoomInBuffer    + "'>Buffer full</bufferfull>" + 
           "\n  <nextfillbsn value='" + nextFillBSN       + "'></nextfillbsn>" +
           "\n  <forceOnTimeout value='" + flushOnTimeout + "'></forceOnTimeout>" +
           "\n  <forceNoWaitingThreads value='" + forceNoWaitingThreads + "'>force because no other trheads waiting on force</forceNoWaitingThreads>" +
           "\n  <forceHalfOfBuffers value='" + forceHalfOfBuffers + "'>force due to 1/2 of buffers waiting</forceHalfOfBuffers>" +
           "\n  <forceMaxWaitingThreads value='" + forceMaxWaitingThreads + "'>force due to max waiting threads</forceMaxWaitingThreads>" +
           "\n  <maxThreadsWaitingForce value='" + maxThreadsWaitingForce + "'>maximum threads waiting</maxThreadsWaitingForce>" +
           "\n  <avgThreadsWaitingForce value='" + avgThreadsWaitingForce + "'>Avg threads waiting force</avgThreadsWaitingForce>" +
           "\n  <LogBufferPool>" +
           "\n"
         );
    
    for (int i=0; i < freeBuffer.length; ++i)
      stats.append(freeBuffer[i].getStats());

    stats.append(
         "\n</LogBufferPool>" +
         "\n</LogBufferManager>" +
      "\n"
    );
     
     return stats.toString();
  }

  /**
   * configures the log
   */
  void configure()
  {
    // TODO: place code to read configuration file here

    // override default values with commandline -Dx=y values
    flushSleepTime = Integer.getInteger("howl.log.flushSleepTime",flushSleepTime).intValue();
    bufferSize = Integer.getInteger("howl.log.bufferSize",bufferSize).intValue() * 1024;
    bufferPoolSize = Integer.getInteger("howl.log.bufferPoolSize",bufferPoolSize).shortValue();
    threadsWaitingForceThreshold = Integer.getInteger("howl.log.maxWaitingThreads",threadsWaitingForceThreshold).intValue();
    bufferClassName = System.getProperty("howl.LogBuffer.class", "org.objectweb.howl.log.LogException");
  }

  /**
   * helper thread to flush buffers that have threads waiting
   * longer than configured maximum.
   */
  class FlushManager extends Thread
  {
    FlushManager(String name)
    {
      super(name);
    }

    public void run()
    {
      LogBuffer buffer = null;

      for (;;)
      {
        if (interrupted()) return;

        try
        {
          sleep(flushSleepTime); // check for timeout every 50 ms

          synchronized(bufferManagerLock)
          {
            buffer = fillBuffer;
            if (buffer != null && buffer.shouldForce())
            {
              ++flushOnTimeout;
              fillBuffer = null;
            }
            else
              buffer = null;
          }

          if (buffer != null)
          {
              force(buffer);
          }

        }
        catch (InterruptedException e)
        {
          return;
        }
        catch (IOException e)
        {
          ; // TODO: report IOException to error log
        }
      }
    }
  }

}
