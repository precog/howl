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
 * $Id: ReplayListener.java,v 1.3 2005-06-23 23:28:15 girouxm Exp $
 * ------------------------------------------------------------------------------
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
