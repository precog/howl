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
 * $Id$
 * ------------------------------------------------------------------------------
 */
package org.objectweb.howl.log;


/**
 * Test cases in this class operate on log files > 2 GB.
 * <p>
 * Testers may want to exclude this test from automatic builds
 * when testing on systems with slow disk devices.
 */

public class BigLogTest extends TestDriver {

  public BigLogTest(String name) {
    super(name);
    // TODO Auto-generated constructor stub
  }

  protected void setUp() throws Exception {
    super.setUp();

    log = new Logger(cfg);

  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }


  /**
   * BUG 306425 - verify that app can read and replay records when 
   * BSN is > Integer.MAX_VALUE
   * <p>
   * This test should fail if fix for 306425 is not applied.
   * 
   * @throws Exception
   */
  public void testGetMethod_NegativeBSN() throws Exception {
    DataRecords dr = new DataRecords(log, 5);
    LogRecord lr = dr.lr;

    // make sure we are working from the beginning of a new file.
    deleteLogFiles();
    log.open();

    // force BSN to be negative
    log.bmgr.init(log.lfmgr, Integer.MAX_VALUE+1);
    
    // populate journal with test records
    dr.putAll(true);
    
    // verify that we can read a record
    dr.verify(0);
    
    log.close();
  }
  
  public void testLoggerReplay_NegativeMark() throws Exception {
    DataRecords dr = new DataRecords(log, 5);

    // make sure we are working from the beginning of a new file.
    deleteLogFiles();
    log.open();

    // force BSN to be negative
    log.bmgr.init(log.lfmgr, Integer.MAX_VALUE+1);

    // populate journal with test records and set mark at first test record
    dr.putAll(true);
    log.mark(dr.key[0]);
    
    long key = dr.key[0];
    System.out.println(Long.toHexString(key));
    TestLogReader reader = new TestLogReader();
    log.replay(reader, key);
    log.close();
    if (reader.exception != null)
      throw reader.exception;
  }

  public void testLoggerReplay_NegativeActiveMark() throws Exception {
    DataRecords dr = new DataRecords(log, 5);

    // make sure we are working from the beginning of a new file.
    deleteLogFiles();
    log.open();

    // force BSN to be negative
    log.bmgr.init(log.lfmgr, Integer.MAX_VALUE+1);

    // populate journal with test records and set mark at first test record
    dr.putAll(true);
    log.mark(dr.key[0]);
    
    long key = dr.key[0];
    System.out.println(Long.toHexString(key));
    TestLogReader reader = new TestLogReader();
    log.replay(reader);
    log.close();
    if (reader.exception != null)
      throw reader.exception;
  }
  

  /**
   * Verify that we can create a log that is larger than 2 Gb.
   * @throws Exception
   */
  public void test2GigLog() throws Exception {

    // define a log that will be larger than 2 Gb
    cfg.setBufferSize(4);
    cfg.setMaxBlocksPerFile(Integer.MAX_VALUE);
    
    deleteLogFiles();
    log.open();
    log.setAutoMark(true);
    
    // Generate enough log records to fill > 2Gb (this will take a while)
    prop.setProperty("msg.count", "20000");
    workers = 1000;
    runWorkers(LogTestWorker.class);
    
  }
  
  public void test2GigLogRestart() throws Exception {
    DataRecords dr = new DataRecords(log, 50);

    // define a log that will be larger than 2 Gb
    cfg.setBufferSize(4);
    cfg.setMaxBlocksPerFile(Integer.MAX_VALUE);
    
    // open log again and add a few more records
    log.open();
    log.setAutoMark(false);

    dr.putAll(false);
    log.mark(dr.key[0], true);
    dr.putAll(true);
    
    // now try to replay from the mark
    TestLogReader reader = new TestLogReader();
    log.replay(reader);
  }

  

}
