/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */
package org.objectweb.howl.log;

import java.io.IOException;

import java.nio.ByteBuffer;

/**
 * Classes used as buffers in LogBufferManager must implement this interface.
 * 
 * <p>This abstract class implements methods common to all LogBuffer sub-classes.
 */
abstract class LogBuffer
{
  /**
   * 
   */
  ByteBuffer buffer = null;

  /**
   * buffer number used by owner to index into an array of buffers
   * or possibly some other purpose.
   */
  short index = 0;

  /**
   * number of waiting threads.
   */
  volatile int waitingThreads = 0;

  /**
   * results of last write.
   * <p>Value must be one of the constants defined in LogBuffer interface.
   */
  int iostatus = 0;
  
  /**
   * buffer sequence number.
   * <p>LogBufferManager maintains a sequence number
   * of buffers written. The sequence number is stored
   * in the block header of each log block. 
   */
  int bsn = 0;
  
  /**
   * set true if this LogBuffer should issue a rewind on the FileChannel before
   * writing ByteBuffer.
   */
  boolean rewind = false;
  
  /**
   * IOException from last write
   */
  IOException ioexception = null;
  
  /**
   * name of this buffer object.
   */
  String name = null;
  
  /**
   * LogFile associated with the current LogBuffer.
   * 
   * <p>The LogBufferManager will have a pool of LogBuffers.  If the containing
   * Logger is managing a pool of files, then it is possible
   * that some period of time, some buffers will be written to one file, while other 
   * buffers are written to another file.  To allow writes and forces to separate
   * files to be performed in parallel, each LogBuffer must keep track of its own
   * LogFile.
   * 
   * @see LogFileManager#getLogFile
   */
  LogFile lf = null;
  
/**
   * default constructor.
   * <p>after creating a new instance of LogBuffer the caller must
   * invoke config().
   * @see #config
   */
  LogBuffer()
  {
    name = this.getClass().getName();
  }

  /**
   * decrements count of waiting threads and returns updated value.
   *
   * @return number of threads still waiting after the release.
   * @see #put
   */
  final int release()
  {
    synchronized(this)
    {
      return --waitingThreads;
    }
  }
  
  /**
   * sets index and ByteBuffer variables.
   * <p>The LogBufferManager creates a pool of LogBuffer objects.  The
   * class name for the LogBuffer implementation is specified via configuration.
   * The LogBufferManager instantiates LogBuffer objects using
   * Class.newInstance() for the configured class name.  After the instance
   * is created, each LogBuffer instance is configured using the config()
   * method.
   * 
   * TODO: consider using Properties for more generalized configuration.
   * 
   * @param size of the underlying ByteBuffer byte[].
   * @param index into an array of buffers maintained by LogBufferManager.
   */
  final void config(int size, short index)
  {
    this.index = index;
    buffer = ByteBuffer.allocateDirect(size);
  }

  /**
   * returns the number of threads currently waiting for
   * the buffer to be forced to disk.
   * 
   * @return current value of waitingThreads 
   */
  final int getWaitingThreads()
  {
    return waitingThreads;
  }

  /**
   * park threads that are waiting for the ByteBuffer to
   * be forced to disk.
   * <p>The count of waiting threads (<i> waitingThreads </i>)
   * has been incremented in <i> put() </i>.
   * @see #put
   */
  final void sync() throws IOException, InterruptedException
  {
    if (Thread.interrupted()) throw new InterruptedException();

    synchronized(this)
    {
      while (iostatus != LogBufferStatus.COMPLETE)
      {
        if (iostatus == LogBufferStatus.ERROR)
          throw new IOException();
        wait();
      }
    }
  }

  /**
   * May be used in traces or other purposes for debugging.
   * 
   * @return name of LogBuffer object.
   */
  String getName()
  {
    return "(" + name + ")";
  }
  
    /**
   * initialize members for LogBuffer implementation class for reuse.
   * <p>LogBufferManager maintains a pool of LogBuffer objects. Each
   * time a LogBuffer is allocated from the pool for use as the current
   * collection buffer, the init() routine is called.  After performing necessary
   * initialization, the LogBuffer invokes the LogFileManager to obtain
   * a LogFile for use when writing and forcing the buffer.  If a file
   * switch occurrs, the LogFileManager will store a file header record
   * into this newly initialized buffer.
   * 
   * @param bsn Logical Block Sequence Number of the buffer.
   * LogBufferManager maintains a block sequence number
   * to ensure correct order of writes to disk.  Some implementations
   * of LogBuffer may include the BSN as part of a record or
   * block header.
   * 
   * @param lfm LogFileMaager to call to obtain a LogFile.
   * 
   * @return this LogBuffer
   * 
   */ 
  abstract LogBuffer init(int bsn, LogFileManager lfm) throws LogFileOverflowException;

  /**
   * returns <b>true</b> if the buffer should be forced to disk.
   * <p>The criteria for determining if a buffer should be
   * forced is implementation dependent.  Typically, this 
   * method will return true if there are one or more threads
   * waiting for the buffer to be forced and the amount
   * of time the threads has been waiting has been longer
   * than some implementation defined value.
   *
   * @return true if buffer should be forced immediately.
   */ 
  abstract boolean shouldForce();

  /**
   * puts a data record into the buffer and returns a token for record.
   * <p>PRECONDITION: caller holds a bufferManager monitor.
   * <p>The caller must set the sync parameter true if the thread
   * will ultimately call sync() after a successful put().
   * This strategy allows the waitingThreads counter to be
   * incremented while the current thread holds the bufferManager
   * monitor.
   * <p>Implementations should return a token that can be used
   * later for replay, and for debugging purposes.
   * 
   * @param type short containing implementation defined record
   * type information.
   * @param data byte[] to be written to log
   * @param sync true if thread will call sync following the put.
   * Causes count of waitingThreads to be incremented.
   *
   * @return 0 if no room in buffer for record, otherwise a long
   * that contains the physical position of the record is returned.
   * The value returned by put() is an encoding of the physical 
   * position.  The format of the returned value is implementation
   * specific, and should be treated as opaque by the caller.
   */
  abstract long put(short type, byte[] data, boolean sync);

  /**
   * write ByteBuffer to the LogFile.
   *
   * <p>updates the buffer header with the number
   * of bytes used. Based on configuration, some implementations
   * may compute a hash code or other integrety value and
   * include the value in some implementation defined header
   * information. 
   * <p>The buffer is written using the LogFile.write() method
   * to allow the LogFile to manage file position for circular
   * journals.
   * <p>if the <i> force </i> parameter is true then the
   * LogFile.force() method is called to synchronize
   * data to disk.  When the force completes, all threads
   * waiting in the sync() method are notified.
   * <p>If the <i> force </i> parameter is false, then
   * forcing and notification of waiting threads is
   * the responsibility of the LogBufferManager that owns this LogBUffer.
   * 
   * @param force true if FileChannel.force() should be invoked.
   * @throws IOException
   *          rethrows any IOExceptions thrown by FileChannel methods.
   *
   * <p>QUESTION: should waiters be interupted if IO Error occurs?
   * @see #init(int, LogFileManager)
   * @see LogFile#write
   */
  abstract void write(boolean force) throws IOException;
  
  /**
   * returns statistics for this LogBuffer object.
   * 
   * <p>information is returned in the form of an XML node that
   * can be included as a nested element in a larger document
   * containing stats for multiple LogBuffer objects, and any
   * containing objects.
   * 
   * @return statistics for this buffer as XML
   */
  abstract String getStats();

}