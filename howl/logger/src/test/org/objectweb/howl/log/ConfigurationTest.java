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
 * $Id: ConfigurationTest.java,v 1.13 2007-01-02 17:17:19 girouxm Exp $
 * ------------------------------------------------------------------------------
 */
package org.objectweb.howl.log;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * 
 * @author Michael Giroux
 */
public class ConfigurationTest extends TestCase
{
  Configuration cfg = null;
  
  Properties prop = null;

  PrintStream systemErr;
    private File baseDir;
    private File outDir;

    public static void main(String[] args) {
    junit.textui.TestRunner.run(ConfigurationTest.class);
  }
  
  /*
   * Start every test with a new Properties object
   * 
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();

    String baseDirName = System.getProperty("basedir", ".");
    baseDir = new File(baseDirName);
    outDir = new File(baseDir, "target/test-resources");
    outDir.mkdirs();
    systemErr = System.err;
    prop = new Properties();
  }
  /*
   * @see TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    System.setErr(systemErr);
  }

  
  /**
   * Delete log files for tests that verify behaviour
   * against newly created files.
   */
  protected void deleteLogFiles()
  {
    String logDir = cfg.getLogFileDir();
    File dir = new File(logDir);
    File[] logs = dir.listFiles();
    if (logs == null || logs.length == 0) return; // nothing to delete
    
    String logFileName = cfg.getLogFileName();
    String logFileExt = cfg.getLogFileExt();
    
    // file name pattern for current test
    String pattern = "^" + logFileName + "_" + "\\d+\\." + logFileExt;

    for (int i=0; i<logs.length; ++i)
    {
      File f = logs[i];
      if (f.getName().matches(pattern))
        f.delete();
    }
  }
  
  /**
   * Constructor for XALoggerTest.
   * @param name
   */
  public ConfigurationTest(String name) {
    super(name);
  }
  
  /**
   * Generate NPE if Configuration constructor
   * trys to display configuration information.
   * 
   * @throws LogException
   * @throws Exception
   */
  public void testListConfigDefault()
  throws LogException, Exception
  {
    System.setErr(null);
    cfg = new Configuration(prop);
  }
  
  public void testListConfigTrue()
  throws LogException, Exception
  {
    System.setErr(null);
    prop.setProperty("listConfig", "true");
    try {
      cfg = new Configuration(prop);
      fail("Expecting NPE");
    } catch (NullPointerException e) {
      // ignore it -- this is what we expected
    }
  }
  
  public void testListConfigFalse()
  throws LogException, Exception
  {
    System.setErr(null);
    prop.setProperty("listConfig", "false");
    cfg = new Configuration(prop);
  }
  
  public void testBufferSize_1()
  throws LogException, Exception
  {
    prop.setProperty("bufferSize", "1");
    cfg = new Configuration(prop);
    assertEquals("bufferSize", 1, cfg.getBufferSize()); // BUG 300957
    
    // make sure values are trimmed
    prop.setProperty("bufferSize", " 1");
    cfg = new Configuration(prop);
    assertEquals("bufferSize", 1, cfg.getBufferSize()); // BUG 300957
  }
  
  public void testBufferSize_32()
  throws LogException, Exception
  {
    prop.setProperty("bufferSize", "32");
    cfg = new Configuration(prop);
    assertEquals("bufferSize", 32 , cfg.getBufferSize()); // BUG 300957
  }
  
  public void testBufferSize_0()
  throws LogException, Exception
  {
    prop.setProperty("bufferSize", "0");
    try {
      cfg = new Configuration(prop);
      fail("Expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      // we expected it
    }
  }
  
  // MG 20060509 rename testBufferSize_33 to testMaxBufferSize
  public void testMaxBufferSize()
  throws LogException, Exception
  {
    // MG 20060508 use Configuration.MAX_BUFFER_SIZE instead of literal.
    prop.setProperty("bufferSize", "" + (Configuration.MAX_BUFFER_SIZE + 1));
    try {
      cfg = new Configuration(prop);
      fail("Expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      // we expected it
    }
  }
  
  public void testAdler32ChecksumError()
  {
    prop.setProperty("adler32Checksum", "yes");
    try {
      cfg = new Configuration(prop);
      fail("Expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      // ignore the exception
    }
  }
  
  public void testAdler32ChecksumTrue() throws Exception
  {
    prop.setProperty("adler32Checksum", "true");
    prop.setProperty("checksumEnabled", "true");
    cfg = new Configuration(prop);
    assertTrue("adler32Checksum", cfg.isAdler32ChecksumEnabled());

    // check that values are trimmed
    prop.setProperty("adler32Checksum", "true  ");
    cfg = new Configuration(prop);
    assertTrue("adler32Checksum", cfg.isAdler32ChecksumEnabled());
    
    deleteLogFiles();  // start with fresh log files
    Logger log = new Logger(cfg);
    log.open();
    LogBuffer lb = log.bmgr.getLogBuffer(-1);
    // verify that the Adler32 checksum option will be used
    assertFalse("checksum method ", lb.checksum == null);
    assertTrue("checksum method ", (lb.checksum instanceof java.util.zip.Adler32));
    log.close();
    deleteLogFiles(); 
  }
  
  public void testAdler32ChecksumFalse() throws Exception
  {
    prop.setProperty("adler32Checksum", "false");
    cfg = new Configuration(prop);
    assertFalse("adler32Checksum", cfg.isAdler32ChecksumEnabled());

    // check that values are trimmed
    prop.setProperty("adler32Checksum", "false  ");
    cfg = new Configuration(prop);
    assertFalse("adler32Checksum", cfg.isAdler32ChecksumEnabled());

    // start with fresh log files to avoid checksum failures during restart
    deleteLogFiles();  
    Logger log = new Logger(cfg);
    log.open();
    LogBuffer lb = log.bmgr.getLogBuffer(-1);
    // verify that the default ByteBuffer.hashCode() will be used
    assertTrue("checksum method ", lb.checksum == null);
    log.close();
    deleteLogFiles();
  }
  
  public void testChecksumEnabledError()
  {
    prop.setProperty("checksumEnabled", "yes");
    try {
      cfg = new Configuration(prop);
      fail("Expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      // ignore the exception
    }
  }
  
  public void testChecksumEnabledTrue() throws Exception
  {
    prop.setProperty("checksumEnabled", "true");
    cfg = new Configuration(prop);
    assertTrue("checksumEnabled", cfg.isChecksumEnabled());

    // check that values are trimmed
    prop.setProperty("checksumEnabled", "true  ");
    cfg = new Configuration(prop);
    assertTrue("checksumEnabled", cfg.isChecksumEnabled());
  }
  
  public void testChecksumEnabledFalse() throws Exception
  {
    prop.setProperty("checksumEnabled", "false");
    cfg = new Configuration(prop);
    assertFalse("checksumEnabled", cfg.isChecksumEnabled());
    
    // check that values are trimmed
    prop.setProperty("checksumEnabled", "   false   ");
    cfg = new Configuration(prop);
    assertFalse("checksumEnabled", cfg.isChecksumEnabled());
  }
  
  public void testMinBuffers_GT_MaxBuffers()
  {
    prop.setProperty("maxBuffers", "1");
    prop.setProperty("minBuffers", "2");
    try {
      cfg = new Configuration(prop);
      fail("Expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      // ignore the error
    }
  }
  
  public void testMinBuffers_LE_zero()
  {
    prop.setProperty("minBuffers", "-1");
    try {
      cfg = new Configuration(prop);
      fail("Expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      // ignore the error
    }

    prop.setProperty("minBuffers", "0");
    try {
      cfg = new Configuration(prop);
      fail("Expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      // ignore the error
    }
  }
  
  public void testMinBuffers_EQ_MaxBuffers()
  throws LogException, Exception
  {
    prop.setProperty("maxBuffers", "1");
    prop.setProperty("minBuffers", "1");
    try {
      cfg = new Configuration(prop);
    } catch (LogConfigurationException e) {
      throw e;
    }
  }
  
  public void testConstrucFromFile_FileNotFound()
  throws LogException, Exception
  {
    File file = new File(baseDir, "src/test-resources/filenotfound.properties");
    try {
      cfg = new Configuration(file);
      fail("Expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      // ignore error if cause was FileNotfoundException -- otherwise rethrow
      Throwable cause = e.getCause();
      if (!(cause instanceof FileNotFoundException)) throw e;
    }
  }
  
  /**
   * Verify that construct using a File works correctly.
   * <p>Creates a default Configuration then modifies some properties
   * and writes a new property file. Finally, constructs a Configuration
   * using the file (just created) and verifies the properties to
   * be the ones saved.
   * 
   * @throws LogException
   * @throws Exception
   */
  public void testConstructFromFile()
  throws LogException, Exception
  {
    // get a default configuration
    cfg = new Configuration();
    
    // define properties with non-default values
    prop.setProperty("maxLogFiles", Integer.toString(cfg.getMaxLogFiles() + 1));
    prop.setProperty("minBuffers", Integer.toString(cfg.getMinBuffers() + 1));
    
    // save the Properties object to a file
    File file = new File(outDir, "testConstructFromFileLog.properties");
    prop.store(new FileOutputStream(file),"testConstructFromFile test properties");
    
    // construct a new Configuration using the test properties
    Configuration cfg2 = new Configuration(file);
    
    // validate the custom values
    assertEquals("maxLogFiles", cfg.getMaxLogFiles()+1, cfg2.getMaxLogFiles());
    cfg.setMaxLogFiles(cfg2.getMaxLogFiles());
    
    assertEquals("minBuffers", cfg.getMinBuffers()+1, cfg2.getMinBuffers());
    cfg.setMinBuffers(cfg2.getMinBuffers());
    
    // validate cfg2
    verifyConfiguration(cfg2);
    
    
  }

    /**
   * compare a Configuration object with <var> this.cfg </var>.
   * @param cfg a Configuration to be compared.
   */
  private void verifyConfiguration(Configuration cfg)
  {
    assertEquals("bufferClassName", this.cfg.getBufferClassName(),  cfg.getBufferClassName());
    assertEquals("bufferSize", this.cfg.getBufferSize(),       cfg.getBufferSize());
    assertEquals("checksumEnabled", this.cfg.isChecksumEnabled(), cfg.isChecksumEnabled());
    assertEquals("flushSleepTime", this.cfg.getFlushSleepTime(),   cfg.getFlushSleepTime());
    assertEquals("logFileDir", this.cfg.getLogFileDir(),       cfg.getLogFileDir());
    assertEquals("logFileExt", this.cfg.getLogFileExt(),       cfg.getLogFileExt());
    assertEquals("logFileName", this.cfg.getLogFileName(),      cfg.getLogFileName());
    assertEquals("maxBlocksPerFile", this.cfg.getMaxBlocksPerFile(), cfg.getMaxBlocksPerFile());
    assertEquals("minBuffers", this.cfg.getMinBuffers(),       cfg.getMinBuffers());
    assertEquals("maxBuffers", this.cfg.getMaxBuffers(),       cfg.getMaxBuffers());
    assertEquals("maxLogFiles", this.cfg.getMaxLogFiles(),      cfg.getMaxLogFiles());
    assertEquals("threadsWaitingForceThreshold", this.cfg.getThreadsWaitingForceThreshold(), cfg.getThreadsWaitingForceThreshold());
  }

  public void testConstructFromProperties()
  throws LogException, Exception
  {
    
    cfg = new Configuration();
    prop.setProperty("bufferClassName", cfg.getBufferClassName());
    prop.setProperty("logFileDir", cfg.getLogFileDir());
    prop.setProperty("logFileExt", cfg.getLogFileExt());
    prop.setProperty("logFileName", cfg.getLogFileName());
    prop.setProperty("bufferSize", Integer.toString(cfg.getBufferSize())); // BUG 300957
    prop.setProperty("flushSleepTime", Integer.toString(cfg.getFlushSleepTime()));
    prop.setProperty("flushPartialBuffers", Boolean.toString(cfg.isFlushPartialBuffers()));
    prop.setProperty("maxLogFiles", Integer.toString(cfg.getMaxLogFiles()));
    prop.setProperty("maxBlocksPerFile", Integer.toString(cfg.getMaxBlocksPerFile()));
    prop.setProperty("maxBuffers", Integer.toString(cfg.getMaxBuffers()));
    prop.setProperty("minBuffers", Integer.toString(cfg.getMinBuffers()));
    prop.setProperty("checksumEnabled", Boolean.toString(cfg.isChecksumEnabled()));
    prop.setProperty("threadsWaitingForceThreshold", Integer.toString(cfg.getThreadsWaitingForceThreshold()));
    
    Configuration cfg2 = new Configuration(prop);
    verifyConfiguration(cfg2);
  }
  
  public void testSetMethods()
  throws LogException, Exception
  {
    cfg = new Configuration();
    Configuration cfg3 = new Configuration();
    cfg3.setBufferClassName(cfg.getBufferClassName());
    cfg3.setLogFileDir(cfg.getLogFileDir());
    cfg3.setLogFileExt(cfg.getLogFileExt());
    cfg3.setLogFileName(cfg.getLogFileName());
    cfg3.setBufferSize(cfg.getBufferSize()); // BUG 300957
    cfg3.setMaxBlocksPerFile(cfg.getMaxBlocksPerFile());
    cfg3.setMaxLogFiles(cfg.getMaxLogFiles());
    cfg3.setMaxBuffers(cfg.getMaxBuffers());
    cfg3.setMinBuffers(cfg.getMinBuffers());
    cfg3.setChecksumEnabled(cfg.isChecksumEnabled());
    cfg3.setFlushSleepTime(cfg.getFlushSleepTime());
    cfg3.setThreadsWaitingForceThreshold(cfg.getThreadsWaitingForceThreshold());
    cfg3.setFlushPartialBuffers(cfg.isFlushPartialBuffers());

    verifyConfiguration(cfg3);
  }
  
  public void testLogFileMode()
  throws LogException, Exception
  {
    cfg = new Configuration();
    cfg.setLogFileMode("rw");
    cfg.setLogFileMode("rwd");
    try {
      cfg.setLogFileMode("r");
      fail(getName() + ": expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      // ignore error
    }
    try {
      cfg.setLogFileMode("RW");
      fail(getName() + ": expected LogConfigurationException");
    } catch (LogConfigurationException e) {
      // ignore error
    }
  }
  
  public void testStore() throws Exception
  {
    cfg = new Configuration();
    File file = new File(outDir, getName() + ".properties");
    cfg.store(new FileOutputStream(file));
  }
  
  public void testCallerPropertiesNotChanged() throws Exception
  {
    
    cfg = new Configuration(); // so we can extract a valid property
    prop.setProperty("bufferClassName", cfg.getBufferClassName());
    
    // Make a duplicate set of properties for compare later
    Properties original = new Properties();
    original.setProperty("bufferClassName", cfg.getBufferClassName());
    
    // construct new Configuration using test Properties
    cfg = new Configuration(prop);
    // make sure we still have exactly one property
    assertEquals(1, prop.size());
    
    // modify Configuration object with some new properties 
    cfg.setBufferSize(cfg.getBufferSize() + 1); // BUG 300957
    cfg.setBufferClassName(cfg.getBufferClassName() + "Sink");
    
    // now compare the current and original properties
    assertEquals(original.size(), prop.size());
    for (Enumeration e = original.keys(); e.hasMoreElements(); )
    {
      String key = (String) e.nextElement();
      assertTrue(prop.containsKey(key));
      assertEquals(prop.getProperty(key), original.getProperty(key));
    }
  }
  
}
