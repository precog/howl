/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */
package org.objectweb.howl.log;


/**
 * Defines valid values for LogBuffer status.
 */
interface LogBufferStatus
{
  /**
   * IO Status of LogBuffer while it is being filled with
   * new records.
   */
  final static int FILLING  = 0;
  
  /**
   * IO Status of LogBuffer while it is being forced.
   */
  final static int WRITING  = 1;
  
  /**
   * IO Status of LogBuffer when force is complete.
   * <p>Threads waiting for a force may be notified
   * when status is COMPLETE.
   */
  final static int COMPLETE = 2;
  
  /**
   * IO Status of LogBuffer if an IOException occurs
   * during the force.
   * <p>Threads waiting for a force must receive
   * an IOException if an error occurs during force.
   */
  final static int ERROR    = 3;

}