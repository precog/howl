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
 * $Id: ExceptionTest.java,v 1.7 2005-11-21 17:51:26 girouxm Exp $
 * ------------------------------------------------------------------------------
 */
package org.objectweb.howl.log;

import junit.extensions.RepeatedTest;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;

/**
 * Collection of test cases used to exercise
 * exception processing logic in HOWL.
 * <p>Any test that requires manual intervention to
 * remove drives, power off devices, ... should
 * be placed in this file.
 * 
 * <p>These tests may not execute properly on
 * all platforms.  This testcase should not
 * be included in standard build scripts
 * to avoid unexpected failures.
 * 
 * @author Michael Giroux
 */
public class ExceptionTest extends TestDriver
{

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
    // start with new files
    deleteLogFiles();
    
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
   * <p>We cannot use the worker infrastructure because
   * we need to write to two logs so we can validate the
   * results.
   * <p>The test requires that we cause an IOException
   * then run a separate test to verify file content. 
   * 
   * @throws Exception
   */
  public void testVerifyMode_rw() throws Exception
  {
  }

  /**
   * Verify that putting records into the journal without moving the mark
   * will result in a LogFileOverflowException.
   * 
   * <b>TODO: </b>We need to figure out how to examine the log files
   * to confirm that nothing was overwritten.  We also need to figure
   * out how to restart using this set of files once the overflow condition
   * has occurred.
   * @throws Exception
   */
  public void testLogFileOverflowException() throws Exception {
    deleteLogFiles(); // start with fresh files.
    log.open();
    log.setLogEventListener(new LogEventListener_LFOE(log));
    log.setAutoMark(false);
    prop.setProperty("msg.count", "1000");
    workers = 50;
    try {
      runWorkers(LogTestWorker.class);
      assertFalse(getName() + ": LogFileOverflowException expected.", this.exception == null);
    } catch (LogFileOverflowException e) {
      System.err.println(getName() + ": ignoring LogFileOverflowException");
      // ignore -- we expected it to occur
    }
    finally {
      deleteLogFiles(); // clean up the mess for next test
    }
    // log.close(); called by runWorkers
  }
  
  /**
   * LogEventListener methods.
   */
  class LogEventListener_AIOOB implements LogEventListener
  {
    final Logger log;
    
    LogEventListener_AIOOB(Logger log)
    {
      this.log = log;
    }
    
    public void logOverflowNotification(long logkey) {
      // move the mark 
      try {
        log.mark(logkey);
      } catch (Exception e) {
        e.printStackTrace();
      }
      // generate a RuntimeException ( ArrayIndexOutOfBounds ) to test the catch in EventManager thread
      byte[] b = new byte[1];
      b[1] = 0;
    }
    
    public boolean isLoggable(int level) {
      // TODO Auto-generated method stub
      return false;
    }

    public void log(int level, String message) {
      // TODO Auto-generated method stub
      
    }

    public void log(int level, String message, Throwable thrown) {
      // TODO Auto-generated method stub
      
    }

  }

  
  /**
   * Verify that LogFileManager.EventManager traps RuntimeExceptions in application code
   */ 
  public void testLogOverflowNotificationException() throws Exception
  {
    deleteLogFiles();  // start with fresh files
    
    // start the log with a lot of work to force an overflow notification.
    log.open();
    log.setLogEventListener(new LogEventListener_AIOOB(log));
    prop.setProperty("msg.count", "1000");
    workers = 50;
    try {
      runWorkers(LogTestWorker.class);
    } catch (LogFileOverflowException e) {
      System.err.println(getName() + ": ignoring LogFileOverflowException");
      // ignore -- we expected it to occur
    }
    finally {
      deleteLogFiles(); // clean up the mess for next test
    }
  }

  /**
   * LogEventListener used by testLogFileOverflowNotification.
   */
  class LogEventListener_LFOE implements LogEventListener
  {
    final Logger log;
    LogEventListener_LFOE(Logger log)
    {
     this.log = log; 
    }
    
    public void logOverflowNotification(long logkey) {
      System.err.println(getName() + ": logOverflowNotification received" +
          "\n  activeMark: " + log.getActiveMark() +
          "\n  logkey: " + Long.toHexString(logkey));
      for (int i = 0; i < workers; ++i)
        worker[i].setException(new LogFileOverflowException());
      
    }

    public boolean isLoggable(int level) {
      return false;
    }

    public void log(int level, String message) {
    }

    public void log(int level, String message, Throwable thrown) {
    }

  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite(ExceptionTest.class);
    return new RepeatedTest(suite, Integer.getInteger("ExceptionTest.repeatcount", 1).intValue());
  }
  
}
