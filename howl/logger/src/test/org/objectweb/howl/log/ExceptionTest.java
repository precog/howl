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

import java.io.IOException;

/**
 * Collection of test cases used to exercise
 * exception processing logic in HOWL.
 * <p>Any test that requires manual intervention to
 * remove drives, power off devices, ... should
 * be placed in this file.
 * 
 * @author Michael Giroux
 */
public class ExceptionTest extends TestDriver {

  /**
   * Constructor for ExceptionTest.
   * 
   * @param name
   */
  public ExceptionTest(String name) {
    super(name);
  }

  protected void setUp() throws Exception {
    super.setUp();

    log = new Logger(cfg);
  }
  

  public static void main(String[] args) throws Exception {
    junit.textui.TestRunner.run(LogTest.class);
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

}
