/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */

package org.objectweb.howl.log;

/**
 * Checked exception thrown when the byte[] passed to <i>put</i>
 * is larger than the configured buffer size.
 * 
 * <p>LogBufferManager does not support spanned records
 * (records that span physical blocks).  Increase the
 * configured journal block size to accomodate larger
 * records.
 */
public class LogRecordSizeException extends LogException
{
    /**
     * Constructs an instance of this class.
     */
    public LogRecordSizeException() { }

    /**
     * Constructs an instance of this class with
     * specified description.
     * 
     * @param size maximum size of a user data record
     */
    public LogRecordSizeException(int size)
    {
      super("maximum user data record size: " + size);
    }

}
