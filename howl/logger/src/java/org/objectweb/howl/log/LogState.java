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
 * $Id: LogState.java,v 1.3 2005-06-23 23:28:15 girouxm Exp $
 * ------------------------------------------------------------------------------
 */
package org.objectweb.howl.log;

import java.io.File;
import java.util.Properties;


/**
 * Manage state variables for the Logger instance.
 * 
 * <p>state variables are saved when logger is closed.
 * 
 * <p>when a logger is open, a lock file is created
 * to record the fact that a logger is active.  Whe the
 * logger is closed, the lock file is deleted.  If the
 * system or JVM crashes before the logger is closed, 
 * the lock file will serve to notify future users of
 * the log that recovery may be necessary.  
 * 
 * @author Michael Giroux
 *
 */
class LogState
{
  Properties state = null;
  
  File lockFile = new File(".lock");
  File stateFile = new File(".state");
  
  /**
   * Construct LogState instance.
   */
  LogState()
  {
  }
  
  /**
   * loads state variables from property file.
   */
  void load()
  {
    
  }
  
  /**
   * saves state variables to property file.
   */
  void save()
  {
    
  }
  
  /**
   * make state unavailable whil log is open.
   * 
   * <p>Locking the state is accomplished by 
   * creating a ".lock" file.  Presence of the
   * lock file indicates that an instance of
   * the logger is currently open, or that a
   * previous instance failed to close properly,
   * perhaps due to system or JVM failure.
   */
  void lock()
  {

  }
  
}
