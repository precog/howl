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
package org.objectweb.howl.test;

/**
 * Partial implementation of Doug Lea's CyclicBarier used to establish
 * test environments with pre-determined number of threads in execution.
 * 
 * @author Michael Giroux
 */
public class Barrier {
  /**
   * number of threads that need to enter barrier() before
   * releasing all of them.
   */
  protected int count; // number of parties still waiting
  
  public Barrier(int count)
  {
    if (count <= 0) throw new IllegalArgumentException();
    this.count = count;
  }
  
  /**
   * wait for <var>count</var> to become zero. 
   */
  public synchronized void barrier()
  {
    // let test driver know when it is the last waiting thread
    if (--count <= 1)
    {
      notifyAll();
    }
    
    while(count > 0)
    {
      try {
        wait();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt(); // propagate
      }
    }
  }
  
  /**
   * @return number of threads that have not hit the barrier yet.
   * <p>Used by test drivers to determine when count == 1
   * so the test driver itself can call barrier() to set
   * all worker threads into execution.
   */
  public synchronized int getCount()
  {
    return count;
  }
  
  /**
   * decrement count and notify other threads.
   * <p>Used by worker threads to signal the
   * test driver that the work thread has terminated.
   * The test driver should be sitting in barrier() waiting
   * for the count to go to zero.
   */
  public synchronized void release()
  {
    --count;
    notifyAll();
  }

}
