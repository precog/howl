/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */

package org.objectweb.howl.log;

import java.io.File;

/**
 * Checked exception thrown when the Logger
 * attempts to switch to an alternate log file that
 * contains the active mark.
 * @see Logger#mark(long)
 */
public class LogFileOverflowException extends LogException
{
    /**
     * Constructs an instance of this class.
     */
    public LogFileOverflowException() { }
    
    /**
     * Construct an exception with message describing the problem
     * @param activeMark
     * @param highMark
     */
    public LogFileOverflowException(long activeMark, long highMark, File lf)
    {
      super(format(activeMark, highMark, lf));
    }
    
    static String format (long activeMark, long highMark, File lf)
    {
      StringBuffer sb = new StringBuffer(lf.toString() + ": high mark = ");
      sb.append(Long.toHexString(highMark));
      sb.append("; active mark for Logger = ");
      sb.append(Long.toHexString(activeMark));
      return sb.toString();
    }
    
}
