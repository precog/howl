/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 * Created on Apr 8, 2004
 */
package org.objectweb.howl.log;


/**
 * Objects that wish to read a log must implement the ReplayListener interface.
 * @author Michael Giroux
 * @see Logger#replay(ReplayListener,long)
 */
public interface ReplayListener
{
  /**
   * Called by Logger for each record retrieved from the log.
   * <p>when the entire log has been processed,
   * lr.type is set to LogRecordType.END_OF_LOG. 
   * @param lr LogRecord to be processed
   */
  void onRecord(LogRecord lr);
  
  /**
   * Called by Logger when an exception is encountered
   * during replay.
   *  
   * @param exception LogException object that was thrown
   * when the error occurred.
   */
  void onError(LogException exception);
  
  /**
   * Called by Logger when ReplayListener is registered for
   * replay.
   * <p>The Logger calls getLogRecord to obtain a LogRecord instance
   * to be used to process log records.
   *  
   * <p>The same LogRecord instance is used to return all
   * log records to the ReplayListener.
   * 
   * @return LogRecord object to be used when calling onRecord()
   */
  LogRecord getLogRecord();
}
