/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */
package org.objectweb.howl.log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.channels.FileChannel;

import java.util.Arrays;

/**
 * This reader supports journals written by LogBufferManager.
 *
 * <p>Journal records are written to disk as blocks
 * of data.  Block size is a multiple of 512 bytes
 * to assure optimum disk performance.
 * <p>Each block contains a header, journal records,
 * and a footer.  The header and footer provide
 * enough information to detect errors during
 * read and recovery operations.
 */
public class XAJournalReader
{
  // ByteBuffer used for journal IO
  ByteBuffer buffer = null;

  // block header & footer fields
  private byte[] header_ID = "HOWL".getBytes();
  private byte[] footer_ID = "LWOH".getBytes();

  // header data items for current buffer
  private byte[] bufferHeader = new byte[header_ID.length];
  private int bufferbsn = 0;
  private int bufferSize = 0; // determined by reading journal
  private int bytesUsed = 0;
  private long headerTod = 0;
  private long footerTod = 0;
  private byte[] bufferFooter = new byte[footer_ID.length];
  
  private long token = 0; // token for last record returned by get()

  private int bufferHeaderSize = 24;
  /* buffer Header format
   * byte header_ID             [4]
   * int  block_sequence_number [4]
   * int  block_size            [4]
   * int  bytes used            [4]
   * long  currentTimeMillis    [8]
   */
  private int bufferFooterSize = 12;
  /* long currentTimeMillis     [8] same value as header
   * byte footer_ID             [4]
   */
  
  // record header is length of data
  private int recordHeaderSize = 2;

  // next block sequence number to be read
  int nextBSN = 0;

  // JOURNAL FILE CHANNEL
  FileChannel jrnl = null;

  // maximimum size of data record
  int maxRecordSize = 0;
  
  /**
   * @return token for last record processed by get()
   */
  public long getToken()
  {
    return token;
  }

  /**
   * copies next data record from ByteBuffer to callers byte[].
   * <p>after calling get() use getToken() to retrieve the token for the record.
   *
   * @returns number of bytes copied (may be less than data.length)
   * @throws BufferOverflowException if callers byte[] is too small for next record.
   */
  public int get(byte[] data)
  {
    // if current position is beyond bytes used then read next block
    
    // save token for current record
    token = (bufferbsn << 32) + buffer.mark().position();

    // copy next record
    int recordSize = buffer.getInt();
    if (recordSize > data.length)
    {
      buffer.reset();  // caller can retry current record
      throw new BufferOverflowException();
    }
    
    buffer.get(data, 0, recordSize);
    return recordSize;
  }

  /**
   * opens designated journal file and allocates IO buffers.
   */
  public void open(File journal) throws IOException, InvalidLogHeaderException
  {
    jrnl = new RandomAccessFile(journal, "r").getChannel();

    ByteBuffer buffer = ByteBuffer.allocateDirect(bufferHeaderSize);
    int bytesRead = jrnl.read(buffer);

    // verify buffer header
    buffer.clear();
    buffer.get(bufferHeader);
    if (!Arrays.equals(bufferHeader, header_ID))
      throw new InvalidLogHeaderException();

    bufferbsn = buffer.getInt();
    bufferSize = buffer.getInt();
    bytesUsed = buffer.getInt();
    headerTod = buffer.getLong();
    int firstDataRecord = buffer.position();

    maxRecordSize = bufferSize - buffer.position() - bufferFooterSize;

    // reallocate buffer large enough to hold an entire journal block
    buffer = ByteBuffer.allocateDirect(bufferSize);
    jrnl.position(0);
    jrnl.read(buffer);

    // verify buffer footer
    buffer.position(bufferSize - bufferFooterSize);
    footerTod = buffer.getLong();
    buffer.get(bufferFooter);
    

    if (footerTod != headerTod || !Arrays.equals(bufferFooter, footer_ID))
      throw new InvalidLogHeaderException();

    // we have a valid journal file -- set instance members
    this.buffer = buffer;
    buffer.position(firstDataRecord);
  }

  /**
   * closes the log file and performs necessary cleanup tasks.
   */
  public void close() throws IOException
  {
    jrnl.close();
  }

  /**
   * @returns the size of the largest possible data record
   */
  public int getMaxRecordSize()
  {
    return (buffer == null ? 0 : maxRecordSize);
  }

}
