/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */
package org.objectweb.howl.log;

import java.io.File;

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
    } catch (Exception e) {
      System.out.println("Exception\n");
      e.printStackTrace();
    } catch (LogException e) {
      System.out.println("LogException\n");
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
    throws Exception, LogClosedException, LogFileOverflowException
  {
      log = new Logger();
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

  public void testXAJournalValidate() throws Exception {
    System.err.println("Begin Journal Validation");
//    XAJournalReader reader = new XAJournalReader();
//    reader.open(journalFile);

//    int maxRecordSize = reader.getMaxRecordSize();
//    byte[] data = new byte[maxRecordSize];

  }

  final Object mutex = new Object();
  long totalBytes = 0;
  private long journalTest(final Logger logger, final int MESSAGE_SYNC_COUNT, String testName)
    throws Exception
  {
    
      for (int i = 0; i < WORKERS; i++) {
        new Thread() {
          public void run() {
            long bytes = 0;
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
              String threadName = "[xxxx]COMMIT:" + Thread.currentThread().getName() + " ";
              int tnl = threadName.length();

              if (tnl < data.length)
                System.arraycopy(threadName.getBytes(), 0, data, 0, tnl);
              
              byte[] donerec = ("[xxxx]DONE  :" + Thread.currentThread().getName() + "\n").getBytes();

              for (int i = 0; i < MESSAGE_COUNT; i++) {
                // put message number into data buffer
                int msg = i;
                for(int j = 4; j > 0; --j)
                {
                  data[j] = (byte)('0' + (msg % 10));
                  msg /= 10;
                }

                // journalize COMMITTING record
                logger.put(data, true);
                bytes += data.length;

                // journalize FORGET record
                System.arraycopy(data,1,donerec,1,4);
                logger.put(donerec, false);
                bytes += donerec.length;
                
              }
            
            } catch (Exception e) {
              System.err.println(Thread.currentThread().getName());
              e.printStackTrace(System.err);
              exception = true;
            } catch (LogClosedException e) {
              // ignore this for now
            } catch (LogException e) {
              System.err.println(Thread.currentThread().getName());
              e.printStackTrace(System.err);
              exception = true;
            }
            finally
            {
              if (exception)
              {
                try {logger.close(); }
                catch (Exception e) { exception = true; }
              }
              synchronized(mutex)
              {
                totalBytes += bytes;
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
    System.err.println(" wrote: "+mc+" messages in " + duration +" ms. ("+(mc*1000/(duration))+" m/s)");
    System.err.println(" wrote: "+kb+" kb in " + duration +" ms. ("+(kb*1000/(duration))+" kb/s)");
  }
}
