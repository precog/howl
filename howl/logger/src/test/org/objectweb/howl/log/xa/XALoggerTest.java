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
package org.objectweb.howl.log.xa;

import org.objectweb.howl.log.LogException;
import org.objectweb.howl.log.LogRecord;
import org.objectweb.howl.log.ReplayListener;
import org.objectweb.howl.log.TestDriver;

/**
 * 
 * @author Michael Giroux
 */
public class XALoggerTest extends TestDriver
{
  XALogger log = null;
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(XALoggerTest.class);
  }
  
  private class XLTReplayListener implements ReplayListener
  {
    /* (non-Javadoc)
     * @see org.objectweb.howl.log.ReplayListener#onRecord(org.objectweb.howl.log.LogRecord)
     */
    public void onRecord(LogRecord lr) {
      // TODO Auto-generated method stub
      assertTrue("Expecting XALogRecord, found " + lr.getClass().getName(), (lr instanceof XALogRecord));
    }

    /* (non-Javadoc)
     * @see org.objectweb.howl.log.ReplayListener#onError(org.objectweb.howl.log.LogException)
     */
    public void onError(LogException exception) {
      // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.objectweb.howl.log.ReplayListener#getLogRecord()
     */
    public LogRecord getLogRecord() {
      return new XALogRecord(120);
    }
    
  }

  /**
   * Constructor for XALoggerTest.
   * @param name
   */
  public XALoggerTest(String name) {
    super(name);
  }
  
  protected void setUp() throws Exception {
    super.setUp();

    log = new XALogger(cfg);
    super.log = log;
  }
  
  /**
   * Verify that XALogger.open() throwss
   * an UnsupportedOperationException.
   * 
   * @throws LogException
   * @throws Exception
   */
  public void testUnsupportedOpen()
  throws LogException, Exception
  {
    log = new XALogger(cfg);
    try {
      log.open();
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      ; // test passed
    }
  }

  /**
   * Test a single thread.
   * <p>This test verifies that a single thread is able to log
   * transactions successfully.  It depends on the 
   * LogBufferManager.FlushManager thread to force the
   * COMMIT records.
   * <p>msg.count is set to 10.
   * 
   * @throws LogException
   * @throws Exception
   */
  public void testSingleThread()
  throws LogException, Exception
  {
    log.open(new XLTReplayListener());
    log.setAutoMark(true);
    
    prop.setProperty("msg.count", "10");
    workers = 1;
    runWorkers(XAWorker.class);
  }
  
  /**
   * Test with automark TRUE so we never get overflow.
   * <p>This test drives messages through the logger and
   * confirms that the normal buffer full conditions will
   * force buffers to disk.
   * 
   * @throws LogException
   * @throws Exception
   */
  public void testAutoMarkTrue()
  throws LogException, Exception
  {
    log.open(new XLTReplayListener());
    log.setAutoMark(true);

    runWorkers(XAWorker.class);
  }
  
  /**
   * Test with automark FALSE so we can checkout the
   * log overflow processing.
   * <p>Test uses a single delayed worker.
   * 
   * @throws LogException
   * @throws Exception
   */
  public void testAutoMarkFalseOneDelayedWorker()
  throws LogException, Exception
  {
    log.open(new XLTReplayListener());
    log.setAutoMark(false);
    
    delayedWorkers = 1;
    runWorkers(XAWorker.class);
  }

  /**
   * Test with automark FALSE so we can checkout the
   * log overflow processing.
   * <p>Test uses four delayed workers.
   * 
   * TODO: may be able to delete this test since
   * the basic feature is covered in previous test.
   * 
   * @throws LogException
   * @throws Exception
   */
  public void testAutoMarkFalseFourDelayedWorker()
  throws LogException, Exception
  {
    log.open(new XLTReplayListener());
    log.setAutoMark(false);
    
    delayedWorkers = 4;
    runWorkers(XAWorker.class);
  }
  
  /**
   * Display the activeTx table following open.
   */
  public void testActiveTxDisplay()
  throws LogException, Exception
  {
    log.open(new XLTReplayListener());
    log.activeTxDisplay();
  }
  
}
