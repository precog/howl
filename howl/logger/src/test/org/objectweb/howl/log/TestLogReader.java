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




class TestLogReader implements ReplayListener
{
  LogRecord logrec = new LogRecord(80);
  long recordCount = 0;
  long previousKey = 0;
  boolean done = false;
  Exception exception = null;
  
  public void onRecord(LogRecord lr)
  {
    if (lr.type == LogRecordType.END_OF_LOG)
    {
      synchronized(this) {
        done = true;
        notifyAll();
      }
    }
    else {
      ++recordCount;
      if (lr.key <= previousKey) {
        System.err.println("Key Out of Sequence; total/prev/this: " + recordCount + " / " +
            Long.toHexString(previousKey) + " / " + Long.toHexString(lr.key));
      }
    }
  }
  public void onError(LogException e)
  {
    exception = e;
  }
  
  public LogRecord getLogRecord()
  {
    return logrec;
  }
  
  void run(Logger log) throws Exception, LogException
  {
    log.replay(this, 0L);
    log.close();
    
    synchronized (this)
    {
      while(!done)
      {
        wait();
      }
    }
  }
  
}