/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
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
