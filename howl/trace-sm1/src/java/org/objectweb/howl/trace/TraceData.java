/*
 * Copyright (c) Core Developers Network LLC, All rights reserved
 */
package org.objectweb.howl.trace;

import java.io.PrintWriter;

/**
 * Interface that denotes an event has being able to format itself based
 * on the data values in the trace.
 * 
 * @version $Revision: 1.1 $ $Date: 2004-01-08 15:58:31 $
 */
public interface TraceData {
    /**
     * Write a message to the supplied writer that formats the supplied trace
     * data fields.
     * @param writer where the message should be written; should normally include a newline
     * @param oparam1 first Object param
     * @param oparam2 second Object param
     * @param oparam3 third Object param
     * @param iparam1 first int param
     * @param iparam2 second int param
     */ 
    void formatTraceData(PrintWriter writer, Object oparam1, Object oparam2, Object oparam3, int iparam1, int iparam2);
}
