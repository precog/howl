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

import java.io.File;
import java.io.FileNotFoundException;

import junit.extensions.RepeatedTest;
import junit.framework.Test;
import junit.framework.TestSuite;

public class LogTest extends TestDriver
{

  /**
   * Constructor for LogTest.
   * 
   * @param name
   */
  public LogTest(String name) {
    super(name);
  }
  
  protected void setUp() throws Exception {
    super.setUp();

    log = new Logger(cfg);

  }
  
  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  public static void main(String[] args) throws Exception {
    junit.textui.TestRunner.run(suite());
  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite(LogTest.class);
    return new RepeatedTest(suite, Integer.getInteger("LogTest.repeatcount", 1).intValue());
  }
  
  public void testGetHighMark() throws Exception {
    try {
      log.lfmgr.getHighMark();
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      ; // expected this
    }
    
    log.open();
    log.lfmgr.getHighMark();
    log.close();
  }
  
  public void testGetHighMark_NewFiles() throws Exception {
    deleteLogFiles();
    try {
      log.lfmgr.getHighMark();
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      ; // expected this
    }
    
    log.open();
    log.lfmgr.getHighMark();
    log.close();
  }
  
  public void testLoggerSingleThread()
    throws LogException, Exception
  {
    log.open();
    log.setAutoMark(true);
    
    prop.setProperty("msg.count", "10");
    workers = 1;
    runWorkers(LogTestWorker.class);
    // log.close(); called by runWorkers()

  }
  
  public void testLoggerAutomarkTrue()
    throws LogException, Exception
  {
    log.open();
    log.setAutoMark(true);

    runWorkers(LogTestWorker.class);
    // log.close(); called by runWorkers()
  }
  
  public void testLoggerReplay() throws Exception {
    log.open();
    TestLogReader reader = new TestLogReader();
    reader.run(log);
    System.err.println(getName() + "; total records processed: " + reader.recordCount);
    // log.close(); called by reader.run()
  }
  
  /**
   * Verifies that replay can begin with a log key that
   * has not been forced to the journal.
   * <p>puts a record to the journal with sync == false, then
   * trys to replay from the log key for that record.
   * @throws Exception
   */
  public void testLoggerReplay_unforcedRecord() throws Exception {
    log.open();
    long key = log.put("".getBytes(), false);
    TestLogReader reader = new TestLogReader();
    log.replay(reader, key);
    if (reader.exception != null)
      throw reader.exception;
    log.close();
  }
  
  public void testLoggerReplay_forcedRecord() throws Exception {
    log.open();
    long key = log.put("".getBytes(), true);
    TestLogReader reader = new TestLogReader();
    log.replay(reader, key);
    if (reader.exception != null)
      throw reader.exception;
    log.close();
  }
  
  public void testMultipleClose() throws Exception {
    log.open();
    log.close();
    log.close();
  }
  
  /**
   * Verify that replay works with newly created files. 
   * @throws Exception
   */
  public void testLoggerReplay_NewFiles() throws Exception {
    deleteLogFiles();
    log.open();
    TestLogReader reader = new TestLogReader();
    reader.run(log);
    assertEquals("unexpected records found in new log files", 0L, reader.recordCount);    
    // log.close(); called by reader.run()
  }
  
  public void testLogClosedException() throws Exception, LogException {
    log.open();
    log.close();
    try {
      log.setAutoMark(false);
      fail("expected LogClosedException");
    } catch (LogClosedException e) {
      // this is what we expected so ignore it
    }
  }
  
  /**
   * FEATURE 300922
   * Verify that LogConfigurationException is thrown if multiple
   * openings on the log are attempted.
   * 
   * @throws Exception
   */
  public void testLogConfigurationException_Lock() throws Exception
  {
    log.open();
    Logger log2 = new Logger(cfg);

    try {
      log2.open();
    } catch (LogConfigurationException e) {
      // this is what we expected so ignore it
      log2.close();
    }
    
    log.close();
    
  }
  /**
   * Verify that an invalid buffer class name throws LogConfigurationException.
   * <p>The LogConfigurationException occurs after the log files have been
   * opened and locked.  As a result, it is necessary to call close to 
   * unlock the files.
   * @throws Exception
   */
  public void testLogConfigurationException_ClassNotFound() throws Exception
  {
    cfg.setBufferClassName("org.objectweb.howl.log.noSuchBufferClass");
    
    try {
      log.open();
      log.close();
      fail("expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      if (!(e.getCause() instanceof ClassNotFoundException))
        throw e;
      // otherwise this is what we expected so ignore it
    }
    
    // close and unlock the log files
    log.close();
  }
  
  /**
   * Verify that log.open() will throw an exception if the configuration
   * is changed after a set of log files is created.
   * <p>In this test, we change the number of log files.
   * <p>PRECONDITION: log files exist from prior test.
   * @throws Exception
   */
  public void testLogConfigurationException_maxLogFiles() throws Exception
  {
    // increase number of log files for current set
    cfg.setMaxLogFiles(cfg.getMaxLogFiles() + 1);

    // try to open the log -- we should get an error
    try {
      log.open();
      log.close();
      fail("expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      // this is what we expected so ignore it
    }
    log.close();
  }
  
  /**
   * Verify that FileNotFoundException is processed
   * correctly.
   * 
   * <p>In order to test FileNotFoundException
   * it is necessary to devise a pathname that will fail
   * on all platforms.  The technique used here is to
   * create a file, then use the file as a directory
   * name in cfg.setLogFileDir.  So far, all file
   * systems reject the attempt to create a file
   * subordinate to another file, so this technique
   * seems to be platform neutral.
   * 
   * <p>The test fails if FileNotFoundException is
   * not thrown by the logger.
   * 
   * @throws Exception
   */
  public void testFileNotFoundException() throws Exception
  {
    // create a file (not directory) that will be used as LogFileDir for test
    File invalidDir = new File(outDir, "invalid");
    if (!invalidDir.exists() && !invalidDir.createNewFile())
        fail("unable to create 'invalid' directory");
    
    String invalid = invalidDir.getPath();
    
    // set log dir to some invalid value
    cfg.setLogFileDir(invalid);
    try {
      log.open();
      log.close();
      fail("expected FileNotFoundException");
    } catch (FileNotFoundException e) {
      // this is what we expected
    }
  }

  public void testLogConfigurationException_1File() throws Exception
  {
    // a single log file is not allowed
    cfg.setMaxLogFiles(1);

    try {
      // open should catch this and get an error
      log.open();
      log.close();
      fail("expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      // this is what we expected so ignore it
    }
  }
  
  public void testLogRecordSizeException() throws Exception {
    log.open();
    // record size == block size is guaranteed to fail
    byte[] data = new byte[cfg.getBufferSize() * 1024]; // BUG 300957
    
    try {
      log.put(data,false);
      fail("expected LogRecordSizeException");
    } catch (LogRecordSizeException e) {
      // this is what we expected so ignore it
    }
    log.close();
  }
  
  public void testInvalidLogKeyException_NegativeKey() throws Exception {
    TestLogReader tlr = new TestLogReader();
    log.open();
    
    // try log key == -1
    try {
      log.replay(tlr, -1L);
      log.close();
      fail("expected InvalidLogKeyException (-1)");
    } catch (InvalidLogKeyException e) {
      // this is what we expected
    }

    log.close();
  }

  public void testInvalidLogKeyException_InvalidKey() throws Exception {
    TestLogReader tlr = new TestLogReader();
    log.open();
    
    // try a key that is invalid
    try {
      log.replay(tlr, (log.getActiveMark() + 1L));
      log.close();
      fail("expected InvalidLogKeyException");
    } catch (InvalidLogKeyException e) {
      // this is what we expected
    } finally {
      saveStats();
    }

    log.close();
  }

  public void testBlockLogBufferSink() throws Exception {
    cfg.setBufferClassName("org.objectweb.howl.log.BlockLogBufferSink");
    log.open();
    log.setAutoMark(true);
    runWorkers(LogTestWorker.class);
    // log.close(); called by runWorkers()
  }
  
  class DataRecords {
    final int count;
    final String[] sVal;
    final byte[][] r1;
    final long[] key;
    final LogRecord lr;
    
    DataRecords(int count) {
      this.count = count;
      sVal = new String[count];
      r1   = new byte[count][];
      key  = new long[count];

      // initialize test records
      for (int i=0; i< count; ++i)
      {
        sVal[i] = "Record_" + (i+1);
        r1[i] = sVal[i].getBytes();
      }
      int last = count - 1;
      lr = new LogRecord(sVal[last].length()+6);
    }
    
    void putAll(boolean forceLastRecord) throws Exception {
      // populate journal with test records
      for (int i=0; i< count; ++i) {
        boolean force = (i == sVal.length - 1) ? forceLastRecord : false ;
        key[i] = log.put(r1[i], force); 
      }
    }
    
    LogRecord verify(int index) throws Exception {
      log.get(lr, key[index]);
      verifyLogRecord(lr, sVal[index], key[index]);
      return lr;
    }
    
    LogRecord verify(int index, LogRecord lr) throws Exception {
      verifyLogRecord(lr, sVal[index], key[index]);
      return lr;
    }
    
  }
  
  /**
   * Verify that Logger.get() method returns requested records.
   * <p>We write records to the journal than go through
   * a series of Logger.get() requests to verify that Logger.get()
   * works as expected.
   * 
   * <p>We also verify that channel position is not affected
   * by the Logger.get() methods.
   * 
   * @throws Exception
   */
  public void testGetMethods() throws Exception {
    DataRecords dr = new DataRecords(5);
    LogRecord lr = dr.lr;
    
    // make sure we are working from the beginning of a new file.
    deleteLogFiles(); 
    
    log.open();
    
    // populate journal with test records
    dr.putAll(true);
    
    // remember file position for subsequent validations
    long pos = log.lfmgr.currentLogFile.channel.position();
    
    lr.setFilterCtrlRecords(true);
    for (int i=0; i < dr.sVal.length; ++i) {
      dr.verify(i);
    }
    
    // read backwards
    for (int i = dr.sVal.length-1; i >= 0; --i) {
      dr.verify(i);
    }
    long posNow = log.lfmgr.currentLogFile.channel.position();
    assertEquals("File Position", pos, posNow);
    
    // check the Logger.getNext method
    lr = dr.verify(0);
    
    for (int i=1; i < dr.count; ++i) {
      do {
        lr = log.getNext(lr);
      } while (lr.isCTRL()); // skip control records
      dr.verify(i, lr);
    }
    posNow = log.lfmgr.currentLogFile.channel.position();
    assertEquals("File Position", pos, posNow);
    
    // now read to end of journal
    int recordCount = 0;
    while (true) {
      lr = log.getNext(lr);
      if (lr.type == LogRecordType.END_OF_LOG) break;
      ++recordCount;
    }
    posNow = log.lfmgr.currentLogFile.channel.position();
    assertEquals("File Position", pos, posNow);
    
    // read backwards, and write a new record after each read
    for (int j = 0, i = dr.count-1; i >= 0; --i, ++j)
    {
      lr = dr.verify(i);
      log.put(dr.r1[j], true);
    }
    
    // verify that we now have two sets of records
    lr = dr.verify(0);

    for (int i=1; i < dr.count; ++i) {
      do {
        lr = log.getNext(lr);
      } while (lr.isCTRL()); // skip control records
      dr.verify(i, lr);
    }
    
    // make sure have a second set of records.
    for (int i=0; i < dr.count; ++i) {
      do {
        lr = log.getNext(lr);
      } while (lr.isCTRL()); // skip control records
      verifyLogRecord(lr, dr.sVal[i], lr.key);
    }
    
    log.close();
    
  }
  
  /**
   * Verify that Logger.get() method returns a record
   * that was written with force = false.
   * 
   * @throws Exception
   */
  public void testGetMethods_UnforcedRecord() throws Exception {
    DataRecords dr = new DataRecords(1);
    log.open();
    dr.putAll(false);
    dr.verify(0);
    log.close();
  }
  
  /**
   * Verifies the content of the LogRecord is correct.
   * @param lr LogRecord to be verified
   * @param eVal expected value
   * @param eKey expected record key
   */
  void verifyLogRecord(LogRecord lr, String eVal, long eKey) {
    byte[][] r2 = lr.getFields();
    String rVal = new String(r2[0]);
    assertEquals("Record Type: " + Long.toHexString(lr.type), 0, lr.type);
    assertEquals("Record Key: " + Long.toHexString(eKey), eKey, lr.key);
    assertEquals("Record Data", eVal, rVal);
    assertEquals("Field Count != 1", 1, r2.length);
  }
  
}
