/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * All rights reserved.
 * 
 * Contact: howl@objectweb.org
 * 
 * This software is licensed under the BSD license.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *     
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
