/*
 * Created on Mar 25, 2004
 *
 */
package org.objectweb.howl.log;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.nio.ByteBuffer;

/**
 * Manage a set of log files.
 * 
 * This class implements methods that can be called by LogBufferManager to obtain a LogFile for
 * logger IO and to signal the LogFileManager when new buffers are being initialized.  LogFileManager
 * manages log files according to implementation specific policies.
 * Some LogFileManagers may use a circular file policy while others may use a set of files.
 * The most simple implementations will use a single file and allow it to grow as needed.
 * 
 * QUESTION: do we need multiple implementations, or can we deal with different policies
 * in this one class using configuration?
 * 
 * @author Michael Giroux
 *
 */
class LogFileManager
{
  
  /**
   * maximum number of blocks to store in each LogFile.
   * 
   * <p>controls when logging is switched to a new log file, and/or when a circular
   *  log is reset to  seek address zero.
   * 
   * @see #getLogFile
   */
  int maxBlocksPerFile = Integer.MAX_VALUE;

  /**
   * The log key for the oldest active entry in the log.
   * 
   * <p>When automark is enabled (true) the <i> activeMark </i>
   * is updated after every put() or putAndSync() operation.
   * When automark is disabled (as should be the case with JOTM)
   * the <i> activeMark </i> is updated manually by a call
   * to mark().
   * 
   * @see #mark
   */
  long activeMark = 0;
  
  /**
   * indicates whether log files will be marked automatically.
   * <p>When automark is false, the <i> mark() </i> method must be
   * invoked by the log user.  When automark is true, the Logger
   * will automatically set the mark at the most recent record. 
   */
  boolean automark = false;
  
  /**
   * data written to log when autoMark is turned on
   */
  final byte[] autoMarkOn = new byte[] { 1 };
  
  /**
   * data written to log when autoMark is turned off
   */
  final byte[] autoMarkOff = new byte[] { 0 };

  /**
   * lock controlling access to LogFile.
   */
  private final Object fileManagerLock = new Object();
  
  
  /**
   * set of LogFile objects associated with the physical log files.
   * 
   * @see #open
   */
  LogFile[] fileSet = null;
  
  /**
   * index to current entry in fileSet[]
   */
  short lfIndex = 0;

  LogFile currentLogFile = null;
  
  /**
   * number of log files to configure
   */
  int maxLogFiles = 2;
  
  /**
   * directory used to create log files
   */
  String logDir = ".";
  
  /**
   * file name extension for log files
   */
  String logFileExt = "log";
  
  /**
   * filename used to create log files.
   * <p>file names are generated using the following pattern:
   * <pre>
   *   <value of logFileName> + "_" + <file number> + "." + <value of logFileExt>
   * </pre>
   */
  String logFileName = "def";
  
  /**
   * LogFile header record.
   * 
   * <p>protected by fileManagerLock
   * 
   * <p>The first record of every LogFile is a HEADER record containing
   * information that is used during recovery to reposition the log file
   * and replay records starting from the active mark.
   * 
   * byte[1] autoMark          byte[1]
   * long     activeMark         byte[8]  global to all log files
   * long     lowMark            byte[8]  low mark for current file == high mark for previous file
   * long     prevSwitchTod   byte[8] time of previous file switch
   * byte[2] crlf                   byte[2]
   */
  byte[] logFileHeader = new byte[27];
  
  /**
   * ByteBuffer wrapper for logFileHeader to facilitate conversion of numeric
   * information to byte[] format.
   * 
   * <p>protected by fileManagerLock
   */
  ByteBuffer lfhBuffer = ByteBuffer.wrap(logFileHeader);
  
  /**
   * end of line for log records to make logs readable in text editors.
   */
  byte[] crlf = "\r\n".getBytes();
  
  /**
   * Called by LogBuffer.init() to obtain the LogFile that is to be used
   * to write a specific log block.
   * 
   * <p>The buffer sequence number of the LogBuffer parameter ( <i> lf.bsn</i> ) represents an
   * implementation specific value that is used to manage log file space. 
   * As buffers are written to disk the buffer
   * sequence number is incremented.
   * The LogFileManager is able to compute the seek
   * address for a buffer as a function of <i>lf.bsn</i> and buffer size.
   *  
   * @param lb LogBuffer that is asking for the LogFile.
   * LogFileManager implementations use <i> lf.bsn </i> to determine when to switch 
   * to a new file, or wrap a circular file back to seek address zero.
   * 
   * @return a LogFile to use for writing the LogBuffer
   */
  LogFile getLogFile(LogBuffer lb) throws LogFileOverflowException
  {
    synchronized(fileManagerLock)
    {
      if (currentLogFile == null || ((lb.bsn - 1) % maxBlocksPerFile) == 0)
      {
        int fsl = fileSet.length;
        lfIndex %= fsl;

        // Make sure active mark is not within the next log file. 
        LogFile nextLogFile = fileSet[lfIndex];
        assert nextLogFile != null: "nextLogFile == null";

        if (activeMark < nextLogFile.highMark)
          throw new LogFileOverflowException(activeMark, nextLogFile.highMark, nextLogFile.name);

        ++lfIndex;
        
        // remember the TOD we switched to this file
        nextLogFile.tod = System.currentTimeMillis();

        // fabricate log key for beginning of new bsn as high mark for current file
        // this value is used to compare with activeMark the next time this object is
        // reused.
        long highMark = lb.bsn << 24;
        
        // default tod for previous file switch is current file switch tod
        long switchTod = nextLogFile.tod;

        if (currentLogFile != null)
        {
          switchTod = currentLogFile.tod;
          currentLogFile.highMark = highMark;
        }
        
        // indicate that the new file must be rewound before this buffer is written
        lb.rewind = true;

        short fileHeader = LogRecordType.CTRL | LogRecordType.HEADER;

        lfhBuffer.clear();
        lfhBuffer.put(automark ? autoMarkOn : autoMarkOff);
        lfhBuffer.putLong(activeMark);
        lfhBuffer.putLong(highMark);
        lfhBuffer.putLong(switchTod);
        lfhBuffer.put(crlf);
        
        lb.put(fileHeader, logFileHeader, false);
        currentLogFile = nextLogFile;
      }
    }
    return currentLogFile;
  }
  
  /**
   * sets the LogFile's mark.
   * 
   * <p><i> mark() </i> provides a generalized method for callers
   * to inform the Logger that log space can be released
   * for reuse.
   *
   * <p>writes a MARKKEY control record to the log.
   * 
   * @param key is an opaque log key returned by a previous call
   * to put() or putAndSync().
   * @throws InvalidLogKeyException if <i> key </i> parameter is out of range.
   * key must be greater than current activeMark and less than the most recent
   * key returned by put().
   */
  void mark(long key, LogBufferManager bmgr)
    throws InvalidLogKeyException, LogClosedException, IOException, InterruptedException
  {
    byte[] markData = new byte[8];
    ByteBuffer markDataBuffer = ByteBuffer.wrap(markData);

    // TODO: validate key -- verify key is within active log file sets 
    // less than most recent key and greater than current active key
    if (key < activeMark)
      throw new InvalidLogKeyException();
    activeMark = key;
    
    short type = LogRecordType.CTRL | LogRecordType.MARKKEY;
    markDataBuffer.putLong(key);
    try {
      bmgr.put(type, markData, false);
    }
    catch (LogRecordSizeException e) {
      // cannot happen, but ignore it if it does
    }
    catch (LogFileOverflowException e) {
      // should not happen since we just gave back some space
      // TODO: handle possible overflow here
    }
  }
  
  
  /**
   * Sets the LogFile marking mode.
   * 
   * <p>writes an AUTOMARK control record to the log if the log
   * is open.
   * 
   * @param automark true to indicate automatic marking.
   */
  void setAutoMark(boolean automark, LogBufferManager bmgr)
    throws LogClosedException, IOException, InterruptedException, LogFileOverflowException
  {
    this.automark = automark;
    
    short type = LogRecordType.CTRL + LogRecordType.AUTOMARK;
    byte[] mode = automark ? autoMarkOn : autoMarkOff;
    try {
      bmgr.put(type, mode, false);
    }
    catch (LogRecordSizeException e) {
      // cannot happen, but ignore it if it does
    }
  }
  
  /**
   * allow LogFileManager to open pool of LogFile(s)
   * 
   * @throws FileNotFoundException
   * 
   * TODO: allow for restart parameter
   */
  void open()
    throws FileNotFoundException
  {
    // retrieve configuration information
    configure();
    
    fileSet = new LogFile[maxLogFiles];
    for (int i=0; i<maxLogFiles; ++i)
    {
      // make sure the directory exists
      File dir = new File(logDir);
      dir.mkdirs();
      
      File name = new File(logDir + "/" + logFileName + "_" + (i+1) + "." + logFileExt);
      try
      {
        fileSet[i] = new LogFile(name).open();
      }
      catch (FileNotFoundException e)
      {
        System.err.println(e + ":" + name);
        throw e;
      }
      
    }
    currentLogFile = null;

    // TODO: write file header to current log file
  }
  
  void close()
    throws IOException
  {
    // TODO: close the LogFiles
    for (int i=0; i < fileSet.length; ++i)
    {
      fileSet[i].close();
    }
  }
  
  
  /**
   * Configure the LogFile pool
   */
  void configure()
  {
    // TODO: configuration code here
    maxBlocksPerFile = Integer.getInteger("howl.log.maxBlocksPerFile",maxBlocksPerFile).intValue();
    maxLogFiles = Integer.getInteger("howl.log.LogFile.maxLogFiles", maxLogFiles).intValue();
    logDir = System.getProperty("howl.log.LogFile.dir", logDir);
    logFileName = System.getProperty("howl.log.LogFile.filename", logFileName);
    logFileExt = System.getProperty("howl.log.LogFile.ext", logFileExt);
  }
  
  String getStats()
  {
    String name = this.getClass().getName();

    StringBuffer stats = new StringBuffer(
        "\n<LogFileManager  class='" + name + "'>" 
        );
    
    // TODO: append LogFileManager stats here

    stats.append("\n<LogFiles>");
    for (int i = 0; i < fileSet.length; ++i)
      stats.append(fileSet[i].getStats());
    stats.append("\n</LogFiles>");

    stats.append("\n</LogFileManager");
    
    return stats.toString();
  }
}
