/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */

package org.objectweb.howl.log;

/**
 * Exception thrown when XAJournalReader detects a journal block
 * with an invalid header.
 */
public class InvalidLogKeyException extends LogException
{

    public InvalidLogKeyException() { }

    public InvalidLogKeyException(String s) { super(s); }

}
