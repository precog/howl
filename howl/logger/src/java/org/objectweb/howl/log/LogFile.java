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

import java.io.FileNotFoundException;
import java.io.IOException;

import java.io.File;
import java.io.RandomAccessFile;

import java.nio.channels.FileChannel;

/**
 * An individual file within a set of log files managed by a Logger.
 * 
 * <p>The Logger will create an instance of LogFile for each physical file that is
 * configured for the Logger.
 * 
 * @author Michael Giroux
 *
 */
class LogFile
{
  File file = null;
  
  /**
   * @see java.io.RandomAccessFile#RandomAccessFile(java.lang.String, java.lang.String)
   */
  String fileMode = "rw";
  
  /**
   * Will be set false for fileMode "rwd".
   */
  boolean forceRequired = true;
  
  /**
   * FileChannel associated with this LogFile.
   * 
   * <p>The FileChannel is private to guarantee that all calls to the
   * channel methods come through this LogFile object to allow
   * for statistics collection.  
   */
  FileChannel channel = null;
  
  /**
   * number of times this file position was reset to zero.
   */
  int rewindCounter = 0;
  
  /**
   * total number of data written.
   */
  long bytesWritten = 0;
  
  /**
   * log key for the first record in the next file. 
   * <p>when a file switch occurs, the LogFileManager stores the mark
   * for the file header record of the next log file into this LogFile object.
   * Effectively, any mark with a value less than the header record
   * for the next log file resides in this or some previous log file.
   * 
   * <p>Later, when this LogFile object is about to be reused, the value for the
   * active mark is compared with the highMark value.
   * If the active mark is less than highMark, then a LogFileOverflowException
   * is thrown to prevent re-use of a log file that contains active data.
   * 
   * <p>Any attempt to add records to the log will cause an exception
   * until Logger.mark() is called to clear prior records from the log.
   */
  long highMark = Long.MIN_VALUE;
  
  /**
   * BSN of first block in the file.
   * 
   * <p>Initialized by LogFileManager and updated as
   * log files are reused.
   * <p>Used by LogFileManager.read() to calculate offset into a file to
   * read a specific block.
   */
  int firstBSN = 0;
  
  /**
   * currentTimeMillis when LogFileManager switched to this LogFile. 
   */
  long tod = 0;
  
  /**
   * FileChannel.position() of last read or write.
   * 
   * <p>May be used to report the file position when IOException occurs. 
   */
  long position = 0;
  
  /**
   * indicates the file was created during the call to open()
   * @see #open()
   */
  boolean newFile = true;
  
  /**
   * construct an instance of LogFile for a given file name
   * @param file filename
   */
  LogFile(File file)
  {
    this.file = file;
  }
  
  /**
   * open the file and get the associated nio FileChannel for the file.
   * 
   * <p>If the file does not exist, then the newFile member is set true.
   * 
   * @param fileMode value passed to RandomAccessFile constructor.
   * @throws FileNotFoundException
   * if the parent directory structure does not exist. 
   * @see java.io.RandomAccessFile#RandomAccessFile(java.lang.String, java.lang.String)
   */
  LogFile open(String fileMode) throws FileNotFoundException
  {
    this.fileMode = fileMode;
    forceRequired = fileMode.equals("rw");

    // remember whether the file existed or not
    newFile = !file.exists();
    
    // if it already existed, but length is zero, then it is still a new file
    if (!newFile) newFile = file.length() == 0;
    
    channel = new RandomAccessFile(file, fileMode).getChannel();
    return this;
  }
  
  /**
   * Close the channel associated with this LogFile
   * @return this LogFile
   * @throws IOException
   */
  LogFile close() throws IOException
  {
    position = channel.position(); // remember postion at close
    channel.close();
    return this;
  }
  
  /**
   * Helper provides access to the FileChannel.write() method for
   * the FileChannel associated with this LogFile.
   * @param lb Reference to a LogBuffer object that is to be written.
   * @throws IOException
   */
  void write(LogBuffer lb) throws IOException
  {
    if (lb.rewind)
    {
      channel.position(0);
      ++rewindCounter;
      lb.rewind = false;
    }

    bytesWritten += channel.write(lb.buffer);
    position = channel.position();
  }
  
  /**
   * Helper provides access to the FileChannel.force() method for
   * the FileChannel associated with this LogFile.
   * <p>Hides actual FileChannel and allows capture of statistics.
   * @param forceMetadata as defined by FileChannel.force()
   * @throws IOException
   * @see FileChannel#force(boolean)
   */
  void force(boolean forceMetadata) throws IOException
  {
    if (forceRequired)
      channel.force(forceMetadata);
  }
  
  /**
   * return statistics for this LogFile as an XML string.
   * @return XML string containing LogFile statistics.
   */
  String getStats()
  {
    String clsname = this.getClass().getName();

    StringBuffer result = new StringBuffer("\n<LogFile class='" + clsname + "' file='" + file + "'>" +
    "\n  <fileMode value='" + fileMode + "'>RandomAccessFile open mode</fileMode>" +    
    "\n  <rewindCount value='" + rewindCounter + "'>Number of times this file was rewind to position(0)</rewindCount>" +
    "\n  <bytesWritten value='" + bytesWritten + "'>Number of bytes written to the file</bytesWritten>" +
    "\n  <position value='" + position + "'>FileChannel.position()</position>" +
    "\n</LogFile>" +
    "\n" 
    );

    return result.toString();
  }
}
