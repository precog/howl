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
import java.nio.channels.FileLock;

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
   * <p>While this LogFile is the current LogFile of the set,
   * (ie, the LogFile that is currently being written to) 
   * the highMark will be set to first record of the subsequent
   * block.  For example, while block 1 is being written,
   * highMark will be set to the first record of block 2.
   * 
   * <p>During replay operations, the end of the active
   * journal can be detected by comparing the BSN of a desired
   * block with the current highMark.  If the requested BSN
   * is less than highMark, then the requested block resides
   * within active journal space.  If the requested BSN is
   * >= the highMark, then the BSN is invalid. 
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
   * @see #open(String filemode)
   */
  boolean newFile = true;
  
  /**
   * FileLock acquired when file is opened.
   */
  FileLock lock = null;
  
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
  LogFile open(String fileMode) throws LogConfigurationException, FileNotFoundException
  {
    this.fileMode = fileMode;

    // remember whether the file existed or not
    newFile = !file.exists();
    
    // if it already existed, but length is zero, then it is still a new file
    if (!newFile) newFile = file.length() == 0;
    
    channel = new RandomAccessFile(file, fileMode).getChannel();
    
    //  FEATURE 300922; lock file to prevent simultanious access
    try {
      lock = channel.tryLock();
    } catch (IOException e) {
      throw new LogConfigurationException(e);
    }
    if (lock == null)
      throw new LogConfigurationException("Unable to obtain lock on " + file.getAbsolutePath());
    // TODO: log lock acquired
    // System.err.println(file.getName() + " open");
    
    return this;
  }
  
  /**
   * Close the channel associated with this LogFile.
   * <p>Also releases the lock that is held on the file.
   * @return this LogFile
   * @throws IOException
   */
  LogFile close() throws IOException
  {
    if (channel.isOpen())
    {
      // prevent multiple close
      position = channel.position();     // remember postion at close
      //  FEATURE 300922; unlock the file if we obtained a lock.
      if (lock != null)
      {
        lock.release();
        // TODO: log lock released
        // System.err.println(file.getName() + " closed");
      }
      channel.close();
    }
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
   * 
   * <p>Hides actual FileChannel and allows capture of statistics.
   * 
   * <p>In theory the force could be eliminated if the
   * file is open with mode "rwd" or "rws" because the
   * access method is supposed to guarantee that the
   * writes do not return until the data is on the media.
   * Unfortunately, testing with Windows XP platforms
   * suggests that system write cache may confuse the
   * Java runtime and the program will actually return
   * before data is on media.  Consequently, this 
   * method *always* does a force() regardless of
   * the file open mode.
   * 
   * @param forceMetadata as defined by FileChannel.force()
   * @throws IOException
   * @see FileChannel#force(boolean)
   */
  void force(boolean forceMetadata) throws IOException
  {
    channel.force(forceMetadata);
  }
  
  /**
   * return statistics for this LogFile as an XML string.
   * @return XML string containing LogFile statistics.
   */
  String getStats()
  {
    String clsname = this.getClass().getName();

    StringBuffer result = new StringBuffer("\n<LogFile file='" + file + "'>" +
    "\n  <rewindCount value='" + rewindCounter + "'>Number of times this file was rewind to position(0)</rewindCount>" +
    "\n  <bytesWritten value='" + bytesWritten + "'>Number of bytes written to the file</bytesWritten>" +
    "\n  <position value='" + position + "'>FileChannel.position()</position>" +
    "\n</LogFile>" +
    "\n" 
    );

    return result.toString();
  }
}
