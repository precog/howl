/*
 * Copyright (c) Core Developers Network LLC, All rights reserved
 */
package org.objectweb.howl.trace;

/**
 * Interface used by trace to dump events.
 * 
 * @version $Revision: 1.1 $ $Date: 2004-01-08 15:58:31 $
 */
public interface TraceDumper {
    /**
     * Called by a context for every trace event that should be dumped.
     * @param event the event to trace
     * @param oparam1 first Object param
     * @param oparam2 second Object param
     * @param oparam3 third Object param
     * @param iparam1 first int param
     * @param iparam2 second int param
     */
    void dump(Object event, Object oparam1, Object oparam2, Object oparam3, int iparam1, int iparam2);

    /**
     * Called by a context when all events have been dumped. 
     */
    void flush();
}
