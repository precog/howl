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
 * 
 * ------------------------------------------------------------------------------
 * $Id$
 * ------------------------------------------------------------------------------
 */
package org.objectweb.howl.log;

import junit.framework.Assert;

public final class DataRecords { // make class final to clear findbugs warnings
  protected Logger log = null;

  final int count;

  final String[] sVal;

  final byte[][] r1;

  final long[] key;

  final LogRecord lr;

  DataRecords(Logger log, int count) {
    this.log = log;
    this.count = count;
    sVal = new String[count];
    r1 = new byte[count][];
    key = new long[count];

    // initialize test records
    for (int i = 0; i < count; ++i) {
      sVal[i] = "Record_" + (i + 1);
      r1[i] = sVal[i].getBytes();
    }
    int last = count - 1;
    lr = new LogRecord(sVal[last].length() + 6);
  }

  void putAll(boolean forceLastRecord) throws Exception {
    // populate journal with test records
    for (int i = 0; i < count; ++i) {
      boolean force = (i == sVal.length - 1) ? forceLastRecord : false;
      Assert.assertNotNull("NULL Logger", log);
      key[i] = log.put(r1[i], force);
    }
  }

  LogRecord verify(int index) throws Exception {
    log.get(lr, key[index]);
    verifyLogRecord(lr, sVal[index], key[index]);
    return lr;
  }

  LogRecord verify(int index, LogRecord lr) throws Exception {
    verifyLogRecord(lr, sVal[index], key[index]);
    return lr;
  }
  
  /**
   * Verifies the content of the LogRecord is correct.
   * 
   * @param lr
   *          LogRecord to be verified
   * @param eVal
   *          expected value
   * @param eKey
   *          expected record key
   */
  void verifyLogRecord(LogRecord lr, String eVal, long eKey) {
    byte[][] r2 = lr.getFields();
    String rVal = new String(r2[0]);
    Assert.assertEquals("Record Type: " + Long.toHexString(lr.type), 0, lr.type);
    Assert.assertEquals("Record Key: " + Long.toHexString(eKey), eKey, lr.key);
    Assert.assertEquals("Record Data", eVal, rVal);
    Assert.assertEquals("Field Count != 1", 1, r2.length);
  }
}
