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
 * $Id: ConfigurationMBean.java,v 1.3 2005-11-15 22:43:54 girouxm Exp $
 * ------------------------------------------------------------------------------
 */
package org.objectweb.howl.log;

/**
 * @author Michael Giroux
 */
public interface ConfigurationMBean {
  /**
   * @return Returns the logDir.
   */
  public String getLogFileDir();
  /**
   * @return Returns the logFileExt.
   */
  public String getLogFileExt();
  /**
   * @return Returns the logFileName.
   */
  public String getLogFileName();
  /**
   * @return the adler32Checksum option.
   */
  public boolean isAdler32ChecksumEnabled();
  /**
   * @return Returns the checksumEnabled option.
   */
  public boolean isChecksumEnabled();
  /**
   * @return Returns the bufferSize.
   */
  public int getBufferSize();
  /**
   * @return Returns the bufferClassName.
   */
  public String getBufferClassName();
  /**
   * @return Returns the maxBuffers.
   */
  public int getMaxBuffers();
  /**
   * @return Returns the minBuffers.
   */
  public int getMinBuffers();
  /**
   * @return Returns the flushSleepTime.
   */
  public int getFlushSleepTime();
  /**
   * @return Returns the threadsWaitingForceThreshold.
   */
  public int getThreadsWaitingForceThreshold();
  /**
   * @return Returns the maxBlocksPerFile.
   */
  public int getMaxBlocksPerFile();
  /**
   * @return Returns the maxLogFiles.
   */
  public int getMaxLogFiles();
  /**
   * @return Returns the logFileMode.
   */
  public String getLogFileMode();
}