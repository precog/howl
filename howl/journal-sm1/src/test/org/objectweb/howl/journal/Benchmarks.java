/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Geronimo" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Geronimo", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * ====================================================================
 */
package org.objectweb.howl.journal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import junit.framework.TestCase;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

/**
 * Micro Benchmarks to help show/expain bottlenecks that may show up
 * in the Journal implementation.
 * 
 * @version $Revision: 1.1 $ $Date: 2004-01-26 20:59:30 $
 */
public class Benchmarks extends TestCase {

    static final File jorunalFile = new File("test-journal.log");
    int WORKERS = 1;
    int MESSAGE_COUNT = 16000;
    int MESSAGE_SIZE = 1024 * 4;
    static final byte data[] = new byte[1024 * 4];
    
    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {

        WORKERS = 1;
        MESSAGE_COUNT = 16000;
        MESSAGE_SIZE = 1024 * 4;
        
        // Delete any old journal files..
        File[] files = jorunalFile.getCanonicalFile().getParentFile().listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.getName().startsWith(jorunalFile.getName());
            }
        });
        for (int i = 0; i < files.length; i++)
            files[i].delete();        
    }

    private void printSpeedReport(long startTime, long stopTime) {
        long mc = ((long)MESSAGE_COUNT)*WORKERS;
        long t = mc * MESSAGE_SIZE;
        float kb = (((float)t)/(1024));
        long duration = (stopTime - startTime);        
        System.out.println(" wrote: "+mc+" messages in " + duration +" ms. ("+(mc*1000/(duration))+" m/s)");
        System.out.println(" wrote: "+kb+" kb in " + duration +" ms. ("+(kb*1000/(duration))+" kb/s)");
    }
    
    /**
     * Logs out 4k records and syncs out the file to disk every 50 ms.
     * Does not buffer the output stream.
     * 
     * Results on a 1.3 ghz Intel:
     *   syncs:8
     *   wrote: 64000.0 kb in 4226 ms. (15144.345 kb/s)
     * 
     * @throws IOException
     */
    public void testSyncedAppend() throws IOException {
        FileOutputStream out = new FileOutputStream("test.log");
        FileChannel channel = out.getChannel();
        
        long lastSyncTime= System.currentTimeMillis();
        long syncCount=0;
        long startTime = System.currentTimeMillis(); 
        for( int i =0;  i < MESSAGE_COUNT; i++) {
            out.write(data);
            if( (System.currentTimeMillis()-lastSyncTime) > 50 ) { 
                channel.force(false);
                syncCount++;
                lastSyncTime = System.currentTimeMillis();
            } 
        }
        channel.force(false);
        long stopTime = System.currentTimeMillis();
        out.close();

        System.out.println("testSyncedAppend:");
        System.out.println(" syncs:"+syncCount);
        printSpeedReport(startTime, stopTime);
    }

    /**
     * Logs out 4k records and syncs out the file to disk on every record.
     * Does not buffer the output stream.  This is REALLY slow.
     * 
     * Results on a 1.3 ghz Intel:
     *   wrote: 640.0 kb in 7641 ms. (83.75867 kb/s)
     * 
     * @throws IOException
     */
    public void testSlowSyncedAppend() throws IOException {
        MESSAGE_COUNT = 160;
        FileOutputStream out = new FileOutputStream("test.log");
        FileChannel channel = out.getChannel();
        
        long startTime = System.currentTimeMillis(); 
        for( int i =0;  i < MESSAGE_COUNT; i++) {
            out.write(data);
            channel.force(false);
        }
        channel.force(false);
        long stopTime = System.currentTimeMillis();
        out.close();

        System.out.println("testSlowSyncedAppend:");
        printSpeedReport(startTime, stopTime);
    }

    /**
     * Uses a BufferedOutputStream to write to the file.  This test
     * is useful to compare against the testStandardAppend results which 
     * does not use a buffer.  Only one sync is done once the file is
     * writen.
     * 
     * This is faster the unbuffered but not significantly.
     * 
     * Results on a 1.3 ghz Intel:
     *    wrote: 64000.0 kb in 4587 ms. (13952.475 kb/s)
     * @throws IOException
     */
    public void testBufferedAppend() throws IOException {
        FileOutputStream out = new FileOutputStream("test.log");
        BufferedOutputStream os = new BufferedOutputStream( out );
        
        long startTime = System.currentTimeMillis(); 
        for( int i =0;  i < MESSAGE_COUNT; i++) {
            os.write(data);
        }
        os.flush();
        out.getChannel().force(false);
        long stopTime = System.currentTimeMillis();
        out.close();

        System.out.println("testBufferedAppend:");
        printSpeedReport(startTime, stopTime);
    }

    /**
     * Uses a FileOutputStream to write to the file.  This test
     * is useful to compare against the testBufferedAppend results which 
     * uses a buffer.  Only one sync is done once the file is
     * writen.
     * 
     * Results on a 1.3 ghz Intel:
     *    wrote: 64000.0 kb in 4937 ms. (12963.338 kb/s)
     * @throws IOException
     */
    public void testUnbufferedAppend() throws IOException {
        FileOutputStream out = new FileOutputStream("test.log");
                
        long startTime = System.currentTimeMillis(); 
        for( int i =0;  i < MESSAGE_COUNT; i++) {
            out.write(data);
        }
        out.getChannel().force(false);
        long stopTime = System.currentTimeMillis();
        out.close();
        
        System.out.println("testUnbufferedAppend:");
        printSpeedReport(startTime, stopTime);        
    }
    
    /**
     * Used to test the performance of the EDU.oswego.cs.dl.util.concurrent.LinkedQueue
     * implementation.  Since we will be using it hand off events from the producing threads
     * to the hardening thread we need to make sure it will become a bottleneck (consumer starvation).
     * 
     * Since we can log at 15144.345 kb/s you can sync 727k in a cycle (50 ms) per testSyncedAppend.  
     * That means we can log 7753 events/sync if the events are small about 100 bytes:
     * 
     *  15144.345 kb/s  * ( 1s / 20 sync ) *  (1024 b / 1kb )  * ( 1 event / 100 b ) =  7753 event/sync
     *  
     * Since the linkedQueue and deliver 298062 event/s or 14903 event/sync then the linkedQueue is about
     * allmost 2 times as faster than the events can be written to disk.  
     * 
     *  Results on a 1.3 ghz Intel:
     *   delivered: 200000 events in 671 ms. (298062.59314456035 event/s)
     */
    public void testLinkedQueueSpeed() throws Exception {
        final LinkedQueue eventQueue = new LinkedQueue();
        
        final int WORKERS = 10;
        final int MESSAGE_COUNT=20000;
            
        final CyclicBarrier startBarrier = new CyclicBarrier(WORKERS+1); 
        final CyclicBarrier stopBarrier = new CyclicBarrier(WORKERS);
        
        for( int i=0; i < WORKERS; i++ ) {                
            new Thread() {
                public void run() {
                    try {
                        startBarrier.barrier();
                        for(int i=0; i < MESSAGE_COUNT; i++ ) {         
                            eventQueue.put(this);
                        }                        
                        stopBarrier.barrier();                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        
        long t = MESSAGE_COUNT*WORKERS;
        long startTime = System.currentTimeMillis();
        startBarrier.barrier();
        for( int i=0; i < t; i++) {
            assertNotNull(eventQueue.poll(500));
        }        
        long stopTime = System.currentTimeMillis();        
        long duration = (stopTime - startTime);
        
        System.out.println("testLinkedQueueSpeed:");
        System.out.println(" delivered: "+t+" events in " + duration +" ms. ("+(1000.0*t/(duration))+" event/s)");
    }
    

    /**
     * Tests to see what kind of throughput we can get with the current Journal.
     * 
     * @throws Exception
     */
    public void testHighConncurencySmallMsgSyncedThroughput() throws Exception {
        final Journal journal = new Journal(jorunalFile);
        journal.start();

        WORKERS = 500;
        MESSAGE_COUNT = 50;
        MESSAGE_SIZE = 100;        
        int MESSAGE_SYNC_COUNT = 10;

        journalTest(journal, MESSAGE_SYNC_COUNT, "testHighConncurencySmallMsgSyncedThroughput");        
        
        journal.stop();
        journal.close();
    }

    /**
     * Tests to see what kind of throughput we can get with the current Journal.
     * 
     * @throws Exception
     */
    public void testHighConncurencySmallMsgUnSyncedThroughput() throws Exception {
        final Journal journal = new Journal(jorunalFile);
        journal.start();

        WORKERS = 500;
        MESSAGE_COUNT = 50;
        MESSAGE_SIZE = 100;        
        int MESSAGE_SYNC_COUNT = 50;

        journalTest(journal, MESSAGE_SYNC_COUNT, "testHighConncurencySmallMsgUnSyncedThroughput");        
        
        journal.stop();
        journal.close();
    }

    
    private void journalTest(final Journal journal, final int MESSAGE_SYNC_COUNT, String testName) throws InterruptedException {
        final CyclicBarrier startBarrier = new CyclicBarrier(WORKERS + 1);
        final CyclicBarrier stopBarrier = new CyclicBarrier(WORKERS + 1);

        for (int i = 0; i < WORKERS; i++) {
            new Thread() {
                public void run() {
                    try {

                        byte[] data = new byte[MESSAGE_SIZE];
                        for (int i = 0; i < MESSAGE_SIZE; i++)
                            data[i] = (byte) i;

                        // Wait for all the workers to be ready..
                        startBarrier.barrier();
                        
                        for (int i = 0; i < MESSAGE_COUNT; i++) {
                            if ((i % MESSAGE_SYNC_COUNT) == 0)
                                journal.addAndSync(data);
                            else
                                journal.add(data);
                        }
                        
                        // Wait for al the workers to finish.
                        stopBarrier.barrier();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            .start();
        }

        // Wait for all the workers to be ready..
        startBarrier.barrier();
        long startTime = System.currentTimeMillis();

        // Wait for al the workers to finish.
        stopBarrier.barrier();
        long stopTime = System.currentTimeMillis();

        System.out.println(testName+":");
        printSpeedReport(startTime, stopTime);
    }
    
}
