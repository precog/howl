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
package org.objectweb.howl.jotm;

import org.objectweb.howl.log.LogException;
import org.objectweb.howl.log.LogRecord;
import org.objectweb.howl.log.ReplayListener;

/**
 * This reader supports journals written by
 * org.objectweb.holw.log.Logger.
 */
public class XAJournalReader implements ReplayListener
{
  LogRecord record = null;
  
  final int defaultRecordSize = 256;

  /**
   * constructs an instance of XAJournalReader 
   * with a LogRecord using defaultRecordSize.
   */
  XAJournalReader()
  {
    record = new LogRecord(defaultRecordSize);
  }
  
  /**
   * construct an instance of XAJournalReader with a specified
   * LogRecord size.
   * 
   * @param recordSize size of LogRecord buffer (in bytes) to allocate.
   */
  XAJournalReader(int recordSize)
  {
    record = new LogRecord(recordSize);
  }

  /* (non-Javadoc)
   * @see org.objectweb.howl.log.ReplayListener#onRecord(org.objectweb.howl.log.LogRecord)
   */
  public void onRecord(LogRecord lr)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see org.objectweb.howl.log.ReplayListener#onError(org.objectweb.howl.log.LogException)
   */
  public void onError(LogException exception)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see org.objectweb.howl.log.ReplayListener#getLogRecord()
   */
  public LogRecord getLogRecord()
  {
    // TODO Auto-generated method stub
    return null;
  }
  
}
