/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2005 Bull S.A.
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
package org.objectweb.howl.log.util;

import java.io.File;
import java.io.IOException;

import org.objectweb.howl.log.Configuration;
import org.objectweb.howl.log.LogConfigurationException;
import org.objectweb.howl.log.Logger;

/**
 * Dump a log file.

 * @author girouxm
 *
 */
public class DumpLog {
  private final Configuration cfg;
  private final Logger log;
  
  private DumpLog() {
    Configuration cfg = null;
    Logger log = null;
    String propertyFileName = System.getProperty("cfg");
    try {
      if (propertyFileName == null) {
        System.out.println("DumpLog using default configuration parameters.");
        cfg = new Configuration();
      }
      else
        cfg = new Configuration(new File(propertyFileName));
      log = new Logger(cfg);
    } catch (LogConfigurationException e) {
      System.err.println(e.toString());
      System.exit(1);
    } catch (IOException e) {
      System.err.println("DumpLog: error creating Logger object");
      e.printStackTrace();
      System.exit(1);
    } finally {
      this.cfg = cfg;
      this.log = log;
    }
      
  }
  
  /**
   * Dump the log file.
   */
  private void run() {
    System.out.println("--- end of dump ---");
  }

  /**
   * Command Line processor.
   * <p>
   * Syntax: java -Dcfg=<log config properties> org.objectweb.howl.log.util.DumpLog
   * @param args
   */
  public static void main(String[] args) {
    new DumpLog().run();
  }

}
