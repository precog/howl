/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */
package org.objectweb.howl.log;

/**
 * Base exception class for HOWL exceptions.
 */
public class LogException extends Throwable
{
  public LogException()
  {
  }

  public LogException(String s)
  {
    super(s);
  }
}
