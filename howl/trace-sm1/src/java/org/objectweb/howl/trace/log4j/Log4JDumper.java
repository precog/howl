/*
 * Copyright (c) Core Developers Network LLC, All rights reserved
 */
package org.objectweb.howl.trace.log4j;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.objectweb.howl.trace.TraceContext;
import org.objectweb.howl.trace.TraceData;
import org.objectweb.howl.trace.TraceDumper;

/**
 * A TraceDumper which writes the trace data to a log4j Logger.
 * 
 * @version $Revision: 1.1 $ $Date: 2004-01-08 15:58:31 $
 */
public class Log4JDumper implements TraceDumper {
    private final Logger logger;
    private final Priority level;

    private final StringWriter out = new StringWriter(1024);
    private final PrintWriter writer = new PrintWriter(out);

    public static void dump(Logger logger, Priority level, Object message, TraceContext context) {
        if (logger.isEnabledFor(level)) {
            Log4JDumper dumper = new Log4JDumper(logger, level);
            dumper.writer.println(message);
            context.dump(dumper);
        }
    }

    /**
     * Constructor taking the Logger to dump to and the Priority to dump use
     * @param logger where the trace should be logged
     * @param level the level at which the trace data should be logged
     */
    public Log4JDumper(Logger logger, Priority level) {
        this.logger = logger;
        this.level = level;
    }

    public void dump(Object event, Object oparam1, Object oparam2, Object oparam3, int iparam1, int iparam2) {
        if (event instanceof TraceData) {
            ((TraceData) event).formatTraceData(writer, oparam1, oparam2, oparam3, iparam1, iparam2);
        } else if (event instanceof Throwable) {
            ((Throwable) event).printStackTrace(writer);
        } else {
            writer.println(event);
        }
    }

    public void flush() {
        logger.log(level, out.toString());
    }

    public String toString() {
        return out.toString();
    }
}
