/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */
package org.objectweb.howl.log;

import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * Manage a configured set of two or more physical log files.
 * 
 * <p>Log files have a configured maximum size.  When a file has
 * reached the configured capacity, Logger switches to
 * the next available alternate file.  Normally, log files are created
 * in advance to guarantee that space is available during execution.
 * 
 * <p>Each log file has a file header containing information 
 * allowing Logger to reposition and replay the logs
 * during a recovery scenario.
 * 
 * <p>LogFile <i> marking </i>
 * <p>The LogFile's mark is the the position within the file
 * of the oldest active entry.
 * Initially the mark is set at the beginning of the file.
 * At some configured interval, the caller invokes <i> mark() </i>
 * with the key of the oldest active entry in the log.
 * 
 * <p>For XA the key would be for the oldest transaction still
 * in committing state.  In theory, XA could call <i> mark() </i> every
 * time a DONE record is logged.  In practice, it should only be
 * necessary to call <i> mark() </i> every minute or so depending on the
 * capacity of the log files.
 * 
 * <p>The Logger maintains an active mark within the set
 * of log files.  A file may be reused only if the mark does not
 * reside within the file.  The Logger will throw
 * LogFileOverflowException if an attempt is made to switch to a
 * file that contains a mark.
 * 
 * @author Michael Giroux
 *
 */
public class Logger
{
  /**
   * indicates whether the LogFile is open.
   * <p>Logger methods return LogClosedException when log is closed.
   */
  private volatile boolean isClosed = true;
  
  /**
   * Manages a pool of buffers used for log file IO.
   */
  private LogBufferManager bmgr = null;
  
  /**
   * Manages a pool of files used for log file IO.
   */
  private LogFileManager lfmgr = null;
  
  /**
   * last key returned by put() or putAndSync().
   */
  private long currentKey = 0;
  
  public Logger()
    throws IOException
  {
  }
  
  /**
   * called by public put() and putAndSync() methods to generalize
   * common functionality.
   * 
   * @param type record type
   * @param data record data
   * @param sync true if call should block until force
   * @return the log key returned by buffer manager put() method.
   * @throws LogClosedException
   * @throws LogRecordSizeException
   * @throws InterruptedException
   * @throws IOException
   */
  private long put(short type, byte[] data, boolean sync)
    throws LogClosedException, LogRecordSizeException, LogFileOverflowException,
                InterruptedException, IOException
  {
    if (isClosed) throw new LogClosedException();

    // TODO: deal with exceptions

    long key = bmgr.put(type, data, sync);
    synchronized(this)
    {
      currentKey = key;
      if (lfmgr.automark && key > lfmgr.activeMark) lfmgr.activeMark = key;
    }
    return key;
  }

  /**
   * add a USER record to log.
   * 
   * <p>if <i> force </i> parameter is true, then the method will
   * block until the <i> data </i> buffer is forced to disk.  Otherwise,
   * the method returns immediately.
   * 
   * @return a key that can be used to locate the record.
   * When automark is disabled (false) the caller must
   * invoke mark using this key to indicate the location
   * of the oldest active entry in the log.
   * 
   * @see LogBuffer#put
   */
  public long put(byte[] data, boolean force)
    throws LogClosedException, LogRecordSizeException, LogFileOverflowException,
                InterruptedException, IOException
  
  {
    long key = put(LogRecordType.USER, data, force);
    return key;
  }

  /**
   * sets the LogFile's mark.
   * 
   * <p><i> mark() </i> provides a generalized method for callers
   * to inform the Logger that log space can be released
   * for reuse.
   *
   * <p>calls LogFileManager to process the request.
   * 
   * @param key is a log key returned by a previous call to put() or putAndSync().
   * @throws InvalidLogKeyException if <i> key </i> parameter is out of range.
   * key must be greater than current activeMark and less than the most recent
   * key returned by put().
   */
  public void mark(long key)
    throws InvalidLogKeyException, LogClosedException, IOException, InterruptedException
  {
    if (isClosed)
      throw new LogClosedException("log is closed");
    
    lfmgr.mark(key, bmgr);
  }
  
  /**
   * Sets the LogFile marking mode.
   * 
   * <p>passes call to LogFileManager
   * 
   * @param autoMark true to indicate automatic marking.
   */
  public void setAutoMark(boolean autoMark)
    throws LogClosedException, LogFileOverflowException, IOException, InterruptedException
  {
    lfmgr.setAutoMark(autoMark, bmgr);
  }
  
  /**
   * close the Log files and perform necessary cleanup tasks.
   */
  public void close() throws IOException
  {
    // prevent new threads from adding to the log
    isClosed = true;

    // shutdown the buffer manager
    bmgr.stop();
    
    // TODO: write log status to active log file
    
    lfmgr.close();
  }
  
  /**
   * open Log files and perform necessart initialization.
   * 
   * TODO: consider boolean restart parameter 
   *
   */
  public void open()
    throws FileNotFoundException, ClassNotFoundException, IOException, IllegalAccessException, InstantiationException
  {
    configure();

    lfmgr = new LogFileManager();
    lfmgr.open();
    
    bmgr = new LogBufferManager(lfmgr);
    bmgr.open();
    
    // indicate that Log is ready for use.
    isClosed = false;
  }
  
  public void configure()
  {
    // TODO: configuration code here
  }
  
  /**
   * return an XML node containing statistics for the Logger, the LogFile pool and the LogBuffer pool.
   * 
   * <p>The getStats method for the LogBufferManager and LogFileManager are called to include
   * statistics for these contained objects.
   * 
   * @return String contiining XML document.
   */
  public String getStats()
  {
    String name = this.getClass().getName();
    StringBuffer stats = new StringBuffer(
        "<?xml version='1.0'>" +
        // TODO: define style sheet and link in root node
        "\n<Logger  class='" + name + "'>" 
    );
    
    // TODO: append Logger specific stats
    
    stats.append(bmgr.getStats());
    
    stats.append(lfmgr.getStats());
    
    stats.append("\n</Logger>" +
        "\n");
    
    return stats.toString();
  }
}
