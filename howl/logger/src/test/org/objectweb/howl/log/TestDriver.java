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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.Properties;

import junit.framework.TestCase;


/**
 * HOWL JUnit test cases implement the TestDriver interface.
 * 
 * <p>The constructor for Test Worker classes take a TestDriver object
 * to gain access to the start and stop Barriers, and configuration
 * Properties.
 *  
 * @author Michael Giroux
 */
public class TestDriver extends TestCase {
  protected Logger log = null;

  protected File baseDir;

  protected File outDir;


  protected Configuration cfg = null;
  
  // output stream for test report
  protected PrintStream out = null;
  
  protected Properties prop = null;
  
  protected final Barrier startBarrier = new Barrier();
  protected final Barrier stopBarrier = new Barrier();
  
  protected int workers = 0;
  
  protected int delayedWorkers = 0;
  
  protected long delayBeforeDone = 500;
  
  protected boolean autoMarkMode = true;
  
  protected int totalTransactions = 0;
  protected long elapsedTime = 0L;
  protected float avgLatency = 0.0f;
  protected float txPerSecond = 0.0f;

  public final Properties getProperties() { return prop; }
  
  public final Barrier getStartBarrier() { return startBarrier; }

  public final Barrier getStopBarrier() { return stopBarrier; }
  
  public final Logger getLogger() { return log; }
  
  public class TestException extends Exception
  {
    public TestException() { super(); }
    public TestException(String s) { super(s); }
    public TestException(Throwable cause) { super(cause); }
    public TestException(String s, Throwable cause) { super(s, cause); }
  }
  
  /**
   * process properties for this test case
   * @throws FileNotFoundException
   * @throws IOException
   */
  protected void parseProperties() throws FileNotFoundException, IOException
  {
    String val = null;
    String key = null;
    
    prop = new Properties();
    prop.load(new FileInputStream(new File(baseDir, "src/test-resources/test.properties"))); 
    

    val = prop.getProperty( key = "test.workers", "200" );
    workers = Integer.parseInt(val);
    if (workers <= 0) throw new IllegalArgumentException(key);
    
    val = prop.getProperty( key = "test.delayedWorkers", "0");
    delayedWorkers = Integer.parseInt(val);
    if (delayedWorkers < 0) throw new IllegalArgumentException(key);
    
    val = prop.getProperty( key = "test.delayBeforeDone", "500");
    delayBeforeDone = Long.parseLong(val);
    if (delayBeforeDone < 0) throw new IllegalArgumentException(key);

}
  
  /*
   * Refresh test properties and log configuration from
   * property files.
   * <p>Individual test cases will override values as
   * needed for specific tests.
   * 
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    String baseDirName = System.getProperty("basedir", ".");
    baseDir = new File(baseDirName);
    outDir = new File(baseDir, "target/test-resources");
    outDir.mkdirs();

    parseProperties();
    cfg = new Configuration(new File(baseDir, "src/test-resources/log.properties"));
    
    String reportDir = prop.getProperty( "test.report.dir", "reports");
    if (!reportDir.endsWith("/"))
      reportDir += "/";
    
    // make sure the directory exists
    File outDir = new File(reportDir);
    outDir.mkdirs();
    
    String testName = getName();
    File outFile = new File(reportDir + testName + ".xml");
    out = new PrintStream(new FileOutputStream(outFile));
    
  }
  
  /**
   * Delete log files for tests that verify behaviour
   * against newly created files.
   */
  protected void deleteLogFiles()
  {
    String logDir = cfg.getLogFileDir();
    File dir = new File(logDir);
    File[] logs = dir.listFiles();
    
    String logFileName = cfg.getLogFileName();
    String logFileExt = cfg.getLogFileExt();
    
    // file name pattern for current test
    String pattern = "^" + logFileName + "_" + "\\d+\\." + logFileExt;

    for (int i=0; i<logs.length; ++i)
    {
      File f = logs[i];
      if (f.getName().matches(pattern))
        f.delete();
    }
  }
  
  private String propertyAsXml(String key)
  {
    return ("<" + key + ">" + System.getProperty(key) + "</" + key + ">");
  }
  
  protected void saveStats()
  {
    StringBuffer stats = new StringBuffer(
        "<?xml version='1.0' ?>" +
        "\n<TestResults>"
        );
    // append vm info
    stats.append(
        "\n<VMInfo>" +
        "\n  " + propertyAsXml("java.vm.info") +
        "\n  " + propertyAsXml("java.vm.name") +
        "\n  " + propertyAsXml("java.vm.vendor") +
        "\n  " + propertyAsXml("java.vm.version") +
        "</VMInfo>"
        );
    
    // append test metrics
    stats.append(
        "\n<TestMetrics name='" + getName() + "'>" +
        "\n  <elapsedTime value='" + elapsedTime +
        "'>Elapsed time (ms) for run</elapsedTime>" +
        "\n  <totalTransactions value='" + totalTransactions +
        "'>Total number of transactions</totalTransactions>" +
        "\n  <txPerSecond value='" + txPerSecond +
        "'>Number of transactions per second</txPerSecond>" +
        "\n  <avgLatency value='" + avgLatency +
        "'>Average Latency</avgLatency>" +
        "\n  <workers value='" + workers +
        "'>Number of worker threads</workers>" +
        "\n  <delayedWorkers value='" + delayedWorkers + 
        "'>Number of worker threads using delay between COMMIT and DONE" +
        "</delayedWorkers>" +
        "\n</TestMetrics>"
        );
    
    stats.append(log.getStats());
    
    stats.append(
        "\n</TestResults>"
        );
    
    out.println(stats.toString());
    
  }

  /*
   * @see TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    log.close();
  }

  public TestDriver(String name) {
    super(name);
  }
  
  /**
   * allocates a new instance of a worker object for the test driver.
   * 
   * @param workerClass Class object representing the type of worker to be created.
   * @return requested worker class instance
   */
  protected TestWorker getWorker(Class workerClass) throws Exception
  {
    Class cls = this.getClass().getClassLoader().loadClass(workerClass.getName());
    Constructor ctor = cls.getDeclaredConstructor(new Class[] { TestDriver.class } );
    return (TestWorker)ctor.newInstance(new Object[] {this});
  }
  
  /**
   * creates worker objects and runs the test as defined by
   * the calling testXxx() routine.
   * @param workerClass class of worker objects to create
   * @throws LogException
   * @throws Exception
   */
  protected void runWorkers(Class workerClass)
  throws LogException, Exception
  {
    TestException exception = null;
    
    if (workers <=0) throw new IllegalArgumentException();
    
    TestWorker[] worker = new TestWorker[workers];
    
    startBarrier.setCount(workers + 1);
    stopBarrier.setCount(workers + 1);

    // create the worker threads and get them started
    for (int i = 0; i < workers; ++i)
    {
      try {
        TestWorker w = getWorker(workerClass);
        worker[i] = w;
        if (i < delayedWorkers) 
        {
          w.setDelayBeforeDone(delayBeforeDone);
        }
        w.setWorkerIndex(i);   
        w.start();
      } catch (OutOfMemoryError e) {
        System.err.println(e.toString() + ": " + i + " threads created");
        throw e;
      }
    }

    synchronized(startBarrier)
    {
      while (startBarrier.getCount() > 1) startBarrier.wait();
    }

    long startTime = System.currentTimeMillis();
    startBarrier.barrier(); // put all threads into execution.

    // Wait for all the workers to finish.
    stopBarrier.barrier();
    long stopTime = System.currentTimeMillis();
    
    // close the log so we get stats for all buffers and files
    log.close();
    
    // Collect stats from workers
    long totalBytesLogged = 0L;
    int totalLatency = 0;
    totalTransactions = 0;
    for (int i = 0; i < workers; ++i)
    {
      TestWorker w = worker[i];
      totalBytesLogged += w.bytesLogged;
      totalLatency += w.latency;
      totalTransactions += w.transactions;
      if (w.exception != null)
      {
        exception = new TestException(w.exception);
      }
    }
    
    elapsedTime = stopTime - startTime;
    avgLatency = (float)totalLatency / (float)totalTransactions;
    txPerSecond = (float)(totalTransactions / (elapsedTime / 1000.0));
    
    saveStats();

    if (exception != null) throw exception;
  }

}
