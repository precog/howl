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
import java.util.Vector;

import junit.extensions.RepeatedTest;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class LogTest extends TestDriver
{

  private File baseDir;
  private File outDir;
  
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

    String baseDirName = System.getProperty("basedir", ".");
    baseDir = new File(baseDirName);
    outDir = new File(baseDir, "target/test-resources");
    outDir.mkdirs();
  }
  
  public static void main(String[] args) throws Exception {
    junit.textui.TestRunner.run(suite());
  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite(LogTest.class);
    return new RepeatedTest(suite, Integer.getInteger("LogTest.repeatcount", 1).intValue());
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
  
  public void testLoggerReplay() throws Exception, LogException {
    log.open();
    TestLogReader reader = new TestLogReader();
    reader.run(log);
    System.err.println(getName() + "; total records processed: " + reader.recordCount);
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
    byte[] data = new byte[cfg.getBufferSize()];
    
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
  
  /**
   * Verify that Logger.get() method returns requested records.
   * <p>We write three records to the journal than go through
   * a series of Logger.get() requests to verify that Logger.get()
   * works as expected.
   * @throws Exception
   */
  public void testGetMethods() throws Exception {
    String[] sVal = { "Record_1", "Record_2", "Record_3", "Record_4", "Record_5"};
    byte[][] r1 = new byte[sVal.length][];
    
    // initialize test records
    for (int i=0; i< sVal.length; ++i)
      r1[i] = sVal[i].getBytes();
    
    long[] key = new long[sVal.length];
    
    log.open();
    
    // populate journal with test records
    for (int i=0; i< sVal.length; ++i)
      key[i] = log.put(r1[i],false);
    
    // force the data to disk
    log.put("EOB".getBytes(),true);
    
    LogRecord lr = new LogRecord(sVal[0].length()+6);
    lr.setFilterCtrlRecords(true);
    for (int i=0; i < sVal.length; ++i)
    {
      lr = log.get(lr,key[i]);
      verifyLogRecord(lr, sVal[i], key[i]);
    }
    
    // read backwards
    for (int i = sVal.length-1; i >= 0; --i)
    {
      lr = log.get(lr, key[i]);
      verifyLogRecord(lr, sVal[i], key[i]);
    }
    
    // check the Logger.getNext method
    lr = log.get(lr, key[0]);
    verifyLogRecord(lr, sVal[0], key[0]);
    
    for (int i=1; i < sVal.length; ++i) {
      do {
        lr = log.getNext(lr);
      } while (lr.isCTRL()); // skip control records
      verifyLogRecord(lr, sVal[i], key[i]);
    }
    
    // now read to end of journal
    int recordCount = 0;
    while (true) {
      lr = log.getNext(lr);
      if (lr.type == LogRecordType.END_OF_LOG) break;
      ++recordCount;
    }
    
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
    assertEquals("Field Count != 1", 1, r2.length);
    assertEquals("Record Data", eVal, rVal);
    assertEquals("Record Key", eKey, lr.key);
  }
  
}
