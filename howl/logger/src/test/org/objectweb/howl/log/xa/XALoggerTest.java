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

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;

import org.objectweb.howl.log.LogException;
import org.objectweb.howl.log.LogRecord;
import org.objectweb.howl.log.LogRecordType;
import org.objectweb.howl.log.ReplayListener;
import org.objectweb.howl.log.TestDriver;

/**
 * 
 * @author Michael Giroux
 */
public class XALoggerTest extends TestDriver
{
  XALogger log = null;
  
  XLTReplayListener openListener = null;
  
  final static int FAILEDRM = 9000;
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(XALoggerTest.class);
  }
  
  /**
   * Implementation of ReplayListener for test cases.
   * 
   * This nested class has reference to the outer class,
   * so it could be in a separate .java source file.
   * Rather than have a separate file, the class is declared
   * static.
   * 
   * @author Michael Giroux
   */
  private static class XLTReplayListener implements ReplayListener
  {
    public long count = 0L;
    
    public long commitCount = 0L;
    
    public long movedCount = 0L;
    
    public Exception exception = null;
    
    PrintStream out = null;
    
    XLTReplayListener()
    {
      out = null;
    }
    
    void setPrintStream(PrintStream out)
    {
      this.out = out;
    }
    
    /**
     * When replay is complete, this HashMap should contain 
     * an entry for each XACOMMIT record that did not have
     * a corresponding XADONE record.
     */
    public final HashMap activeTx = new HashMap(64);
    
    /**
     * Stores an XACOMMIT entry into the activeTx HashMap.
     *
     * @param lr LogRecord that was passed to onRecord() method.
     */
    private void activeTxPut(XALogRecord lr)
    {
      byte[] data = lr.getFields()[0];
      String key = new String(data, 1, 9);
      
      // remove any existing entry
      Object o = activeTx.remove(key);
      
      activeTx.put(key, lr.getTx());
    }
    
    private void activeTxRemove(XALogRecord lr)
    {
      if (lr.getFields().length == 0)
        return;
      
      byte[] data = lr.getFields()[0];
      // make sure we have enough data to be a DONE record
      if (lr.dataBuffer.limit() < "[xxxx.xxxx]DONE".length())
        return;
      // and make sure it is a DONE record
      if (!(new String(data, 11, 4).equals("DONE")))
        return;
      
      String key = new String(data, 1, 9);
      XACommittingTx tx = (XACommittingTx)activeTx.remove(key);
    }
    
    public int getActiveTxUsed()
    {
      return activeTx.size();
    }
    
    /* (non-Javadoc)
     * @see org.objectweb.howl.log.ReplayListener#onRecord(org.objectweb.howl.log.LogRecord)
     */
    public void onRecord(LogRecord lr) {
      assertTrue("Expecting XALogRecord, found " + lr.getClass().getName(), (lr instanceof XALogRecord));
      ++count;
      
      switch(lr.type)
      {
        case LogRecordType.END_OF_LOG:
          --count;
          break;
        case LogRecordType.XACOMMIT:
          assertTrue("lr.type", lr.isCTRL());
          assertTrue("lr.type", ((XALogRecord)lr).isCommit());
          ++commitCount;
          activeTxPut((XALogRecord)lr);
          break;
        case LogRecordType.XACOMMITMOVED:
          assertTrue("lr.type", lr.isCTRL());
          assertTrue("lr.type", ((XALogRecord)lr).isCommit());
          ++movedCount;
          activeTxPut((XALogRecord)lr);
          break;
        default:
          assertFalse("lr.type" + Long.toHexString(lr.type), lr.isCTRL());
          activeTxRemove((XALogRecord)lr);
          break;
      }
    }

    /* (non-Javadoc)
     * @see org.objectweb.howl.log.ReplayListener#onError(org.objectweb.howl.log.LogException)
     */
    public void onError(LogException exception) {
      this.exception = exception;
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
    
    openListener = new XLTReplayListener();
  }
  
  /**
   * Verify that XALogger.open() throwss
   * an UnsupportedOperationException.
   * 
   * @throws LogException
   * @throws Exception
   */
  public void testUnsupportedOpen() throws Exception
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
  public void testSingleThread() throws Exception
  {
    log.open(openListener);
    log.setAutoMark(true);
    
    prop.setProperty("msg.count", "10");
    workers = 1;
    runWorkers(XAWorker.class);
    // log.close(); called by runWorkers()
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
  public void testAutoMarkTrue() throws Exception
  {
    log.open(openListener);
    log.setAutoMark(true);

    runWorkers(XAWorker.class);
    // log.close(); called by runWorkers()
  }
  
  /**
   * Simulate a failed RM.
   * <p>write a commit record to log, but do not write the corresponding done.
   * This simulates an RM that never responds to the commit.
   * The XACOMMIT record will (should) be in the log and discovered
   * in the next test case.
   * <p>This record will be moved several times during the course
   * of subsequent tests until we finally run a test case
   * that issues a call to putDone() for this record.
   * 
   * @throws LogException
   * @throws Exception
   */
  public void testRMFailure() throws Exception
  {
    log.open(openListener);
    log.setAutoMark(false);

    XAWorker w = (XAWorker)getWorker(XAWorker.class);
    w.setWorkerIndex(FAILEDRM);
    w.logCommit(1);
    log.close();
  }
  
  /**
   * Test with automark FALSE so we can checkout the
   * log overflow processing.
   * <p>Test uses a single delayed worker.
   * 
   * @throws LogException
   * @throws Exception
   */
  public void testAutoMarkFalseOneDelayedWorker() throws Exception
  {
    log.open(openListener);
    log.setAutoMark(false);
    assertEquals("activeTxUsed", 1, log.getActiveTxUsed());
    assertNull("openListener.exception", openListener.exception);
    
    delayedWorkers = 1;
    runWorkers(XAWorker.class);
    // log.close(); called by runWorkers()
  }

  /**
   * Display the activeTx table following open.
   */
  public void testActiveTxDisplay() throws Exception
  {
    log.open(openListener);
    assertNull("openListener.exception", openListener.exception);
    assertEquals("activeTxUsed", 1, log.getActiveTxUsed());
    log.activeTxDisplay();
    log.close();
  }
  
  /**
   * Verify that number of records in the activeTx table
   * after log.open() 
   * is the same as the number of records in the activeTx 
   * table after log.replay().
   */
  public void testReplayFromAutoMark() throws Exception
  {
    log.open(openListener);
    assertNull("openListener.exception", openListener.exception);
    
    XLTReplayListener replayListener = new XLTReplayListener();
    log.replay(replayListener);
    assertNull("replayListener.exception", replayListener.exception);

    assertEquals("activeTxUsed", openListener.getActiveTxUsed(), replayListener.getActiveTxUsed());
    log.close();
  }
  
  /**
   * Verify that the XACommittingTx entry that is recovered during replay
   * can be used to complete any open transactions.
   * @throws Exception
   */
  public void testFinishIncompleteTx() throws Exception
  {
    log.open(openListener);
    assertEquals("activeTxUsed", 1, log.getActiveTxUsed());
    assertNull("openListener.exception", openListener.exception);
    
    Iterator txSet = openListener.activeTx.values().iterator();
    /*
     * we know there is only one entry (see assertEquals above)
     * but we use a while loop as a further test to be sure
     * the map is valid.
     */
    while(txSet.hasNext())
    {
      XACommittingTx tx = (XACommittingTx)txSet.next();
      byte[] record = tx.getRecord()[0];
      assertEquals("workerID", "["+FAILEDRM+".0001]",new String(record, 0, 11));

      byte[] doneRecord = ("["+FAILEDRM+".0001]DONE\n").getBytes();
      log.putDone(new byte[][] { doneRecord }, tx);
    }
    assertEquals("activeTxUsed", 0, log.getActiveTxUsed());
    log.close();
  }
  
  /**
   * Verify that the previous test actually wrote the XADONE record.
   * <p>After open() returns, there should be no entries in
   * the activeTx table.
   * 
   * @throws Exception
   */
  public void testVerifyFinishIncompleteTx() throws Exception
  {
    log.open(openListener);
    assertNull("openListener.exception", openListener.exception);

    if (log.getActiveTxUsed() > 0)
      log.activeTxDisplay(); // show any unresolved entries in the log
    assertEquals("activeTxUsed", 0, log.getActiveTxUsed());
    log.close();
  }
  
}
