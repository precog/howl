/*
 * Created on Mar 24, 2004
 *
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
  File name = null;
  
  /**
   * FileChannel associated with this LogFile.
   * 
   * <p>The FileChannel is private to guarantee that all calls to the
   * channel methods come through this LogFile object to allow
   * for statistics collection.  
   */
  private FileChannel channel = null;
  
  /**
   * number of times this file position was reset to zero
   */
  int rewindCounter = 0;
  
  /**
   * total number of bytes written
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
   * currentTimeMillis when LogFileManager switched to this LogFile. 
   */
  long tod = 0;
  
  /**
   * construct an instance of LogFile for a given file name
   * @param name filename
   */
  LogFile(File name)
  {
    this.name = name;
  }
  
  /**
   * open the file and get the associated nio FileChannel for the file.
   * @throws FileNotFoundException
   */
  LogFile open() throws FileNotFoundException
  {
    channel = new RandomAccessFile(name, "rw").getChannel();
    return this;
  }
  
  /**
   * Close the channel associated with this LogFile
   * @return this LogFile
   * @throws IOException
   */
  LogFile close() throws IOException
  {
    channel.close();
    return this;
  }
  
  /**
   * A convenience helper method providing access to the
   * current position of the FileChannel for this LogFile.
   * 
   * @return a long containing current position of the channel for this LogFile
   * @throws IOException thrown by FileChannel.positio()
   * @see java.nio.channels.FileChannel#position()
   */
  long position() throws IOException
  {
    return channel.position();
  }
  
  /**
   * Helper provides access to the FileChannel.write() method for
   * the FileChannel associated with this LogFile.
   * <p>Hides actual FileChannel and allows capture of statistics.
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
    
    channel.write(lb.buffer);
    bytesWritten += lb.buffer.capacity();
  }
  
  /**
   * Helper provides access to the FileChannel.force() method for
   * the FileChannel associated with this LogFile.
   * <p>Hides actual FileChannel and allows capture of statistics.
   * @param forceMetadata as defined by FileChannel.force()
   * @throws IOException
   * @see FileChannel#force
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
    
    String result = "\n<LogFile class='" + clsname + "' file='" + name + "'>" +
    "\n  <rewindCount value='" + rewindCounter + "'>Number of times this file was rewind to position(0)</rewindCount>" +
    "\n  <bytesWritten value='" + bytesWritten + "'>Number of bytes written to the file</bytesWritten>" +
      "\n</LogFile>" +
      "\n";
    
    return result;
  }
}
