/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 * Created on Apr 13, 2004
 *
 */
package org.objectweb.howl.log;


/**
 * This interface is implemented by users of the Logger.
 * 
 * <p>If a LogEventListener is registered,
 * the logger will call the LogEventListener
 * when interesting Log Events occur.  For example, the
 * Logger will notify the LogEventListener when the current
 * log file is 50% full to allow the application to copy 
 * old log entries forward.
 * 
 * <p>If the application does not register a LogEventListener
 * it will not have visibility to Log events.
 * 
 * @author Michael Giroux
 */
public interface LogEventListener
{
  /**
   * Called by Logger to notify the LogEventListener
   * that a log file overflow is approaching.
   * 
   * @param logkey first log key in the current log file.
   * <p>LogEventListener should cause log records with 
   * keys less than <i> logkey </i> to be copied forward
   * to prevent a LogOverflowException. 
   */
  void logOverflowNotification(long logkey);
  
}
