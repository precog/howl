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
abstract class LogBuffer extends LogObject
{
  /**
   * 
   */
  ByteBuffer buffer = null;

  /**
   * buffer number used by owner (LogBufferManager) to index into an array of buffers.
   * 
   * <p>Actual use of <i> index </i> is determined by the buffer manager implementation.
   */
  int index = -1;

  /**
   * currentTimeMillis that buffer was initialized.
   * 
   * <p>Implementations of LogBuffer should provide some means
   * to persist <i> tod </i> to the file to allow recovery
   * operations to determine when a buffer was written.
   * 
   * <p>Used during replay situations to validate block integrity.
   * The TOD field from the block header is compared with the TOD field
   * of the block footer to verify that an entire block was written.
   */
  long tod = 0;

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
   * <p>Initialized to zero.
   * <p>Set to -1 by read() if bytes read is -1 (end of file) 
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
   * @see org.objectweb.howl.log.LogFileManager#getLogFileForWrite(LogBuffer)
   */
  LogFile lf = null;
  
  /**
   * switch to enable computation of checksum.
   * 
   * <p>Since it takes some CPU to compute checksums over a buffer,
   * it might be useful to disable the checksum, at least for performance
   * measurements.
   * <p>When <i> doChecksum </i> is true then an implementation class
   * should compute a checksum and store the value in the buffer during
   * the write() method.
   * <p>Use of checksums is optional and depends on the actual implementation
   * class. 
   */
  boolean doChecksum = true;
 
  /**
   * Number of used data bytes in the buffer.
   * 
   * <p>This is different than buffer capacity().  The bytes used is the
   * number of bytes of usable data in a buffer.  Bytes between the
   * bytes used count and the buffer footer are undefined, possibly
   * uninitialized or residue.
   * 
   * <p>set by operations that read data from files into the
   * buffer such as read().
   * <p>checked by operations that retrieve logical records
   * from the buffer get().
   */
  int bytesUsed = 0;
  
  /**
   * default constructor.
   * <p>after creating a new instance of LogBuffer the caller must
   * invoke config().
   */
  LogBuffer(Configuration config)
  {
    super(config);  // LogObject 
    name = this.getClass().getName();
    doChecksum = config.isChecksumEnabled();
    buffer = ByteBuffer.allocateDirect(config.getBufferSize());
  }

  /**
   * decrements count of waiting threads and returns updated value.
   *
   * @return number of threads still waiting after the release.
   */
  final int release()
  {
    synchronized(this)
    {
      return --waitingThreads;
    }
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
   * read a block of data from the LogFile object provided
   *  in the <i> lf </i> parameter starting at the position
   * specified in the <i> postiion </i> parameter.
   * 
   * <p>Used by LogFileManager implementations to read
   * blocks of data that are formatted by a specific LogBuffer
   * implementation.
   * 
   * <p>The LogFileManager uses LogBufferManager.getLogBuffer() method to obtain
   * a buffer that is used for reading log files.
   *   
   * @param lf LogFile to read.
   * @param position within the LogFile to be read.
   * 
   * @return this LogBuffer reference.
   * @throws IOException
   * @throws InvalidLogBufferException
   */
  abstract LogBuffer read(LogFile lf, long position) throws IOException, InvalidLogBufferException;
  
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
   * 
   * <p>PRECONDITION: caller holds a bufferManager monitor.
   * 
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
   * @param data byte[][] to be written to log.
   * The arrays are concatenated into a single log record whose size
   * is the sum of the individual array sizes. 
   * During replay the entire record is returned as a single
   * byte[].  The ReplayListener is responsible for
   * decomposing the record into the original array of byte[].
   * @param sync true if thread will call sync following the put.
   * Causes count of waitingThreads to be incremented.
   * 
   * @throws LogRecordSizeException
   * if the sum of all <i> data[] </i> array sizes is larger than
   * the maximum allowed record size for the configured buffer size.
   *
   * @return  a long that contains the physical position of the record is returned.
   * The value returned by put() is an encoding of the physical 
   * position.  The format of the returned value is implementation
   * specific, and should be treated as opaque by the caller.
   * Returns 0 if there is no room for the record in the current buffer.
   */
  abstract long put(short type, byte[][] data, boolean sync) throws LogRecordSizeException;
  
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
   * @see org.objectweb.howl.log.LogFile#write(LogBuffer)
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