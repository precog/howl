/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Created on Jun 17, 2004
 * Contact: jonas-team@objectweb.org
 */
package org.objectweb.howl.log;

/**
 * base class for all log objects that require
 * configuration information.
 * 
 * @author girouxm
 */
class LogObject {
  /**
   * Configuration object from Logger that owns this LogBufferManager.
   */
  Configuration config = null;
  
  /**
   * constructs a LogObject with Configuration supplied by caller.
   * @param config Configuration object.
   */
  LogObject(Configuration config)
  {
    assert config != null: "null Configuration";
    this.config = config;
  }
  
}
