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

import java.io.IOException;

import org.objectweb.howl.log.Configuration;
import org.objectweb.howl.log.InvalidLogKeyException;
import org.objectweb.howl.log.LogClosedException;
import org.objectweb.howl.log.LogFileOverflowException;
import org.objectweb.howl.log.LogRecordSizeException;
import org.objectweb.howl.log.Logger;
import org.objectweb.howl.log.LogEventListener;

/**
 * A very specialized subclass of {@link org.objectweb.howl.log.Logger}
 * intended to provide functionality required by any XA Transaction Manager.
 * 
 * @author Michael Giroux
 */
public class XALogger extends Logger
  implements LogEventListener
{
  /**
   * array of transactions in COMMIT phase and
   * waiting for DONE.
   * <p>Array is grown as needed to accomodate
   * larger number of transactions in COMMITING state.
   * <p>When the Logger detects that a log file overflow
   * condition is about to occur 
   * {@link org.objectweb.howl.log.LogEventListener#logOverflowNotification(long)}
   * is invoked to allow the application to move older records
   * forward in the log.  This approach avoides the need to
   * move records forward every time a log file switch occurs.
   * <p>During the logOverflowNotification, activeTx[] is scanned
   * for entries with log keys older than the key specified
   * on the notification.  Since this processing only needs to
   * occur when a log file is about to overflow, the array is
   * used as an alternative to a linked list to eliminate the
   * overhead of managing the linked list for every
   * log record.   
   */
  XACommittingTx[] activeTx = null;
  
  /**
   * array of available XACommittingTx objects.
   * <p>Each entry in the array is associated with 
   * an entry in activeTx[].
   * <ul>
   * <li>Entries are obtained using the atxGet member.</li>
   * <li>Entries are returned to the list using the atxPut member.</li>
   * <li>The activeTx and availableTx arrays are grown as needed.</li>
   * </ul>
   */
  XACommittingTx[] availableTx = null;
  
  /**
   * index into availableTx[] used to remove an
   * entry.
   */
  int atxGet = 0;
  
  /**
   * index into availableTx[] used to return (add)
   * an entry.
   */
  int atxPut = 0;
  
  /**
   * number of used entries in activeTx table.
   * <p>table is grown when <i> atxUsed </i> is
   * equal to the size of the table and we are
   * trying to add one more entry. 
   */
  int atxUsed = 0;
  
  /**
   * maximum number of used entries.
   */
  int maxAtxUsed = 0;
  
  /**
   * lock used to synchronize access to the 
   * availableTx array, atxGet and atxPut
   * members.
   */
  final Object activeTxLock = new Object();
  
  /**
   * number of times the activeTx table was resized
   * to accomodate a larger number of transactions
   * in the COMMITTING state.
   */
  int growActiveTxArrayCount = 0;
  
  /**
   * number of times log overflow notification event was called.
   */
  int overflowNotificationCount = 0;
  
  /**
   * number of records moved by log overflow notification processor.
   */
  int movedRecordCount = 0;
  
  /**
   * number of ms that threads waited for overflow processing to complete.
   */
  long totalWaitForThis = 0L;
  
  /**
   * number of times threads waited for overflow processing to complete.
   */
  int waitForThisCount = 0;
  
  /**
   * log key below which COMMIT records will be copied forward
   * by logOverflowNotification to avoid log overflow exceptions.
   * 
   * <p>synchronized on <i> this </i>
   */
  long overflowFence = 0L;
  
  /**
   * Resize the activeTx and availableTx tables
   * to accomodate larger number of transactions
   * in the COMMITTING state.
   * 
   * <p>PRECONDITION: thread has activeTxLock shut 
   */
  private void growActiveTxArray()
  {
    // allocate new arrays
    XACommittingTx[] newActiveTx    = new XACommittingTx[activeTx.length + 50];
    XACommittingTx[] newAvailableTx = new XACommittingTx[newActiveTx.length];

    // initialize new elements
    for (int i = activeTx.length; i < newActiveTx.length; ++ i)
    {
      newActiveTx[i] = null;
      newAvailableTx[i] = new XACommittingTx(i);
    }
    
    // calling thread already holds the lock, so this is really not necessary
    synchronized(activeTxLock)
    {
      // copy existing entries to new tables
      for (int i = 0; i < activeTx.length; ++ i)
      {
        newActiveTx[i] = activeTx[i];
        newAvailableTx[i] = availableTx[i];
      }
      
      // update pointers
      atxPut = 0;
      atxGet = activeTx.length;

      // activate the new tables
      activeTx = newActiveTx;
      availableTx = newAvailableTx;
    }
    
    ++growActiveTxArrayCount;
  }
  
  /**
   * Common initialization for all constructors.
   */
  private void init()
  {
    // allocate a table of active transaction objects
    activeTx = new XACommittingTx[50];
    for (int i=0; i < activeTx.length; ++i)
      activeTx[i] = null;
    
    // allocate and initialize the table of indexes
    // for available entries in activeTx.
    // initially, all entries are available.
    availableTx = new XACommittingTx[activeTx.length];
    for (int i=0; i < activeTx.length; ++i)
      availableTx[i] = new XACommittingTx(i);
    
    // register the event listener
    super.setLogEventListener(this);
  }
  
  /**
   * Construct a Logger using default Configuration object.
   * @throws IOException
   */
  public XALogger()
    throws IOException
  {
    super(new Configuration());
    init();
  }
  
  /**
   * Construct a Logger using a Configuration supplied
   * by the caller.
   * @param config Configuration object
   * @throws IOException
   */
  public XALogger(Configuration config)
    throws IOException
  {
    super(config);
    init();
  }
  
  /**
   * Write a begin COMMIT record to the log.
   * <p>Call blocks until the data is forced to disk.
   * 
   * @param record byte[][] containing data to be logged
   * 
   * @return XACommittingTx object to be used whe putting the DONE record.
   * 
   * @throws IOException
   * @throws InterruptedException
   * @throws LogFileOverflowException
   * @throws LogRecordSizeException
   * @throws LogClosedException
   */
  public XACommittingTx putCommit(byte[][] record)
  throws LogClosedException, LogRecordSizeException, LogFileOverflowException, InterruptedException, IOException
  {
    XACommittingTx tx = null;
    long key = 0L;
    long overflowFence = 0L;
    
    // wait for overflow notification processor to finish.
    // failure to do might cause the overflow processor to get LogFileOverflowException
    long beginWait = System.currentTimeMillis();
    synchronized(this)
    {
      if (this.overflowFence != 0L)
        ++waitForThisCount;
      while (this.overflowFence != 0L)
        wait();
      totalWaitForThis += (System.currentTimeMillis() - beginWait);
    }
    
    /*
     * The following loop handles the (hopefully very rare) case 
     * where the call to put() returns a key that is lower than
     * the current overflowFence.  This should not happen, but
     * just in case we have a situation in which a thread is not
     * dispatched for a very long time after the record is 
     * written to the log file, it is theoretically possible that
     * the log key returned by put() could be lower than the
     * overflowFence.  If this happens, we just rewrite the
     * record.
     * 
     * Note: A very obnoxious test case using extremely small files
     * might actually put this into an infinate loop. 
     */
    do
    {
      key = put(record, true);
      synchronized(this) { overflowFence = this.overflowFence; }
    } while (key < overflowFence);
    
    
    synchronized(activeTxLock)
    {
      // resize activeTx[] if necessary
      if (atxUsed == activeTx.length)
        growActiveTxArray();
      
      // get an available entry index and save reference
      tx = availableTx[atxGet];
      assert tx != null : "availableTx[" + atxGet +"] is null";
      availableTx[atxGet] = null;
      atxGet = (atxGet + 1) % activeTx.length;

      /*
       * update XACommittingTx with values for this COMMIT
       * 
       * DEBUG Note: to test tx for null, we could put a
       * breakpoint on any of the following statements and
       * set a condition for tx == null.  However, this uses
       * a lot of CPU time and makes the debug session run
       * very slow.  It seems to be faster to wrap the code
       * it a try/catch and set a breakpoint in the catch.
       */
      try {
        tx.setLogKey(key);
        tx.setRecord(record);
        tx.setDone(false);
        tx.setMoving(false);
      } catch (NullPointerException npe) {
        throw npe;
      }

      int index = tx.getIndex();
      activeTx[index] = tx;

      // maintain statistics
      ++atxUsed;
      maxAtxUsed = Math.max(atxUsed, maxAtxUsed);
    }
    
    return tx;
  }
  
  
  /**
   * Write a DONE record to the log.
   * <p>Remove XACommittingTx object from the list of
   * active transactions.
   * 
   * @param record byte[][] containing data to be logged
   * @param tx the XACommittingTx that was returned
   * by the putCommit() routine for this transaction.
   * 
   * @throws IOException
   * @throws InterruptedException
   * @throws LogFileOverflowException
   * @throws LogRecordSizeException
   * @throws LogClosedException
   * 
   */
  public long putDone(byte[][] record, XACommittingTx tx)
  throws LogClosedException, LogRecordSizeException, LogFileOverflowException, 
    InterruptedException, IOException
  {
    synchronized(activeTxLock)
    {
      // clear this tx from the table
      int index = tx.getIndex();
      
      // validate the entry
      if (activeTx[index] != tx) throw new IllegalArgumentException();
      
      // remove this entry from the activeTx table so overflow processor does not see it
      activeTx[index] = null;
    }
    
    // mark entry as DONE and wait (if necessary) for move to complete
    synchronized(tx)
    {
      // let logOverflowNotification know that this entry does not have to be moved
      tx.setDone(true);
      
      // in case logOverflowNotification got into the object first
      while (tx.isMoving())
        tx.wait();
    }
    
    // write the DONE record
    long doneKey = 0L;
    do {
      try {
        doneKey = put(record, false);
      } catch (LogFileOverflowException e) {
        Thread.sleep(10);  
      }
    } while (doneKey == 0L);


    // make entry available for re-use.
    synchronized(activeTxLock)
    {
      availableTx[atxPut] = tx;
      atxPut = (atxPut + 1) % activeTx.length;

      --atxUsed;
      assert atxUsed >= 0 : "Negative atxUsed (" + atxUsed + ")";
    }
    
    return doneKey;
  }
  
  /**
   * called by Logger when log file is about to overflow.
   * 
   * <p>copies XACommittingTx records forward to allow reuse
   * of older log file space.
   * 
   * <p>calls Logger.mark() to set the new active mark
   * at the completion of the scan.
   * 
   * <p>Exceptions are ignored. Hopefully they will be
   * reported to the TM when a transaction thread attempts
   * to write a log record.
   *
   * <p>While we are processing activeTx[] we publish
   * the overflowFence so putCommit knows that any record
   * it stores with a key below the fence must be re-put.
   *  
   * @param overflowFence COMMIT records with
   * log keys lower than <i> fence </i> must
   * be moved.  All others can be ignored
   * for now.
   */
  public void logOverflowNotification(long overflowFence)
  {
    long newMark = Long.MAX_VALUE;
    XACommittingTx tx = null;
    long txKey = 0L;
    
    if (overflowFence == 0) throw new IllegalArgumentException("overflowFence == 0");
    
    // increment number of times we are notified
    ++overflowNotificationCount;
    
    synchronized(this)
    {
      // putCommit will re-put a record if it is below the fence
      this.overflowFence = overflowFence;
    }
    
    for (int i=0; i < activeTx.length; ++i)
    {
      synchronized(activeTxLock)
      {
        tx = activeTx[i];
        if (tx == null) continue;
      }

      synchronized(tx)
      {
        // gaurd against the possibility that putDone got into the object first
        if (tx.isDone()) continue;
        
        txKey = tx.getLogKey();
        if (txKey > overflowFence)
        {
          // we are not moving this entry, but it might be the new active mark
          if (txKey < newMark)
            newMark = txKey;  // remember the oldest active transaction
          continue;
        }

        // make putDone wait till we get this record moved
        tx.setMoving(true);
      }
      

      try {
        // move the COMMIT record data
        // we force all the moved records later when we set the new mark.
        txKey = put(tx.getRecord(), false);
        
        // keep track of the number of records moved.
        ++movedRecordCount;
        
        synchronized(tx)
        {
          tx.setLogKey(txKey);
          tx.setMoving(false);
          tx.notifyAll(); // in case putDone is waiting
        }
      } catch (LogClosedException e1) { // ignore
        assert false : "unexpected LogClosedException";
      } catch (LogRecordSizeException e1)  { // ignore
        assert false : "unexpected LogRecordSizeException";
      } catch (LogFileOverflowException e1)  { // ignore
        assert false : "unexpected LogFileOverflowException";
      } catch (InterruptedException e1)  { // ignore
      } catch (IOException e1)  { // ignore
      }
    }
    
    // set new mark using oldest log key encountered during scan of activeTx[]
    try {
      // if we have not moved any records, set new mark at the beginning of next file.
      if (newMark == Long.MAX_VALUE)
        newMark = overflowFence;
      mark(newMark, true);  // force = true
    } catch (InvalidLogKeyException e) { // should never happen
      System.err.println(e.toString());
      Thread.yield();
    } catch (LogClosedException e) { // should never happen
      assert false : "Log closed during logOverflowNotification processing";
    } catch (IOException e) {
      // ignore here - it will get caught by a transaction thread
    } catch (InterruptedException e) {
      // ignore here - it will get caught by a transaction thread
    }

    // let putCommit know that overflow processing is idle
    synchronized(this)
    {
      this.overflowFence = 0L;
      notifyAll();
    }
  }
  
  /**
   * return an XML node containing statistics for
   * this object along with the base Logger,
   * the LogFile pool and the LogBuffer pool.
   * 
   * @return String contiining XML document.
   */
  public String getStats()
  {
    String name = this.getClass().getName();
    StringBuffer stats = new StringBuffer(
        "\n<XALogger  class='" + name + "'>" 
    );
    
    stats.append(
        "\n<growActiveTxArrayCount value='" + growActiveTxArrayCount +
        "'>Number of times activeTx table was resized to accomodate " +
        "a larger number of transactions in COMMITTING state" +
        "</growActiveTxArrayCount>" +
        
        "\n<maxAtxUsed value='" + maxAtxUsed +
        "'>Maximum number of active TX entries used" +
        "</maxAtxUsed>" +
        
        "\n<movedRecordCount value='" + movedRecordCount + 
        "'>Number of records moved during log overflow notification processing" +
        "</movedRecordCount>" +
        
        "\n<overflowNotificationCount value='" + overflowNotificationCount + 
        "'>number of times log overflow notification event was called." +
        "</overflowNotificationCount>" +
        
        "\n<waitForThisCount value='" + waitForThisCount +
        "'>Number of times threads waited for overflow processing to complete" +
        "</waitForThisCount>" +
        
        "\n<totalWaitForThis value='" + totalWaitForThis +
        "'>Total time (ms) threads waited for overflow processing to complete" +
        "</totalWaitForThis>"
        );
    
    stats.append(super.getStats());
    
    stats.append("\n</XALogger>" +
        "\n");
    
    return stats.toString();
  }
}
