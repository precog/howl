/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 * Created on Jun 14, 2004
 */
package org.objectweb.howl.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Provides configuration information for a
 * {@link org.objectweb.howl.log.Logger Logger}
 * instance.
 * @author girouxm
 */
public class Configuration {

  /**
   * Construct a Configuration object with default values.
   * <p>Caller will use setter methods to change the defaults.
   */
  public Configuration()
  {
  }
  
  /**
   * Construct a Configuration object using a Properties
   * object supplied by the caller.
   * @param prop Properties object containing default settings
   */
  public Configuration(Properties prop) throws LogConfigurationException
  {
    // perform default construction
    this();
    
    // apply settings from caller supplied Properties parameter
    parseProperties(prop);
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
    
    Properties prop = new Properties();
    try {
      prop.load(new FileInputStream(propertyFile));
      parseProperties(prop);
    } catch (FileNotFoundException e)
    {
      throw new LogConfigurationException(e.toString());
    } catch (IOException e)
    {
      throw new LogConfigurationException(e.toString());
    }
    
  }
  
  private void showConfig(String key, String val)
  {
    if (listConfig) System.err.println(key + ": " + val);
  }
  
  private void showConfig(String key, int val)
  {
    if (listConfig) System.err.println(key + ": " + val);
  }
  
  private void showConfig(String key, boolean val)
  {
    if (listConfig) System.err.println(key + ": " + val);
  }
  
  private void parseProperties(Properties prop) throws LogConfigurationException
  {
    String  val = null;
    String  key = null;
    
    val = prop.getProperty(key = "listConfig", "true").toLowerCase();
    listConfig = val.equals("true");
    showConfig(key, listConfig);

    bufferClassName = prop.getProperty(key = "bufferClassName", bufferClassName);
    showConfig("bufferClassName", bufferClassName);
    
    val = prop.getProperty(key = "bufferSize");
    if (val != null)
    {
      int ival = Integer.parseInt(val);
      if (ival > 1024) ival = (ival + 512) / 1024; // number of 1K buffers
      bufferSize = ival * 1024;
    }
    showConfig(key, bufferSize);
    
    val = prop.getProperty(key = "checksumEnabled", "true").toLowerCase();
    checksumEnabled = val.equals("true");
    showConfig(key, checksumEnabled);
    
    val = prop.getProperty(key = "flushSleepTime");
    if (val != null) flushSleepTime = Integer.parseInt(val);
    showConfig(key, flushSleepTime);
    
    logFileDir = prop.getProperty(key = "logFileDir", logFileDir);
    showConfig(key, logFileDir);
    
    logFileExt = prop.getProperty(key = "logFileExt", logFileExt);
    showConfig(key, logFileExt);
    
    logFileName = prop.getProperty(key = "logFileName", logFileName);
    showConfig(key, logFileName);

    val = prop.getProperty(key = "maxBlocksPerFile");
    if (val != null) maxBlocksPerFile = Integer.parseInt(val);
    showConfig(key, maxBlocksPerFile);
    
    val = prop.getProperty(key = "minBuffers");
    if (val != null) minBuffers = Integer.parseInt(val);
    showConfig(key, minBuffers);

    val = prop.getProperty(key = "maxBuffers");
    if (val != null) maxBuffers = Integer.parseInt(val);
    showConfig(key, maxBuffers);
    
    val = prop.getProperty(key = "maxLogFiles");
    if (val != null) maxLogFiles = Integer.parseInt(val);
    showConfig(key, maxLogFiles);
    
    val = prop.getProperty(key = "threadsWaitingForceThreshold");
    if (val != null) threadsWaitingForceThreshold = Integer.parseInt(val);
    showConfig(key, threadsWaitingForceThreshold);
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
  private boolean checksumEnabled = true;

  /**
   * Size (in bytes) of buffers used to write log blocks.
   * 
   * <p>The default size of 4K bytes should be suitable
   * for most applications.
   * <p>Larger buffers may provide improved performance
   * for applications with transaction rates that
   * exceed 5K TX/Sec.
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
   * the maximum number of threads that should wait
   * for an IO force.
   * <p>Setting this value may have an effect
   * on latency when threads are waiting for
   * the force. 
   * 
   * <p>By default, there is no limit.
   */
  private int threadsWaitingForceThreshold = Integer.MAX_VALUE;
  
  /* ---------------------------------------------------
   * LogFileManager configuration settings
   * ---------------------------------------------------
   */
  
  /**
   * maximum number of blocks to store in each LogFile.
   * 
   * <p>controls when logging is switched to a new log file, and/or when a circular
   *  log is reset to  seek address zero.
   * 
   * @see #getLogFileForWrite(LogBuffer)
   */
  int maxBlocksPerFile = Integer.MAX_VALUE;
  /**
   * number of log files to configure.
   * <p>Default is 2 log files.
   */
  int maxLogFiles = 2;
  
  /**
   * directory used to create log files.
   * <p>Default is current directory.
   */
  String logFileDir = "../logs";
  
  /**
   * file name extension for log files.
   * <p>Default value is "log"
   */
  String logFileExt = "log";
  
  /**
   * filename used to create log files.
   * <p>Default value is "logger"
   * <p>file names are generated using the following pattern:
   * <pre>
   *   <value of logFileName> + "_" + <file number> + "." + <value of logFileExt>
   * </pre>
   */
  String logFileName = "logger";
  
  /**
   * @return Returns the logDir.
   */
  public String getLogFileDir() {
    return logFileDir;
  }
  /**
   * @param logDir The logDir to set.
   */
  public void setLogFileDir(String logDir) {
    this.logFileDir = logDir;
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
  }
  
  /**
   * @return Returns the bufferSize.
   */
  public int getBufferSize() {
    return bufferSize;
  }
  /**
   * @param bufferSize The size of a log buffer
   * specified as a number of 1024 byte blocks.
   * <p>The value specified by bufferSize is
   * multiplied by 1024 to establish the actual
   * buffer size used by the logger.
   */
  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize * 1024;
  }
  
  /**
   * @return Returns the bufferClassName.
   */
  public String getBufferClassName() {
    return bufferClassName;
  }
  /**
   * @param bufferClassName The bufferClassName to set.
   */
  public void setBufferClassName(String bufferClassName) {
    this.bufferClassName = bufferClassName;
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
  public void setMaxBuffers(int maxBuffers) {
    this.maxBuffers = maxBuffers;
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
  public void setMinBuffers(int minBuffers) {
    this.minBuffers = minBuffers;
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
  }
}
