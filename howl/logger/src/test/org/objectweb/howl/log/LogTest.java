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
import java.util.Date;

import junit.framework.TestCase;

public class LogTest extends TestCase
{
  File journalFile = null;

  org.objectweb.howl.log.Barrier startBarrier;
  org.objectweb.howl.log.Barrier stopBarrier;

  int startedThreads = 0;
  int stoppedThreads = 0;
  int runningThreads = 0;

  int MESSAGE_COUNT = 0;
  int MESSAGE_SIZE = 0;
  int WORKERS = 0;
  
  Logger log = null;
  
  public void testXAJournal() throws Exception
  {
      doXAJournalThroughput();
      doXAJournalValidate();
      System.out.println(log.getStats());
  }

  public static void main(String[] args) throws Exception {
    new LogTest().testXAJournal();
  }

  /**
   * Tests to see what kind of throughput we can get with the current Journal.
   * 
   * @throws Exception
   */
  public void doXAJournalThroughput()
    throws Exception, LogException
  {
      Configuration cfg = new Configuration(new File("conf/log.properties"));
      log = new Logger(cfg);
      log.open();
      log.setAutoMark(Boolean.getBoolean("howl.log.test.setautomark"));

      WORKERS = Integer.getInteger("xa.workers",1).intValue();
      MESSAGE_COUNT = Integer.getInteger("xa.msg.count",5).intValue();
      MESSAGE_SIZE = Integer.getInteger("xa.msg.size",80).intValue();
      
      startBarrier = new org.objectweb.howl.log.Barrier(WORKERS + 1);
      stopBarrier = new org.objectweb.howl.log.Barrier(WORKERS + 1);

      long beginTime = System.currentTimeMillis();

      System.err.println(
        "Start test:" +
        "\n  WORKERS (xa.workers): " + WORKERS +
        "\n  MESSAGE_COUNT (xa.msg.count): " + MESSAGE_COUNT +
        "\n  MESSAGE_SIZE (xa.msg.size): " + MESSAGE_SIZE +
        "\n"
      );
      
      String testName = "testXAJournalThroughput";
      long startTime = journalTest(log, testName);
      
      log.close();
      
      long stopTime = System.currentTimeMillis();

      System.err.println(testName+":");
      printSpeedReport(log, startTime, stopTime);


      long endTime = System.currentTimeMillis();
      System.err.println("End test: elapsed time " + (endTime - beginTime) + " ms");
  }

  public void doXAJournalValidate() throws Exception, LogException {
    System.err.println("Begin Journal Validation");
    LogReader reader = new LogReader();
    reader.run();
    System.err.println("End Journal Validation; total records processed: " + reader.recordCount);
  }

  class LogReader implements ReplayListener
  {
    LogRecord logrec = new LogRecord(MESSAGE_SIZE);
    long recordCount = 0;
    long previousKey = 0;
    boolean done = false;
    
    public void onRecord(LogRecord lr)
    {
      if (lr.type == LogRecordType.END_OF_LOG)
      {
        synchronized(this) {
          done = true;
          notify();
        }
      }
      else {
        ++recordCount;
        if (lr.key <= previousKey) {
          System.err.println("Key Out of Sequence; total/prev/this: " + recordCount + " / " +
              Long.toHexString(previousKey) + " / " + Long.toHexString(lr.key));
        }
      }
    }
    public void onError(LogException e)
    {
      System.err.println(e);
      e.printStackTrace();
    }
    
    public LogRecord getLogRecord()
    {
      return logrec;
    }
    
    void run() throws Exception, LogException
    {
      Configuration cfg = new Configuration(new File("conf/log.properties"));
      Logger log = new Logger(cfg);
      log.open();
      log.replay(this, 0L);
      log.close();
      
      synchronized (this)
      {
        while(!done)
        {
          wait();
        }
      }
    }
    
  }

  final Object mutex = new Object();
  long totalBytes = 0L;
  long totalLatency = 0L;
  private long journalTest(final Logger logger, String testName)
    throws Exception
  {
    
      for (int i = 0; i < WORKERS; i++) {
        new Thread() {
          public void run() {
            long bytes = 0;
            Date today = new Date();
            long latency = 0L;

            boolean exception = false;

            try {
              startBarrier.barrier();

              byte[] data = new byte[MESSAGE_SIZE];
              byte[][] dataRec = new byte[][] { data };
              
              for (int i = 0; i < MESSAGE_SIZE; i++)
                data[i] = (byte) (32 + (i % 94));
              data[MESSAGE_SIZE - 2] = '\r';
              data[MESSAGE_SIZE - 1] = '\n';

              String threadName = "[xxxx]COMMIT:" + Thread.currentThread().getName() + " " ;
              int tnl = threadName.length();

              if (tnl < data.length)
                System.arraycopy(threadName.getBytes(), 0, data, 0, tnl);
              
              byte[] donerec = ("[xxxx]DONE  :" + Thread.currentThread().getName() + "\n").getBytes();
              byte[][] donerecRec = new byte[][] { donerec };
              
              boolean doTimeStamp = Boolean.getBoolean("howl.log.test.timeStamp");

              for (int i = 0; i < MESSAGE_COUNT; i++) {
                long latencyStart = System.currentTimeMillis();
                
                // put message number into data buffer
                int msg = i;
                for(int j = 4; j > 0; --j)
                {
                  data[j] = (byte)('0' + (msg % 10));
                  msg /= 10;
                }
                if (doTimeStamp)
                {
                  byte[] now = new Date().toString().getBytes();
                  if (now.length < (data.length - tnl))
                    System.arraycopy(now, 0, data, tnl, now.length);
                }

                // journalize COMMITTING record
                logger.put(dataRec, true);
                bytes += data.length;

                // journalize FORGET record
                System.arraycopy(data,1,donerec,1,4);
                logger.put(donerecRec, false);
                bytes += donerec.length;
                
                latency += (System.currentTimeMillis() - latencyStart);
                
              }
            
            } catch (LogClosedException e) {
              // ignore this for now
            } catch (LogException e) {
              System.err.println(Thread.currentThread().getName());
              e.printStackTrace(System.err);
              exception = true;
            } catch (Exception e) {
              System.err.println(Thread.currentThread().getName());
              e.printStackTrace(System.err);
              exception = true;
            }
            finally
            {
              if (exception)
              {
                try {logger.close(); }
                catch (LogException e) { exception = true; }
                catch (Exception e) { exception = true; }
              }
              synchronized(mutex)
              {
                totalBytes += bytes;
                totalLatency += latency;
              }
              
              stopBarrier.release();
            }
            
          }
        }
        .start();
    }

    // Wait for all the workers to be ready..
    long startTime = 0;
    synchronized(startBarrier)
    {
      while (startBarrier.getCount() > 1) startBarrier.wait();
      startTime = System.currentTimeMillis();
      startBarrier.barrier(); // put all threads into execution.
    }

    System.err.println("HardenerTest.journalTest(): "+ WORKERS + " threads started");

    // Wait for all the workers to finish.
    stopBarrier.barrier();
    
    return startTime;

  }

  void printSpeedReport(Logger journal, long startTime, long stopTime)
  {
    long mc = ((long)MESSAGE_COUNT*2)*WORKERS;
    float kb = (((float)totalBytes)/(1024));
    long duration = (stopTime - startTime);
    long avgLatency = totalLatency / (MESSAGE_COUNT * WORKERS);
    System.err.println(" wrote: "+mc+" messages in " + duration +" ms. ("+(mc*1000/(duration))+" m/s)");
    System.err.println(" wrote: "+kb+" kb in " + duration +" ms. ("+(kb*1000/(duration))+" kb/s)");
    System.err.println(" average latency: " + avgLatency + " ms.");
  }
  
}
