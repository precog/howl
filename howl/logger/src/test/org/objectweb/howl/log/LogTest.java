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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class LogTest extends TestDriver
{
  /**
   * Constructor for XALoggerTest.
   * @param name
   */
  public LogTest(String name) {
    super(name);
  }
  
  protected void setUp() throws Exception {
    super.setUp();

    log = new Logger(cfg);
  }
  

  public static void main(String[] args) throws Exception {
    junit.textui.TestRunner.run(LogTest.class);
  }

  public void testLoggerSingleThread()
    throws LogException, Exception
  {
    log.open();
    log.setAutoMark(true);
    
    prop.setProperty("msg.count", "10");
    workers = 1;
    runWorkers(LogTestWorker.class);
  }
  
  public void testLoggerAutomarkTrue()
    throws LogException, Exception
  {
    log.open();
    log.setAutoMark(true);

    runWorkers(LogTestWorker.class);
  }
  
  public void testLoggerReplay() throws Exception, LogException {
    log.open();
    TestLogReader reader = new TestLogReader();
    reader.run();
    System.err.println("End Journal Validation; total records processed: " + reader.recordCount);
  }
  
  public void testLoggerThroughput_rw() throws Exception, LogException {
    log.open();
    log.setAutoMark(true);
    prop.setProperty("msg.force.interval", "0");
    prop.setProperty("msg.count", "1000");
    runWorkers(LogTestWorker.class);
  }
  
  public void testLoggerThroughput_rwd() throws Exception, LogException {
    cfg.setLogFileMode("rwd");
    log.open();
    
    log.setAutoMark(true);
    prop.setProperty("msg.force.interval", "0");
    prop.setProperty("msg.count", "1000");
    runWorkers(LogTestWorker.class);
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
  
  public void testLoggerThroughput_checksumEnabled() throws Exception
  {
    cfg.setChecksumEnabled(true);
    log.open();
    runWorkers(LogTestWorker.class);
  }
  
  public void testLogConfigurationException_maxLogFiles() throws Exception
  {
    // increase number of log files for current set
    cfg.setMaxLogFiles(cfg.getMaxLogFiles() + 1);

    // try to open the log -- we should get an error
    try {
      log.open();
      fail("expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      // this is what we expected so ignore it
    }
  }
  
  public void testFileNotFoundException() throws Exception
  {
    // set log dir to some invalid value
    cfg.setLogFileDir(prop.getProperty("test.invalid.dir", "$:/logs"));
    try {
      log.open();
      fail("expected FileNotFoundException");
    } catch (FileNotFoundException e) {
      // this is what we expected
    }
  }
  
  public void testInvalidFileSetException_1() throws Exception
  {
    // a single log file is not allowed
    cfg.setMaxLogFiles(1);
    
    // open should catch this and get an error
    try {
      log.open();
      fail("expected InvalidFileSetException");
    } catch (InvalidFileSetException e) {
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
 
  }
  
  public void testInvalidLogKeyException() throws Exception {
    TestLogReader tlr = new TestLogReader();
    log.open();
    
    // try log key == -1
    try {
      log.replay(tlr, -1L);
      fail("expected InvalidLogKeyException");
    } catch (InvalidLogKeyException e) {
      // this is what we expected
    }
    
    // try a key that is invalid
    try {
      log.replay(tlr, (log.getActiveMark() + 1L));
      fail("expected InvalidLogKeyException");
    } catch (InvalidLogKeyException e) {
      // this is what we expected
    }

  }

  public void testBlockLogBufferSink() throws Exception {
    cfg.setBufferClassName("org.objectweb.howl.log.BlockLogBufferSink");
    log.open();
    log.setAutoMark(true);
    runWorkers(LogTestWorker.class);
  }

  /**
   * Verify that test terminates with an IOException.
   * <p>Remove log drive to cause IOExcepion during test.  
   * This test will probably hang if IOException is not
   * reported properly.
   * 
   * @throws Exception
   */
  public void testIOException() throws Exception {
    String defDir = cfg.getLogFileDir(); // so test runs if test.ioexception.dir not defined
    String logDir = prop.getProperty("test.ioexception.dir", defDir);
    cfg.setLogFileDir(logDir);
    log.open();
    log.setAutoMark(true);
    prop.setProperty("msg.count", "100");
    System.err.println("Begin " + getName() +
        "\n  remove " + logDir + " to generate IOException");
    try {
      runWorkers(LogTestWorker.class);
      fail("Expected an IOException");
    } catch (TestException e) {
      Throwable cause = e.getCause();
      assertTrue(cause instanceof IOException);
    }
    System.err.println("End " + getName());
  }
  
  /**
   * Verify that file mode "rw" works as expected.
   * 
   * @throws Exception
   */
  public void testVerifyMode_rw() throws Exception
  {
    
  }

}
