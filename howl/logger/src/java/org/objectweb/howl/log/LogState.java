/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 * Created on Apr 2, 2004
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
