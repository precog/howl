/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 * 
 * History:
 * 05-Apr-2004 moved LogTest to package org.objectweb.howl.test.
 */
package org.objectweb.howl.test;

import java.io.File;
import java.util.Date;

import org.objectweb.howl.log.LogClosedException;
import org.objectweb.howl.log.LogException;
import org.objectweb.howl.log.LogRecord;
import org.objectweb.howl.log.LogRecordType;
import org.objectweb.howl.log.ReplayListener;

import org.objectweb.howl.log.Configuration;
import org.objectweb.howl.log.Logger;

public class LogTest
{
  File journalFile = null;

  Object startBarrier = new Object();
  Object stopBarrier = new Object();

  int startedThreads = 0;
  int stoppedThreads = 0;
  int runningThreads = 0;

  int MESSAGE_COUNT = 0;
  int MESSAGE_SIZE = 0;
  int WORKERS = 0;
  
  Logger log = null;
  
  void run()
  {
    try {
      createJournalFile();
      testXAJournalThroughput();
      testXAJournalValidate();
    } catch (LogException e) {
      System.err.println("LogException\n");
      e.printStackTrace();
    } catch (Exception e) {
      System.err.println("Exception\n");
      e.printStackTrace();
    } catch (AssertionError e) {
      System.err.println("AssertionError\n");
      e.printStackTrace();
    }
    finally
    {
      System.out.println(log.getStats());
    }
  }

  public static void main(String[] args)
  {
    new LogTest().run();
  }

  /**
   * create a journal file for use in a benchmark
   */
  void createJournalFile()
  {
    journalFile = new File(System.getProperty("howl.journal.filename", "xa.journal"));
  }

  /**
   * Tests to see what kind of throughput we can get with the current Journal.
   * 
   * @throws Exception
   */
  public void testXAJournalThroughput()
    throws Exception, LogException
  {
      Configuration cfg = new Configuration(new File("test.properties"));
      log = new Logger(cfg);
      log.open();
      log.setAutoMark(Boolean.getBoolean("howl.log.test.setautomark"));

      WORKERS = Integer.getInteger("xa.workers",1).intValue();
      MESSAGE_COUNT = Integer.getInteger("xa.msg.count",5).intValue();
      MESSAGE_SIZE = Integer.getInteger("xa.msg.size",80).intValue();
      int MESSAGE_SYNC_COUNT = Integer.getInteger("xa.msg.sync.count",2).intValue();

      long beginTime = System.currentTimeMillis();

      System.err.println(
        "Start test:" +
        "\n  WORKERS (xa.workers): " + WORKERS +
        "\n  MESSAGE_COUNT (xa.msg.count): " + MESSAGE_COUNT +
        "\n  MESSAGE_SIZE (xa.msg.size): " + MESSAGE_SIZE +
        "\n  MESSAGE_SYNC_COUNT(xa.msg.sync.count): " + MESSAGE_SYNC_COUNT +
        "\n"
      );
      
      String testName = "testXAJournalThroughput";
      long startTime = journalTest(log, MESSAGE_SYNC_COUNT, testName);
      
      log.close();
      
      long stopTime = System.currentTimeMillis();

      System.err.println(testName+":");
      printSpeedReport(log, startTime, stopTime);


      long endTime = System.currentTimeMillis();
      System.err.println("End test: elapsed time " + (endTime - beginTime) + " ms");
  }

  public void testXAJournalValidate() throws Exception, LogException {
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
      Configuration cfg = new Configuration(new File("test.properties"));
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
  private long journalTest(final Logger logger, final int MESSAGE_SYNC_COUNT, String testName)
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
              synchronized(startBarrier)
              {
                ++startedThreads;
                startBarrier.notifyAll();
                while (startedThreads < (WORKERS+1)) startBarrier.wait();
              }

              byte[] data = new byte[MESSAGE_SIZE];
              for (int i = 0; i < MESSAGE_SIZE; i++)
                data[i] = (byte) (32 + (i % 94));
              data[MESSAGE_SIZE - 2] = '\r';
              data[MESSAGE_SIZE - 1] = '\n';

              String threadName = "[xxxx]COMMIT:" + Thread.currentThread().getName() + " " ;
              int tnl = threadName.length();

              if (tnl < data.length)
                System.arraycopy(threadName.getBytes(), 0, data, 0, tnl);
              
              byte[] donerec = ("[xxxx]DONE  :" + Thread.currentThread().getName() + "\n").getBytes();
              
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
                logger.put(data, true);
                bytes += data.length;

                // journalize FORGET record
                System.arraycopy(data,1,donerec,1,4);
                logger.put(donerec, false);
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
              synchronized(stopBarrier)
              {
                ++stoppedThreads;
                stopBarrier.notifyAll();
              }
            }
            
          }
        }
        .start();
    }

    // Wait for all the workers to be ready..
    long startTime = 0;
    synchronized(startBarrier)
    {
      while (startedThreads < WORKERS) startBarrier.wait();
      ++startedThreads;
      startBarrier.notifyAll();
      startTime = System.currentTimeMillis();
    }

    System.err.println("HardenerTest.journalTest(): "+ WORKERS + " threads started");

    // Wait for all the workers to finish.
    synchronized(stopBarrier)
    {
      while(stoppedThreads < WORKERS) stopBarrier.wait();
    }
    
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
