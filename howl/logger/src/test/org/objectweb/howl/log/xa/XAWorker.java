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

import java.util.Date;
import java.util.Properties;

import org.objectweb.howl.log.LogException;
import org.objectweb.howl.log.xa.XACommittingTx;
import org.objectweb.howl.log.xa.XALogger;

/**
 * The XAWorker class is used by test cases to write
 * XA 2-phase-commit record sequences to a log.
 * 
 * @author Michael Giroux
 */
public class XAWorker extends Thread {
  /**
   * configuration properties for the test case.
   */
  final Properties config;
  
  /**
   * byte[] containing data to be logged for COMMIT record.
   * <p>default size is 80 bytes.
   * <p>Configuration property: <b> msg.size </b>
   */
  byte[] commitData = new byte[80];
  byte[][] commitDataRecord = new byte[][] { commitData };
  
  /**
   * byte[] containing data to be logged for DONE records.
   * <p>initialized by initDoneData().
   */
  byte[] doneData = null;
  byte[][] doneDataRecord = new byte[1][];
  
  /**
   * number of COMMIT messages to be generated.
   * 
   * <p>default is 200.
   * <p>Configuration property: <b> msg.count </b> 
   */
  int count = 200;
  
  /**
   * When set <b> true </b> each COMMIT message contains
   * the formatted TOD the record was generated.
   *  
   * <p>The information may be interesting during debug and
   * analysis of log records, but does use a bit of CPU.
   */
  boolean doTimeStamp = false;
  
  /**
   * total latency time for all COMMIT/DONE message
   * pairs written by this XAWorker.
   */
  long latency = 0L;

  /**
   * reference to our test driver.
   */
  final org.objectweb.howl.log.TestDriver driver;
  
  /**
   * XALogger obtained from test driver.
   */
  final XALogger log;
  
  /**
   * total number of bytes logged by this XAWorker.
   */
  long bytesLogged = 0L;
  
  /**
   * thread name length.
   * <p>initialized in initCommitData()
   */
  int tnl = 0;
  
  /**
   * any exception encountered by run() will be saved here.
   */
  Exception exception = null;
  
  /**
   * number of transactions logged by this worker.
   */
  int transactions = 0;
  
  /**
   * number of ms to delay between commit and done records.
   * <p>Test driver sets the delay to allow testing of
   * log overflow.
   * <p>Default is 0 - no delay
   * <p>Set using setDelayBeforeDone(int)
   */
  long delayBeforeDone = 0;
  
  /**
   * parse the configuration properties.
   */
  void parseProperties()
  {
    String val = null;
    String key = null;
    int ival;
    
    val = config.getProperty( key = "msg.size" ).trim();
    if (val != null)
    {
      ival = Integer.parseInt(val);
      if (ival <= 0) throw new IllegalArgumentException();
      if (ival != commitData.length)
        commitData = new byte[ival];
    }
    
    val = config.getProperty( key = "msg.count" ).trim();
    if (val != null)
    {
      ival = Integer.parseInt(val);
      if (ival <= 0) throw new IllegalArgumentException();
      count = ival;
    }
    
    val = config.getProperty ( key = "msg.timestamp", "false").trim().toLowerCase();;
    doTimeStamp = val.equals("true"); 
  }
  
  /**
   * initializes the commitData array with data to be written to the log.
   */
  void initCommitData()
  {
    int msgSize = commitData.length;
    
    for (int i = 0; i < msgSize; i++)
      commitData[i] = (byte) (32 + (i % 94));
    commitData[msgSize - 2] = '\r';
    commitData[msgSize - 1] = '\n';

    String threadName = "[xxxx]COMMIT:" + Thread.currentThread().getName() + " " ;
    tnl = threadName.length();

    if (tnl < commitData.length)
      System.arraycopy(threadName.getBytes(), 0, commitData, 0, tnl);
  }
  
  /**
   * initializes the doneData array with data to be written to the log.
   */
  void initDoneData()
  {
    doneData = ("[xxxx]DONE  :" + 
        Thread.currentThread().getName() + 
        "\n").getBytes();
    doneDataRecord[0] = doneData;
  }
  
  /**
   * constructs an XAWorker thread instance.
   * 
   * @param driver provides access to configuration information
   * and other features supplied by the test driver.
   */
  XAWorker(org.objectweb.howl.log.TestDriver driver)
  {
    this.driver = driver;
    config = driver.getProperties();
    log = driver.getXALogger();
  }
  
  /**
   * Sets the delay time between commit and done records.
   * 
   * @param delayBeforeDone number of ms to delay between
   * commit and done.
   */
  void setDelayBeforeDone(long delayBeforeDone)
  {
    this.delayBeforeDone = delayBeforeDone;
  }
  
  
  XACommittingTx logCommit(int id)
  throws LogException, Exception
  {
    // put message number into data buffer
    int msg = id;
    for(int j = 4; j > 0; --j)
    {
      commitData[j] = (byte)('0' + (msg % 10));
      msg /= 10;
    }
    
    if (doTimeStamp)
    {
      byte[] now = new Date().toString().getBytes();
      if (now.length < (commitData.length - tnl))
        System.arraycopy(now, 0, commitData, tnl, now.length);
    }

    // journalize COMMITTING record
    bytesLogged += commitData.length;
    return log.putCommit(commitDataRecord);
  }
  
  void logDone(XACommittingTx tx)
  throws LogException, Exception
  {
    // journalize FORGET record
    System.arraycopy(commitData,1,doneData,1,4);
    log.putDone(doneDataRecord, tx);
    bytesLogged += doneData.length;
    ++transactions;
  }
  
  public void run()
  {
    
    parseProperties();
    
    initCommitData();
    
    initDoneData();
    
    // recuce count if this worker is doing delays between COMMIT and DONE 
    if (delayBeforeDone > 0)
      count = 4;

    try
    {
      // wait till all worker threads have started
      driver.getStartBarrier().barrier();
      
      // generate the log records
      for (int i = 0; i < count; ++i)
      {
        long startTime = System.currentTimeMillis();
        XACommittingTx tx = logCommit(i);
        if (delayBeforeDone > 0) {
          sleep(delayBeforeDone);
        }
        logDone(tx);
        latency += System.currentTimeMillis() - startTime;
      }
    } catch (Exception e) {
      exception = e;
    } finally {
      // notify driver that this thread has finishe its work
      driver.getStopBarrier().release();
    }
  }
}
