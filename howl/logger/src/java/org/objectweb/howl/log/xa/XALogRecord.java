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

import org.objectweb.howl.log.LogRecord;
import org.objectweb.howl.log.LogRecordType;

/**
 * Extends {@link org.objectweb.howl.log.LogRecord} with
 * members that are specific to XALogger.
 * 
 * @author Michael Giroux
 */
public class XALogRecord extends LogRecord {
  private XACommittingTx tx = null;
  

  /**
   * constructs an instance of XALogRecord with a byte[]
   * of <i> size </i> data.
   * 
   * @param size initial size of data buffer.
   * <p>the get() method will reallocate the data buffer
   * to accomdate larger records.
   */
  public XALogRecord(int size)
  {
    super(size);
  }
  
  /**
   * @return Returns the XACommittingTx of this XALogRecord.
   */
  public XACommittingTx getTx() {
    return tx;
  }
  /**
   * Called by XALogger ReplayListener to save the
   * XACommitingTx entry associated with an XACOMMIT record.
   * 
   * @param tx The XACommittingTx to set.
   */
  public void setTx(XACommittingTx tx) {
    this.tx = tx;
  }
  
  /**
   * @return true if the current record is an
   * XACOMMIT type.
   */
  public boolean isCommit()
  {
    return (this.type == LogRecordType.XACOMMIT ||
            this.type == LogRecordType.XACOMMITMOVED);
  }
}
