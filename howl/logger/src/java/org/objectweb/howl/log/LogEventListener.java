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
   * @param logkey lowest safe log key.
   * 
   * <p>LogEventListener should cause log records with 
   * keys less than <i> logkey </i> to be copied forward
   * to prevent a LogOverflowException.
   * <p>Hopefully, the LogEventListener will be able
   * to regenerate the records from memory without
   * having to read the physical log file.  For example,
   * a Transaction Manager should maintain a table of
   * transactions that are in the COMMITTING mode,
   * with associated log key for each transaction.
   * The logOverflowNotification method would call
   * Logger.put() for each transaction that has a
   * log key less than <i> logKey </i>.
   * <p>Before returning from logOverflowNotification
   * the LogEventListener should call Logger.mark(newMark, force)
   * with <i> force </i> set to <b> true </b> to 
   * assure that the new records have been committed to
   * physical disk. 
   */
  void logOverflowNotification(long logkey);
  
}
