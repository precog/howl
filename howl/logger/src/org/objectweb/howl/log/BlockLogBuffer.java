/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */
package org.objectweb.howl.log;

import org.objectweb.howl.log.LogBufferStatus;

import java.io.IOException;

/**
 * An abstract buffer that wraps ByteBuffer
 * for use by LogBufferManager.
 *
 * <p>Each block contains a header, zero or more log records,
 * and a footer.  The header and footer contain enough
 * information to allow recovery operations to validate
 * the integrity of each log block.
 */
class BlockLogBuffer extends LogBuffer
{
  /**
   * currentTimeMillis that buffer was initialized.
   * <p>Used during replay situations to validate block integrity.
   * The TOD field from the block header is compared with the TOD field
   * of the block footer to verify that an entire block was written.
   */
  long tod = 0;

  /**
   * currentTimeMillis that last record was added.
   * <p>This field is used by shouldForce() to determine if the buffer
   * should be forced.
   */
  long todPut = 0;
  
  /**
   * number of times this buffer was used.
   * <p>In general, should be about the same for all buffers in a pool.
   */
  int initCounter = 0;

  /* buffer Header format
   * byte header_ID                     [4] "HOWL"
   * int  block_sequence_number  [4]
   * int  block_size                       [4] in bytes
   * int  bytes used                      [4]
   * int hashCode                        [4] 
   * long  currentTimeMillis          [8]
   * byte crlf                               [2] to make it easier to read buffers in an editor
   */
  
  /**
   * Offset within the block header of the bytes_used field.
   */
  private int bytesUsedOffset = 0;

  /**
   * The number of bytes to reserve for block footer information.
   */
  private int bufferFooterSize = 14;
  /* 
   * byte footer_ID             [4] "LOWH"
   * long currentTimeMillis     [8] same value as header
   * byte crlf                  [2] to make it easier to read buffers in an editor
   */

  /**
   * Size of the header for each data record in the block.
   * <p>Record header is an short containing the length of data.
   */
  private int recordHeaderSize = 4;

  // block header & footer fields
  /**
   * Carriage Return Line Feed sequence used for debugging purposes.
   */
  private byte[] crlf      = "\r\n".getBytes();
  
  /**
   * Signature for each logical block header.
   */
  private byte[] header_ID = "HOWL".getBytes();
  
  /**
   * Signature for each logical block footer.
   */
  private byte[] footer_ID = "LWOH".getBytes();
  
  /**
   * switch to disable writes.
   * <p>Used to measure performance of implementation sans physical writes.
   * Subclass defines <i> doWrite </i> to be false to eliminate IO.
   */
  boolean doWrite = true;

  /**
   * default constructor calls super class constructor.
   */
  BlockLogBuffer()
  {
    super();
  }
  
  /**
   * constructs instance of BlockLogBuffer with file IO disabled.
   * 
   * <p>use this constructor when doing performance measurements
   * on the implementation sans file IO.
   * 
   * @param doWrite
   */
  BlockLogBuffer(boolean doWrite)
  {
    super();
    this.doWrite = doWrite;
  }

  /**
   * adds a data record to the buffer and returns a log key for record.
   * 
   * <p>PRECONDITION: caller holds bufferManager monitor.
   * 
   * <p>The caller must set the sync parameter true if the thread will
   * call sync() after a successful put().  This strategy allows
   * the waitingThreads counter to be incremented while the
   * current thread holds the bufferManager monitor.
   * 
   * <p>The log key returned by <i> put() </i> is an opaque value
   * used by the application to maintain links between related records.
   * The key allows the record to be located within the log and
   * retrieved directly using the key if needed.
   * For example, applications that wish
   * to maintain a linked list of related records may include the
   * log key as part of the data records to form a backward link
   * to the prior record.
   * 
   * <p>The buffer manager and file managers within this package
   * use the key to manage the log files.
   * 
   * @param data byte[] to be written to log
   * @param sync true if thread will call sync following the put.
   * Causes count of waitingThreads to be incremented.
   *
   * @return 0 if no room in buffer for record.
   */
  long put(short type, byte[] data, boolean sync)
  {
    long logKey = 0;
    synchronized(buffer)
    {
      if ((recordHeaderSize + data.length) <= buffer.remaining()) 
      {
        // first 8 bits available to Logger -- possibly to carry file rotation number
        logKey = (bsn << 24) + buffer.position();
  
        // put a new record into the buffer
        buffer.putShort(type).putShort((short)data.length).put(data);
        todPut = System.currentTimeMillis();
        
        if (sync) ++waitingThreads;
      }
    }

    return logKey;
  }

  /**
   * write ByteBuffer to the log file.
   */
  void write( boolean force) throws IOException
  {
    assert lf != null: "LogFile lf is null";
    
    synchronized(this)
    {
      // guard against gating errors that might allow
      // multiple threads to be write this buffer.
      if (iostatus == LogBufferStatus.WRITING)
        throw new IOException();

      // increment count of threads waiting for IO to complete
      ++waitingThreads;
    }

    // Update bytesUsed in the buffer header
    buffer.putInt(bytesUsedOffset, buffer.position());
    
    // TODO: Update checksum if option configured

    try
    {
      iostatus = LogBufferStatus.WRITING;
      buffer.clear();
      if (doWrite) lf.write(this);
      if (force)
      {
        lf.force(false);
        iostatus = LogBufferStatus.COMPLETE;
      }
    }
    catch (IOException e)
    {
      ioexception = e;
      iostatus = LogBufferStatus.ERROR;
      throw e;
    }
    finally
    {
      // notify waiting threads that force is done
      if (force) synchronized(this) { notifyAll(); }
    }
  }

  /**
   * initialize members for buffer reuse.
   * 
   * @param bsn Logic Block Sequence Number of the buffer.
   * LogBufferManager maintains a list of block sequence numbers
   * to ensure correct order of writes to disk.  Some implementations
   * of LogBuffer may include the BSN as part of a record or
   * block header.
   */ 
  LogBuffer init(int bsn, LogFileManager lfm) throws LogFileOverflowException
  {
    this.bsn = bsn;
    
    tod = todPut = System.currentTimeMillis();
    iostatus = LogBufferStatus.FILLING;
    
    ++initCounter;

    // initialize the logical block footer
    int bufferSize = buffer.capacity();

    buffer.clear();
    buffer.position(bufferSize - bufferFooterSize);
    buffer.put(footer_ID).putLong(tod).put(crlf);
    
    // initialize the logical block header
    buffer.clear();
    buffer.put(header_ID);
    buffer.putInt(bsn);
    buffer.putInt(bufferSize);
    
    bytesUsedOffset = buffer.position();

    buffer.putInt( 0 ); // bytes used
    buffer.putLong( tod );
    buffer.put(crlf);
    
    // reserve room for buffer footer, and one record header
    buffer.limit(bufferSize - bufferFooterSize - recordHeaderSize);
    
    // obtain LogFile from the LogFileManager
    // LogFileManager will put a header record into this buffer if LogFile switch occurs
    // so we must make this call after all other initialization is complete.
    lf = lfm.getLogFile(this);
    assert lf != null: "LogFileManager returned null LogFile pointer";

    return this;
  }

  /**
   * determines if buffer should be forced to disk.
   * <p>If there are any waiting threads, then buffer
   * is forced when it is 50 ms old.  Otherwise, if there
   * are no waiting threads, we wait 1 second before we
   * force.
   *
   * @return true if buffer has waiting threads older than 50 ms
   */ 
  boolean shouldForce()
  {
    int forceDelta = waitingThreads > 0 ? 50 : 1000;
    long now = System.currentTimeMillis();

    return ((todPut + forceDelta) < now);
  }
  
  /**
   * return statistics for this buffer.
   * 
   * @return String containing statistics as an XML node
   */
  String getStats()
  {
    String name = this.getClass().getName();
    
    String result = "<LogBuffer class='" + name + "' index='" + index + "'>" +
      "\n  <timesUsed value='" + initCounter + "'>Number of times this buffer was initialized for use</timesUsed>" +
      "\n</LogBuffer>" +
      "\n";
    
    return result;
  }
  
}