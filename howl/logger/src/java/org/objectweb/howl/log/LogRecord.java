/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 * Created on Apr 7, 2004
 */
package org.objectweb.howl.log;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * LogRecord class used by Logger.replay().
 * 
 * <p>This class may be extended by applications to provide
 * Java Bean mappings for application data fields within the
 * record.
 * 
 * @author Michael Giroux
 */
public final class LogRecord
{
  /**
   * type of data record.
   * <p>USER data records have a type == 0.
   * <p>Logger control record types are defined by LogRecordType.
   * @see LogRecordType
   */
  public short type = 0;
  
  /**
   * length of the data record.
   */
  public short length = 0;
  
  /**
   * log key associated with this LogRecord.
   */
  public long key = 0;
  
  /**
   * byte[] containing record data.
   */
  public byte[] data = null;
  
  /**
   * currentTimeMillis the log buffer containing this record was initialized.
   * 
   * <p>LogBuffers normally flush to disk in something less than 50 ms, so
   * this <i> tod </i> should be a pretty close approximation of the time
   * the record was generated. 
   */
  public long tod;
  
  /**
   * ByteBuffer wrapper for the data byte[].
   */
  public ByteBuffer dataBuffer = null;
  
  /**
   * constructs an instance of LogRecord with a byte[]
   * of <i> size </i> data.
   * 
   * @param size initial size of data buffer.
   * <p>the get() method will reallocate the data buffer
   * to accomdate larger records.
   */
  public LogRecord(int size)
  {
    data = new byte[size];
    dataBuffer = ByteBuffer.wrap(data);
  }
  
  /**
   * Return true if current record is an EOB type control record.
   * @return true if this record type is an EOB type.
   */
  public boolean isEOB()
  {
    return (type == LogRecordType.EOB);
  }
  
  /**
   * Return true if the current record is a control record.
   * 
   * @return true if this record type has LogRecordType.CTRL set.
   */
  public boolean isCTRL()
  {
    return (type & LogRecordType.CTRL) != 0;
  }

  /**
   * @return length of the byte[] that backs the ByteBuffer.
   */
  public final int capacity() { return data.length; }
  
  /**
   * protected method to
   * copy next logical record from the LogBuffer specified by the
   * callers <i> lb </i> parameter.
   * 
   * <p>Following the call to get()
   * the number of data bytes transferred into this LogRecord's
   * data buffer is available in
   * LogRecord.length. The LogRecord.dataBuffer.limit is also set
   * to the number of bytes transferred.
   * <p>Sets LogRecord.type to LogRecordType.EOB if the position of this
   * LogBuffer is at or beyond bytes used.
   * <p>Sets the limit of this LogRecord to the number of bytes in the logical
   * record being retreived.
   * <p>LogBuffer.position() is unchanged if any exception is thrown.
   * 
   * @param lb LogBuffer to get the next logical record from.
   * @return this LogRecord.
   * @throws InvalidLogBufferException
   * if the size of the data record exceeds the bytes used for the buffer.
   * @see LogRecordType
   */
  final LogRecord get(LogBuffer lb) throws InvalidLogBufferException
  {
    short type = 0;
    short length = 0;
    long key = 0;
    ByteBuffer buffer = lb.buffer;
    
    if (buffer.position() < lb.bytesUsed)
    {
      // save current record position so we can reset on errors
      buffer.mark();
      long logKey = ((long) lb.bsn << 24) | (buffer.position() & 0xffffff ) ;

      try {
        type = buffer.getShort();
        length  = buffer.getShort();
      } catch (BufferUnderflowException e) {
        buffer.reset();
        throw new InvalidLogBufferException();
      }
      if (buffer.position() + length > lb.bytesUsed) {
        buffer.reset();
        throw new InvalidLogBufferException();
      }
      
      if (length > data.length) {
        // reallocate buffer to accomodate record
        data = new byte[length];
        dataBuffer = ByteBuffer.wrap(data);
        dataBuffer.clear();
      }
      
      if (length > 0)
        buffer.get(data, 0, length);
      key = logKey;   
    }
    else
    {
      // no data left in this buffer
      type = LogRecordType.EOB;
      
      // set key to first record in next block 
      key = ((long) (lb.bsn + 1) << 24); 
    }
    
    this.type = type;
    this.length = length;
    this.key = key;
    this.tod = lb.tod;
    dataBuffer.clear().limit(length);
    
    return this;
  }

}
