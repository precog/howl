/*
 * Copyright (c) Core Developers Network LLC, All rights reserved
 */
package org.objectweb.howl;

import java.io.PrintWriter;

import org.apache.log4j.Priority;
import org.apache.log4j.Logger;
import org.objectweb.howl.trace.TraceContext;
import org.objectweb.howl.trace.TraceData;
import org.objectweb.howl.trace.log4j.Log4JDumper;

/**
 * 
 * 
 * @version $Revision: 1.1 $ $Date: 2004-01-08 15:58:31 $
 */
public class SimpleTrace {
    private static final Data EVENT3 = new Data("Boolean Value:");

    public static void main(String[] args) {
        TraceContext context = new TraceContext(50);
        context.trace("Event 1", null);
        context.trace("Event 2", null);
        context.trace(EVENT3, Boolean.TRUE);
        context.trace(new RuntimeException("A Runtime Exception"), null);

        Logger logger = Logger.getLogger(SimpleTrace.class);
        Log4JDumper.dump(logger, Priority.INFO, "Trace data follows", context);
    }

    private static class Data implements TraceData {
        private final String message;

        public Data(String message) {
            this.message = message;
        }

        public void formatTraceData(PrintWriter writer, Object oparam1, Object oparam2, Object oparam3, int iparam1, int iparam2) {
            writer.println(message + oparam1);
        }
    }
}
