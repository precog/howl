/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */

package org.objectweb.howl.log;

/**
 * Exception thrown when Logger detects a journal block
 * with invalid content.
 * <p>Methods that read log blocks and process
 * records within the blocks detect invalid content
 * and throw this exception.
 * <p>Reasons for throwing this exception include:
 * <ul>
 * <li>Invalid block header information
 * <li>Invalid block footer information
 * <li>Record size exceeds bytes used for block
 * </ul>
 */
public class InvalidLogBufferException extends LogException
{

    public InvalidLogBufferException() { }

    public InvalidLogBufferException(String s) { super(s); }
    
}
