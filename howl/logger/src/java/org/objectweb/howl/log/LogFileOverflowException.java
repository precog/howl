/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * All rights reserved.
 * 
 * Contact: howl@objectweb.org
 * 
 * This software is licensed under the BSD license.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *     
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * ------------------------------------------------------------------------------
 * $Id: LogFileOverflowException.java,v 1.4 2005-11-17 20:50:11 girouxm Exp $
 * ------------------------------------------------------------------------------
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
   * Determines if a de-serialized file is compatible with this class.
   *
   * Maintainers must change this value if and only if the new version
   * of this class is not compatible with old versions. See Sun docs
   * for
   * <a href=http://java.sun.com/j2se/1.5.0/docs/guide/serialization/spec/class.html>
   * details. </a>
   *
   * Not necessary to include in first version of the class, but
   * included here as a reminder of its importance.
   */
  static final long serialVersionUID = -7955598809372627369L;
  
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
