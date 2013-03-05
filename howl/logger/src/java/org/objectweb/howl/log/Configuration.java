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
 * $Id: Configuration.java,v 1.15 2006-12-05 13:51:54 girouxm Exp $
 * ------------------------------------------------------------------------------
 */
package org.objectweb.howl.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Provides configuration information for a
 * {@link org.objectweb.howl.log.Logger Logger}
 * instance.
 * @author girouxm
 */
public class Configuration implements ConfigurationMBean {

  /**
   * Construct a Configuration object with default values.
   * <p>Caller will use setter methods to change the defaults.
   */
  public Configuration()
  {
    this.prop = new Properties();

    // populate this.prop with default values
    try {
      parseProperties();
    } catch (LogConfigurationException e) {
      // will not happen
    }
  }

  /**
   * Construct a Configuration object using a Properties
   * object supplied by the caller.
   * @param prop Properties object containing default settings
   */
  public Configuration(Properties prop) throws LogConfigurationException
  {
    this.prop = new Properties(prop); // so we do not modify callers prop object

    // apply settings from caller supplied Properties parameter
    parseProperties();
  }

  /**
   * Construct a Configuration object using a Properties
   * file specified by the caller.
   * @param propertyFile File object describing a properties file
   * @throws LogConfigurationException
   * if property file cannot be processed.
   */
  public Configuration(File propertyFile) throws LogConfigurationException
  {
    // perform default construction
    this();

    // TODO: if File has xml extension then parse as xml

    try {
      prop.load(new FileInputStream(propertyFile));
      parseProperties();
    } catch (FileNotFoundException e)
    {
      LogConfigurationException lce = new LogConfigurationException(e.toString(), e);
      throw lce;
    } catch (IOException e)
    {
      throw new LogConfigurationException(e.toString(), e);
    }

  }

  /**
   * maximum size of a LogBuffer (number of K bytes).
   * <p>Good performance can be achieved with buffers between
   * 2K and 6K when using a reasonably fast disk.  Larger
   * sizes may help with slower disks, but large buffers
   * may be mostly empty in lightly loaded systems.
   *
   * MG 20060508 remove private qualifier so test case can
   * access the constant.
   */
  static final int MAX_BUFFER_SIZE = 32;

  /**
   * The Properties used to construct this object.
   */
  private Properties prop = null;

  /**
   * Display the value of an int configuration parameter to System.err
   * if <var> listConfig </var> is true.
   * <p>The <i> text </i> parameter allows the program to
   * provide additional text that will be displayed following the
   * value to explain the type of value.  For example, values that
   * represent Milliseconds might be displayed with "Ms".
   *
   * @param key name of the parameter being displaed
   * @param val value for the parameter
   * @param text additional text to be displayed such as "Kb" or "Ms".
   */
  private void showConfig(String key, int val, String text)
  {
    if (listConfig) System.err.println(key + ": " + val + " " + text);
  }

  /**
   * called by parseProperties to obtain an int configuration
   * property and optionally display the configured value.
   *
   * <p>save result in prop member.
   *
   * @param key name of the parameter to return
   * @param val default value if the parameter is not configured
   * @param text additional text to pass to showConfig(String, int, String)
   * @return int value of requested parameter
   * @see #showConfig(String, int, String)
   */
  private int getInteger(String key, int val, String text)
  {
    // BUG: 300738 - trim property value
    val = Integer.parseInt(prop.getProperty(key, Integer.toString(val)).trim());
    showConfig(key, val, text);

    prop.setProperty(key, Integer.toString(val));
    return val;
  }

  /**
   * called by parseProperties to obtain an int configuration
   * property and optionally display the configured value.
   * <p>This routine calls getInteger(String, ing, String) passing
   * a zero length string as the third parameter.
   *
   * <p>save result in prop member.
   *
   * @param key name of parameter to return
   * @param val default value if the parameter is not configured
   * @return int value of requested parameter
   * @see #getInteger(String, int, String)
   */
  private int getInteger(String key, int val)
  {
    return getInteger(key, val, "");
  }

  /**
   * called by parseProperties to obtain a boolean configuration
   * property and optionally display the configured value.
   *
   * <p>save result in prop member.
   *
   * @param key name of parameter to return
   * @param val default value if the parameter is not configured
   * @return boolean value of the requested parameter
   * @throws LogConfigurationException
   * if the configured value of the property is something other than
   * 'true' or 'false'
   */
  private boolean getBoolean(String key, boolean val)
  throws LogConfigurationException
  {
    // BUG: 300738 - trim property value
    String pval = prop.getProperty(key, Boolean.toString(val)).toLowerCase().trim();
    if (!pval.equals("true") && !pval.equals("false"))
      throw new LogConfigurationException(key + "[" + pval +
          "] must be true of false");

    val = Boolean.valueOf(pval).booleanValue();
    if (listConfig) System.err.println(key + ": " + val);

    prop.setProperty(key, Boolean.toString(val));
    return val;
  }

  /**
   * called by parseProperties to obtain a String configuration
   * property and optionally display the configured value.
   *
   * <p>save result in prop member.
   *
   * @param key name of parameter to return
   * @param val default value if the parameter is not configured
   * @return String value of the requested parameter
   */
  private String getString(String key, String val)
  {
    val = prop.getProperty(key, val).trim();
    if (listConfig) System.err.println(key + ": " + val);

    prop.setProperty(key,val);
    return val;
  }

  /**
   * initialize member variables from property file.
   *
   * <p>entire property set is saved in prop member
   * for use in store(OutputStream) method.
   *
   * @throws LogConfigurationException
   * with text explaining the reason for the exception.
   */
  private void parseProperties() throws LogConfigurationException
  {
    listConfig = getBoolean("listConfig", listConfig);

    bufferClassName = getString("bufferClassName", bufferClassName);

    setBufferSize(getInteger("bufferSize", (bufferSize / 1024), "Kb")); // BUG 300791

    adler32Checksum = getBoolean("adler32Checksum", adler32Checksum);

    checksumEnabled = getBoolean("checksumEnabled", checksumEnabled);

    flushPartialBuffers = getBoolean("flushPartialBuffers", flushPartialBuffers);

    flushSleepTime = getInteger("flushSleepTime", flushSleepTime);

    logFileDir = getString("logFileDir", logFileDir);

    logFileExt = getString("logFileExt", logFileExt);

    setLogFileMode(getString("logFileMode", logFileMode)); // BUG 300791

    logFileName = getString("logFileName", logFileName);

    maxBlocksPerFile = getInteger("maxBlocksPerFile", maxBlocksPerFile);

    setMinBuffers(getInteger("minBuffers", minBuffers)); // BUG 300791

    setMaxBuffers(getInteger("maxBuffers", maxBuffers)); // BUG 300791

    maxLogFiles = getInteger("maxLogFiles", maxLogFiles);

    threadsWaitingForceThreshold = getInteger("threadsWaitingForceThreshold", threadsWaitingForceThreshold);
  }

  /* ---------------------------------------------------
   * LogBufferManager configuration settings
   * ---------------------------------------------------
   */

  /**
   * When set to <b> true </b> the configuration properties
   * are displayed to System.out following construction
   * of a Configuration object.
   * <p>Default is false --> config is not displayed
   */
  private boolean listConfig = false;

  /**
   * When set to <b> true </b> and
   * checksumEnabled is also <b> true </b>
   * checksums are computed
   * using java.util.zip.Adler32.
   */
  private boolean adler32Checksum = false;

  /**
   * When set to <b> true </b> checksums are computed on the contents
   * of each buffer prior to writing buffer contents to disk.
   *
   * <p>The checksum is used when blocks of data are retrieved
   * from the log during replay to validate the content of the
   * file.
   * <p>Default value is true.
   * <p>Setting this option to <b> false </b> may reduce
   * slightly the amount of cpu time that is used by the
   * logger.
   */
  private boolean checksumEnabled = false;

  /**
   * Size (in K bytes) of buffers used to write log blocks.
   *
   * <p>Specify values between 1 and 32 to allocate buffers
   * between 1K and 32K in size.
   *
   * <p>The default size of 4K bytes should be suitable
   * for most applications.
   * <p>Larger buffers may provide improved performance
   * for applications with transaction rates that
   * exceed 5K TX/Sec and a large number of threads.
   */
  private int bufferSize = 4 * 1024;

  /**
   * Name of class that implements LogBuffer used by
   * LogBufferManager.
   * <p>Class must extend LogBuffer.
   */
  private String bufferClassName = "org.objectweb.howl.log.BlockLogBuffer";

  /**
   * maximum number of buffers to be allocated by LogBufferManager.
   * <p>Default value is 0 (zero) -- no limit.
   */
  private int maxBuffers = 0;

  /**
   * minimum number of buffers to be allocated by LogBufferManager.
   * <p>Default value is 4.
   */
  private int minBuffers = 4;

  /**
   * The amount of time
   * (specified in number of milli-seconds)
   * the ForceManager sleeps between log forces.
   *
   * <p>During periods of low activity, threads could
   * wait an excessive amount of time
   * (possibly for ever) for buffers to fill and be
   * flushed.  To mitigate this situation, the
   * Logger runs a ForceManager thread that wakes
   * up periodically and forces IO when other
   * threads are waiting.
   *
   * <p>The default value is 50 milli-seconds.
   */
  private int flushSleepTime = 50;

  /**
   * Indicates whether LogBufferManager should flush buffers
   * before they are full.
   *
   * <p>Normally, buffers are flushed to disk only when
   * they become full.  In lightly loaded situations,
   * one or more threads may have to wait until the
   * flushSleepTime expires before the buffer is written.
   * In the worst case, a single thread is using the
   * log, and every put() with sync requested will
   * be delayed flushSleepTime ms before the buffer is
   * written.
   *
   * <p>Setting flushPartialBuffers true will allow
   * the LogBufferManager to flush buffers to disk
   * any time the channel is not busy.  This improves
   * throughput in single threaded and lightly loaded
   * environments.
   *
   * <p>By default, this feature is disabled (false) to
   * provide compatability with earlier versions of
   * this library.
   */
  private boolean flushPartialBuffers = false;

  /**
   * the maximum number of threads that should wait
   * for an IO force.
   * <p>Setting this value may have an effect
   * on latency when threads are waiting for
   * the force.
   *
   * <p>By default, there is no limit.
   */
  private int threadsWaitingForceThreshold = Integer.MAX_VALUE;

  /**
   * The scheduler for internal tasks.
   */
  private ScheduledExecutorService scheduler = null;

  /* ---------------------------------------------------
   * LogFileManager configuration settings
   * ---------------------------------------------------
   */

  /**
   * maximum number of blocks to store in each LogFile.
   *
   * <p>controls when logging is switched to a new log file, and/or when a circular
   * log is reset to  seek address zero.
   */
  private int maxBlocksPerFile = Integer.MAX_VALUE;

  /**
   * number of log files to configure.
   * <p>Default is 2 log files.
   */
  private int maxLogFiles = 2;

  /**
   * directory used to create log files.
   * <p>Default is logs directory relative to parent of current working dir.
   */
  private String logFileDir = "../logs";

  /**
   * file name extension for log files.
   * <p>Default value is "log"
   */
  private String logFileExt = "log";

  /**
   * filename used to create log files.
   * <p>Default value is "logger"
   * <p>file names are generated using the following pattern:
   * <pre>
   *   <value of logFileName> + "_" + <file number> + "." + <value of logFileExt>
   * </pre>
   */
  private String logFileName = "logger";

  /**
   * IO mode used to open the file.
   * <p>Default is "rw"
   * <p>Must be "rw" or "rwd"
   *
   * @see java.io.RandomAccessFile#RandomAccessFile(java.io.File, java.lang.String)
   */
  private String logFileMode = "rw";

  /**
   * @return Returns the logDir.
   */
  public String getLogFileDir() {
    return logFileDir;
  }
  /**
   * @param logFileDir The logFileDir to set.
   */
  public void setLogFileDir(String logFileDir) {
    this.logFileDir = logFileDir;
    prop.setProperty("logFiledir", logFileDir);
  }

  /**
   * @return Returns the logFileExt.
   */
  public String getLogFileExt() {
    return logFileExt;
  }
  /**
   * @param logFileExt The logFileExt to set.
   */
  public void setLogFileExt(String logFileExt) {
    this.logFileExt = logFileExt;
    prop.setProperty("logFileExt", logFileExt);
  }
  /**
   * @return Returns the logFileName.
   */
  public String getLogFileName() {
    return logFileName;
  }
  /**
   * @param logFileName The logFileName to set.
   */
  public void setLogFileName(String logFileName) {
    this.logFileName = logFileName;
    prop.setProperty("logFileName", logFileName);
  }

  /**
   * @return the adler32Checksum option.
   */
  public boolean isAdler32ChecksumEnabled() {
    return adler32Checksum;
  }

  /**
   * @return Returns the checksumEnabled option.
   */
  public boolean isChecksumEnabled() {
    return checksumEnabled;
  }

  /**
   * @param checksumOption The checksumOption to set.
   */
  public void setChecksumEnabled(boolean checksumOption) {
    this.checksumEnabled = checksumOption;
    prop.setProperty("checksumEnabled", Boolean.toString(checksumEnabled));
  }

  /**
   * Returns the size of buffers specified as a number of 1K blocks.
   * <p>As an example, if buffers are 4096 bytes large, getBufferSize()
   * returns 4.
   * @return Returns the bufferSize as a number of 1K blocks.
   */
  public int getBufferSize() {
    return bufferSize / 1024; // BUG 300957 return number of 1k blocks
  }
  /**
   * @param bufferSize The size of a log buffer
   * specified as a number of 1024 byte blocks.
   * <p>The value specified by bufferSize is
   * multiplied by 1024 to establish the actual
   * buffer size used by the logger.
   */
  public void setBufferSize(int bufferSize)
  throws LogConfigurationException
  {
    if (bufferSize < 1 || bufferSize > MAX_BUFFER_SIZE)
      throw new LogConfigurationException("bufferSize [" + bufferSize + "] must be" +
          " between 1 and "+ MAX_BUFFER_SIZE);

    this.bufferSize = bufferSize * 1024;
    prop.setProperty("bufferSize", Integer.toString(bufferSize));
  }

  /**
   * @return Returns the bufferClassName.
   */
  public String getBufferClassName() {
    return bufferClassName;
  }

  /**
   * @param adler32Checksum <b>true</b> if application
   * wishes to use java.util.zip.Adler32 checksum method.
   */
  public void setAdler32Checksum(boolean adler32Checksum)
  {
    this.adler32Checksum = adler32Checksum;
  }

  /**
   * @param bufferClassName The bufferClassName to set.
   */
  public void setBufferClassName(String bufferClassName) {
    this.bufferClassName = bufferClassName;
    prop.setProperty("bufferClassName", bufferClassName);
  }

  /**
   * @return Returns the maxBuffers.
   */
  public int getMaxBuffers() {
    return maxBuffers;
  }
  /**
   * @param maxBuffers The maxBuffers to set.
   */
  public void setMaxBuffers(int maxBuffers)
  throws LogConfigurationException
  {
    if (maxBuffers > 0 && maxBuffers < minBuffers)
      throw new LogConfigurationException("minBuffers [" + minBuffers +
          "] must be <= than maxBuffers[" + maxBuffers +  "]");

    this.maxBuffers = maxBuffers;
    prop.setProperty("maxBuffers", Integer.toString(maxBuffers));
  }

  /**
   * @return Returns the minBuffers.
   */
  public int getMinBuffers() {
    return minBuffers;
  }
  /**
   * @param minBuffers The minBuffers to set.
   */
  public void setMinBuffers(int minBuffers)
  throws LogConfigurationException
  {
    if (minBuffers <= 0)
      throw new LogConfigurationException("minBuffers[" + minBuffers +
          "] must be > 0");

    this.minBuffers = minBuffers;
    prop.setProperty("minBuffers", Integer.toString(minBuffers));
  }

  /**
   * @return Returns the flushSleepTime.
   */
  public int getFlushSleepTime() {
    return flushSleepTime;
  }
  /**
   * @param flushSleepTime The amount of time
   * (specified in milli-seconds) the FlushManager
   * should sleep.
   */
  public void setFlushSleepTime(int flushSleepTime) {
    this.flushSleepTime = flushSleepTime;
    prop.setProperty("flushSleepTime", Integer.toString(flushSleepTime));
  }
  /**
   * @return Returns the threadsWaitingForceThreshold.
   */
  public int getThreadsWaitingForceThreshold() {
    return threadsWaitingForceThreshold;
  }
  /**
   * @param threadsWaitingForceThreshold The threadsWaitingForceThreshold to set.
   */
  public void setThreadsWaitingForceThreshold(int threadsWaitingForceThreshold) {
    this.threadsWaitingForceThreshold = threadsWaitingForceThreshold;
    prop.setProperty("threadsWaitingForceThreshold", Integer.toString(threadsWaitingForceThreshold));
  }
  /**
   * @return Returns the maxBlocksPerFile.
   */
  public int getMaxBlocksPerFile() {
    return maxBlocksPerFile;
  }
  /**
   * @param maxBlocksPerFile The maxBlocksPerFile to set.
   */
  public void setMaxBlocksPerFile(int maxBlocksPerFile) {
    this.maxBlocksPerFile = maxBlocksPerFile;
    prop.setProperty("maxBlocksPerFile", Integer.toString(maxBlocksPerFile));
  }

  /**
   * @return Returns the maxLogFiles.
   */
  public int getMaxLogFiles() {
    return maxLogFiles;
  }
  /**
   * @param maxLogFiles The maxLogFiles to set.
   */
  public void setMaxLogFiles(int maxLogFiles) {
    this.maxLogFiles = maxLogFiles;
    prop.setProperty("maxLogFiles", Integer.toString(maxLogFiles));
  }
  /**
   * @return Returns the logFileMode.
   */
  public String getLogFileMode() {
    return logFileMode;
  }
  /**
   * @param logFileMode The logFileMode to set.
   */
  public void setLogFileMode(String logFileMode)
  throws LogConfigurationException
  {
    if (!logFileMode.equals("rw") && !logFileMode.equals("rwd"))
      throw new LogConfigurationException("logFileMode[" + logFileMode +
          "] must be \"rw\" or \"rwd\"");

    this.logFileMode = logFileMode;
    prop.setProperty("logFileMode", logFileMode);
  }

  public ScheduledExecutorService getScheduler() {
    if (scheduler == null) {
      // Default scheduler is 10 threads
      this.scheduler = new ScheduledThreadPoolExecutor(10, new ThreadFactory() {
          private AtomicInteger threadId = new AtomicInteger();

          public Thread newThread(Runnable r) {
            return new Thread(r, "HOWL-Scheduler-" + threadId.getAndIncrement());
          }
        });
    }

    return scheduler;
  }

  public void setScheduler(ScheduledExecutorService scheduler) {
    this.scheduler = scheduler;
  }

  /**
   * Stores configuration properties to OutputStream.
   *
   * @see java.util.Properties#store(java.io.OutputStream, java.lang.String)
   */
  public void store(OutputStream out)
  throws IOException
  {
    String header = "HOWL Configuration properties\n" +
      "#Generated by " + this.getClass().getName();
    try {
      prop.store(out, header);
    } catch (IOException e) {
      // BUG 303907 add a message to the IOException
      IOException ioe = new IOException("Configuration.store(): error writing properties." +
          "[" + e.getMessage() + "]");
      ioe.setStackTrace(e.getStackTrace());
      throw ioe;
    }
  }

  /**
   * @return Returns the flushPartialBuffers.
   */
  public boolean isFlushPartialBuffers() {
    return flushPartialBuffers;
  }

  /**
   * @param flushPartialBuffers The flushPartialBuffers to set.
   */
  public void setFlushPartialBuffers(boolean flushPartialBuffers) {
    this.flushPartialBuffers = flushPartialBuffers;
    prop.setProperty("flushPartialBuffers", Boolean.toString(flushPartialBuffers));
  }
}
