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


/**
 * Define record types used by Logger implementations.
 * 
 * <p>NOTE: record types used by all sub-classes and
 * packages within org.objectweb.howl should be defined
 * here so it is easy to determine that new types
 * are always unique and do not conflict with existing
 * types.
 * 
 * @author Michael Giroux
 */
public interface LogRecordType
{
  /**
   * Log records generated by user.
   */
  static final short USER   = 0;
  
  /**
   * Log records generated by Logger.
   * <p>must be ORed with another value to form a
   * complete control record type.
   * <p>The first nibble contains only the CTRL flag
   * to make it easy to see in file dumps.
   * The remaining three nibbles contain bits that
   * identify the specific type of control record.
   */
  static final short CTRL   = 0x4000;
  
  /**
   * Header records generated at start of every log file.
   */
  static final short FILE_HEADER = CTRL | 0x0800;
  
  /**
   * Log records containing mark data.
   * 
   * <p>Data portion of the record contains the
   * current automark mode (true or false) and
   * the active mark.
   */
  static final short MARKKEY  = CTRL | 0x0400;
  
  /**
   * recorded by Logger to signal a clean close on the log.
   */
  static final short CLOSE = CTRL | 0x0200;
  
  /**
   * recorded by Logger to mark first block following a 
   * restart of the Logger.
   */
  static final short RESTART = CTRL | 0x0100;
  
  /**
   * recorded by XALogger to mark records
   * generated by XALogger#putCommit()
   */
  static final short XACOMMIT = CTRL | 0x0080;
  
  /**
   * recorded by XALogger to mark records
   * generated by XALogger#putDone()
   */
  static final short XADONE = CTRL | 0x0040;
  
  /**
   * recorded by XALogger *after* a XACOMMIT record
   * is moved to allow replay to remove the original
   * XACOMMIT record from the activeTx table. 
   */
  static final short XACOMMITMOVED = CTRL | XACOMMIT | XADONE;
  
  /**
   * Type returned by get() methods to signal end of buffer.
   * 
   * <p>This record type may or may not actually exist within
   * a data buffer.  A get() method should return this
   * record type when the ByteBuffer.position() is at or beyond
   * the used bytes for the buffer.
   */
  static final short EOB = CTRL | 0xE0B;
  
  /**
   * Type indicating that end of log has been reached.
   * 
   * <p>signals ReplayListener that no more records will
   * be delivered.
   */
  static final short END_OF_LOG = CTRL | 0xE0F;
  
}
