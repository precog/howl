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
 * $Id: LogBufferStatus.java,v 1.4 2005-11-17 21:00:50 girouxm Exp $
 * ------------------------------------------------------------------------------
 */
package org.objectweb.howl.log;


/**
 * Defines valid values for LogBuffer status.
 */
interface LogBufferStatus
{
  /**
   * IO Status of LogBuffer while it is being filled with
   * new records (0).
   */
  final static int FILLING  = 0;
  
  /**
   * IO Status of LogBuffer while it is being forced (1).
   */
  final static int WRITING  = 1;
  
  /**
   * IO Status of LogBuffer when force is complete (2).
   * <p>Threads waiting for a force may be notified
   * when status is COMPLETE.
   */
  final static int COMPLETE = 2;
  
  /**
   * IO Status of LogBuffer if an IOException occurs
   * during the force (3).
   * <p>Threads waiting for a force must receive
   * an IOException if an error occurs during force.
   */
  final static int ERROR    = 3;

}