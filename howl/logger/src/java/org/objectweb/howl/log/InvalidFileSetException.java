/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 * Created on Mar 30, 2004
 */
package org.objectweb.howl.log;


/**
 * Exception thrown when LogFileManager.open() detects an
 * invalid file set.
 * <p>This exception can occur if the log file manager detects
 * that one or more of the files that should reside in a file
 * set are missing.
 * 
 * @author Michael Giroux
 *
 */
public class InvalidFileSetException extends LogException
{
  public InvalidFileSetException() { }

  public InvalidFileSetException(String s) { super(s); }

}
