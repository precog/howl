/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */
package org.objectweb.howl.log;

/**
 * Checked exception thrown when <i>put </i> is called 
 * after the log has been closed.
 */
public class LogClosedException extends LogException
{

  /**
   * Constructs an instance of this class.
   */
  public LogClosedException()
  {
  }

  public LogClosedException(String s)
  {
    super(s);
  }
}
