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

import java.util.Date;
import java.util.Properties;

import org.objectweb.howl.log.Logger;

/**
 * The TestWorker class serves as a base class for
 * specific test worker implementations.
 * 
 * <p>Implementations of this class must provide
 * a public void run() method.
 * 
 * @author Michael Giroux
 */
public abstract class TestWorker extends Thread {

  /**
   * configuration properties for the test case.
   */
  protected final Properties config;
  
  /**
   * byte[] containing data to be logged for COMMIT record.
   * <p>default size is 80 bytes.
   * <p>Configuration property: <b> msg.size </b>
   */
  protected int msgSize = 80;
  protected final byte[] commitData;
  protected final byte[][] commitDataRecord = new byte[1][];
  
  /**
   * byte[] containing data to be logged for DONE records.
   * <p>initialized by initDoneData().
   */
  protected final byte[] doneData;
  protected final byte[][] doneDataRecord = new byte[1][];
  
  /**
   * number of COMMIT messages to be generated.
   * 
   * <p>default is 50.
   * <p>Configuration property: <b> msg.count </b> 
   */
  protected int count = 50;
  
  /**
   * When set <b> true </b> each COMMIT message contains
   * the formatted TOD the record was generated.
   *  
   * <p>The information may be interesting during debug and
   * analysis of log records, but does use a bit of CPU.
   */
  protected boolean doTimeStamp = false;
  
  /**
   * total latency time for all COMMIT/DONE message
   * pairs written by this XAWorker.
   */
  public long latency = 0L;

  /**
   * reference to our test driver.
   */
  protected final TestDriver driver;
  
  /**
   * XALogger obtained from test driver.
   */
  protected final Logger log;
  
  /**
   * total number of bytes logged by this XAWorker.
   */
  protected long bytesLogged = 0L;
  
  /**
   * thread name length.
   * <p>initialized in initCommitData()
   */
  protected int tnl = 0;
  
  /**
   * any exception encountered by run() should be saved here.
   */
  protected Exception exception = null;
  
  /**
   * number of transactions logged by this worker.
   */
  protected int transactions = 0;
  
  /**
   * number of ms to delay between commit and done records.
   * <p>Test driver sets the delay to allow testing of
   * log overflow.
   * <p>Default is 0 - no delay
   * <p>Set using setDelayBeforeDone(int)
   */
  protected long delayBeforeDone = 0;
  
  /**
   * parse the configuration properties.
   */
  void parseProperties()
  {
    String val = null;
    String key = null;
    
    val = config.getProperty( key = "msg.size", Integer.toString(msgSize)).trim();
    msgSize = Integer.parseInt(val);
    if (msgSize <= 0) throw new IllegalArgumentException(key + "must be > 0");
    
    val = config.getProperty( key = "msg.count", Integer.toString(count)).trim();
    count = Integer.parseInt(val);
    if (count <= 0) throw new IllegalArgumentException(key + "must be > 0");
    
    val = config.getProperty ( key = "msg.timestamp", "false").trim().toLowerCase();;
    doTimeStamp = val.equals("true"); 
  }
  
  /**
   * initializes the commitData array with data to be written to the log.
   */
  byte[] initCommitData()
  {
    byte[] commitData = new byte[msgSize];

    int msgSize = commitData.length;
    
    for (int i = 0; i < msgSize; i++)
      commitData[i] = (byte) (32 + (i % 94));
    commitData[msgSize - 2] = '\r';
    commitData[msgSize - 1] = '\n';

    String threadName = "[xxxx]COMMIT:" + Thread.currentThread().getName() + " " ;
    tnl = threadName.length();

    if (tnl < commitData.length)
      System.arraycopy(threadName.getBytes(), 0, commitData, 0, tnl);
    
    return commitData;
  }
  
  /**
   * initializes the doneData array with data to be written to the log.
   */
  byte[] initDoneData()
  {
    byte [] doneData = ("[xxxx]DONE  :" + 
        Thread.currentThread().getName() + 
        "\n").getBytes();

    return doneData;
  }
  
  /**
   * constructs an XAWorker thread instance.
   * 
   * @param driver provides access to configuration information
   * and other features supplied by the test driver.
   */
  protected TestWorker(TestDriver driver)
  {
    this.driver = driver;
    config = driver.getProperties();
    log = driver.getXALogger();
    
    parseProperties();
    
    commitData = initCommitData();
    commitDataRecord[0] = commitData;
    
    doneData = initDoneData();
    doneDataRecord[0] = doneData;
    
  }
  
  /**
   * Sets the delay time between commit and done records.
   * 
   * @param delayBeforeDone number of ms to delay between
   * commit and done.
   */
  public void setDelayBeforeDone(long delayBeforeDone)
  {
    this.delayBeforeDone = delayBeforeDone;
  }
  
}
