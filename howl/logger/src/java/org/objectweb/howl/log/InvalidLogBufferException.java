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
 * $Id: InvalidLogBufferException.java,v 1.6 2005-11-17 20:50:11 girouxm Exp $
 * ------------------------------------------------------------------------------
 */
package org.objectweb.howl.log;

/**
 * Exception thrown when Logger detects a journal block
 * with invalid content.
 * <p>Methods that read log blocks and process
 * records within the blocks detect invalid content
 * and throw this exception.
 * <p>Reasons for throwing this exception include:
 * <ul>
 * <li>Invalid block header information
 * <li>Invalid block footer information
 * <li>Record size exceeds bytes used for block
 * </ul>
 */
public class InvalidLogBufferException extends LogException
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
  static final long serialVersionUID = -3688485317638394257L;
  
  public InvalidLogBufferException() { }

  public InvalidLogBufferException(String s) { super(s); }
  
  public InvalidLogBufferException(Throwable cause) { super(cause); }

  public InvalidLogBufferException(String s, Throwable cause) { super(s,cause); }
  
}
