/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 */
package org.objectweb.howl.log;

/**
 * An implementation of BlockLogBuffer that does *not* perform IO.

 * <p>This class implements a sink for log records.  It can be used to
 * measure the performance of the log implementation without IO.
 *  
 * @author Michael Giroux
 *
 */
class BlockLogBufferSink extends BlockLogBuffer
{
    BlockLogBufferSink()
    {
      super(false); // set doWrite false
    }
}