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
  
  public Logger()
    throws IOException
  {
  }
  
  /**
   * add a USER record to log.
   * 
   * <p>if <i> force </i> parameter is true, then the method will
   * block (in bmgr.put()) until the <i> data </i> buffer is forced to disk.
   * Otherwise, the method returns immediately.
   * 
   * @param data record data
   * @param sync true if call should block until force
   * 
   * @return a key that can be used to locate the record.
   * Some implementations may use the key as a correlation ID 
   * to associate related records.
   * 
   * When automark is disabled (false) the caller must
   * invoke mark() using this key to indicate the location
   * of the oldest active entry in the log.
   * 
   * @throws LogClosedException
   * @throws LogRecordSizeException
   * @throws LogFileOverflowException
   * @throws InterruptedException
   * @throws IOException
   * 
   * @see #mark
   */
  public long put(byte[] data, boolean sync)
    throws LogClosedException, LogRecordSizeException, LogFileOverflowException,
                InterruptedException, IOException
  {
    if (isClosed) throw new LogClosedException();

    // QUESTION: should we deal with exceptions here?

    long key = bmgr.put(LogRecordType.USER, data, sync);
    lfmgr.setCurrentKey(key);

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
    
    lfmgr.mark(key);
  }
  
  /**
   * Sets the LogFile marking mode.
   * 
   * <p>passes call to LogFileManager
   * 
   * @param autoMark true to indicate automatic marking.
   */
  public void setAutoMark(boolean autoMark)
    throws InvalidLogKeyException, LogClosedException, LogFileOverflowException, IOException, InterruptedException
  {
    lfmgr.setAutoMark(autoMark);
  }
  
  /**
   * close the Log files and perform necessary cleanup tasks.
   */
  public void close() throws InvalidLogKeyException, LogClosedException, IOException, InterruptedException
  {
    // prevent new threads from adding to the log
    isClosed = true;
    
    lfmgr.close();
  }
  
  /**
   * open Log files and perform necessart initialization.
   * 
   * TODO: consider boolean restart parameter or replayListener parameter
   * 
   * TODO: consider open(String name) to allow named configurations.
   *       this would allow utility to open two loggers and copy 
   *       old records to new files.
   *
   */
  public void open()
    throws InvalidFileSetException, ClassNotFoundException,
           FileNotFoundException, IOException,
           InvalidLogBufferException,
           LogConfigurationException
  {
    configure();

    lfmgr = new LogFileManager();
    lfmgr.open();
    
    bmgr = new LogBufferManager();
    bmgr.open(); // TODO: get logfile[0] 
    
    // read header information from each file
    lfmgr.init(bmgr);
    
    // indicate that Log is ready for use.
    isClosed = false;
  }
  
  /**
   * Registers a LogEventListener for log event notifications.
   * 
   * @param eventListener object to be notified of logger events.
   */
  public void setLogEventListener(LogEventListener eventListener)
  {
    lfmgr.setLogEventListener(eventListener);
  }
  
  /**
   * Replays log from a specified mark forward to the current mark.
   * 
   * <p>Beginning with the record located at <i> mark </i>
   * the Logger reads log records forward to the end of the log.
   * USER records are passed to the <i> listener </i> onRecord()
   * method. When the end of log has been reached, replay returns
   * one final record with a type of END_OF_LOG to inform <i> listener </i>
   * that no further records will be returned.
   * 
   * <p>If an error is encountered while reading the log, the
   * <i> listener </i> onError method is called.  Replay terminates
   * when any error occurs and when END_OF_LOG is encountered.
   * 
   * @param listener an object that implements ReplayListener interface.
   * @param mark a log key to begin replay from.
   * <p>The <i> mark </i> should be a valid log key returned by the put()
   * method.  To replay the entire log beginning with the oldest available
   * record, <i> mark </i> should be set to zero (0L).
   * @throws InvalidLogKeyException
   * if <i> mark </i> is not a valid log key.
   */
  public void replay(ReplayListener listener, long mark)
    throws InvalidLogKeyException
  {
    
    if (mark == 0)
    {
      // TODO: mark = oldest_available_key;
    }

    // TODO: position log to mark
    
    // TODO: replay from a specific mark
  }
  
  /**
   * Replays log from the active mark forward to the current mark.
   * 
   * @param listener an object that implements ReplayListener interface.
   * @throws InvalidLogKeyException
   * @see #replay(ReplayListener, long)
   */
  public void replay(ReplayListener listener)
    throws InvalidLogKeyException
  {
    replay(listener, lfmgr.activeMark);
  }
  
  /**
   * Reads configuration parameters from conf directory.
   */
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
        "<?xml version='1.0' ?>" +
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
