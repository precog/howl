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

import org.objectweb.howl.log.LogException;
import org.objectweb.howl.log.TestDriver;
import org.objectweb.howl.log.TestWorker;
import org.objectweb.howl.log.xa.XACommittingTx;
import org.objectweb.howl.log.xa.XALogger;

/**
 * The XAWorker class is used by test cases to write
 * XA 2-phase-commit record sequences to a log.
 * 
 * @author Michael Giroux
 */
public class XAWorker extends TestWorker {
  /**
   * Constructor upcasts the TestWorker.log member to an
   * XALogger for these tests.
   */
  final XALogger xalog;
  
  /**
   * constructs an XAWorker thread instance.
   * 
   * @param driver provides access to configuration information
   * and other features supplied by the test driver.
   */
  public XAWorker(TestDriver driver)
  {
    super(driver);
    xalog = (XALogger)log;
  }
  
  
  public XACommittingTx logCommit(int id)
      throws LogException, Exception
  {
    // journalize COMMIT record
    updateRecordData(id);
    bytesLogged += commitData.length;
    return xalog.putCommit(commitDataRecord);
  }
  
  public void logDone(XACommittingTx tx)
  throws LogException, Exception
  {
    // journalize FORGET record
    xalog.putDone(doneDataRecord, tx);
    bytesLogged += doneData.length;
    ++transactions;
  }
  
  public void run()
  {
    // recuce count if this worker is doing delays between COMMIT and DONE 
    if (delayBeforeDone > 0)
      count = 4;

    try
    {
      // wait till all worker threads have started
      driver.getStartBarrier().barrier();
      
      // generate the log records
      for (int i = 1; i <= count; ++i)
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
