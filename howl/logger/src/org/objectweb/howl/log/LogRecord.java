/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 * Created on Apr 7, 2004
 */
package org.objectweb.howl.log;

import java.nio.ByteBuffer;

/**
 * @author Michael Giroux
 */
public class LogRecord
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
   * @param size number of data to allocate for the data buffer.
   */
  public LogRecord(int size)
  {
    data = new byte[size];
    dataBuffer = ByteBuffer.wrap(data);
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
   * <p>Followint the call to get()
   * the number of data bytes transferred into this LogRecords
   * data buffer is available in
   * LogRecord.length. The LogRecord.dataBuffer.limit is also set.
   * <p>Sets LogRecord.type to LogRecordType.EOB if the position of this
   * LogBuffer is at or beyond bytes used.
   * <p>Sets the limit of this LogRecord to the number of bytes in the logical
   * record being retreived.
   * <p>LogBuffer.position() is unchanged if any exception is thrown.
   * 
   * @param lb LogBuffer to get the next logical record from.
   * @return this LogRecord.
   * @throws LogRecordSizeException
   * if capacity of this <i> LogRecord.dataBuffer </i> is not sufficient to
   * hold the entire record data.
   * @throws InvalidLogBufferException
   * if the size of the data record exceeds the bytes used for the buffer.
   * @see LogRecordType
   */
  LogRecord get(LogBuffer lb) throws LogRecordSizeException, InvalidLogBufferException
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
      type = buffer.getShort();
      length  = buffer.getShort();
      if (length > data.length)
      {
        buffer.reset();
        throw new LogRecordSizeException();
      }
      if (buffer.position() + length > lb.bytesUsed)
      {
        buffer.reset();
        throw new InvalidLogBufferException();
      }
      buffer.get(data, 0, length);
      // TODO: update key
      key = logKey;   
    }
    else
    {
      // no data left in this buffer
      type = LogRecordType.CTRL | LogRecordType.EOB;
    }
    
    this.type = type;
    this.length = length;
    this.key = key;
    this.tod = lb.tod;
    dataBuffer.clear().limit(length);
    
    return this;
  }

}
