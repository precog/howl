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
    log.open();
  }
  

  public static void main(String[] args) throws Exception {
    junit.textui.TestRunner.run(LogTest.class);
  }

  public void testLoggerSingleThread()
    throws LogException, Exception
  {
    log.setAutoMark(true);
    
    prop.setProperty("msg.count", "10");
    workers = 1;
    runWorkers(LogTestWorker.class);
  }
  
  public void testLoggerAutomarkTrue()
    throws LogException, Exception
  {
    log.setAutoMark(true);

    runWorkers(LogTestWorker.class);
  }
  
  public void testLoggerReplay() throws Exception, LogException {
    TestLogReader reader = new TestLogReader();
    reader.run();
    System.err.println("End Journal Validation; total records processed: " + reader.recordCount);
  }
  
  public void testLoggerThroughput() throws Exception, LogException {
    log.setAutoMark(true);
    prop.setProperty("msg.force.interval", "0");
    prop.setProperty("msg.count", "1000");
    runWorkers(LogTestWorker.class);
  }

}
