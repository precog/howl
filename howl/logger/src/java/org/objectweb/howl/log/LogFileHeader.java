
package org.objectweb.howl.log;
/*
 * Created on Mar 29, 2004
 */

/**
 * Defines the header record for a LogFile.
 * 
 * @author Michael Giroux
 *
 */
class LogFileHeader
{
  boolean automark = false;
  
  /**
   * data written to log when autoMark is turned on
   */
  final byte[] autoMarkOn = new byte[] { 1 };
  
  /**
   * data written to log when autoMark is turned off
   */
  final byte[] autoMarkOff = new byte[] { 0 };

  long activeMark = 0;
  
  long cfHighMark = 0;
  
  long cfTOD = 0;
  
  byte[] crlf = "\r\n".getBytes();
  
}
