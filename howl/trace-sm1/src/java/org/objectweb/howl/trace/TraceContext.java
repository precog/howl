/*
 * Copyright (c) Core Developers Network LLC, All rights reserved
 */
package org.objectweb.howl.trace;

import java.util.Arrays;

/**
 * <p>A context for capturing trace data that can be replayed if necessary. Typical
 * usage would be for a server application to store trace data in such a
 * context and if a system fault happens replay the data to provide an
 * indication of how the fault was generated.</p>
 *
 * <p>A trace data will normally be captured during production operation,
 * this class is optimized for speed in capturing log event rather than ease
 * of use (or implementation). It is not thread-safe, does not synchronize at
 * any time, and does not create any new objects after instantiation. Futher,
 * the trace buffer is implmented as a ring buffer of the supplied size so
 * older trace information will be lost if the buffer is too small. Finally, the
 * object references stored as trace parameters will not be released until the
 * buffer loops around or clear() is explicitly invoked.
 * </p>
 * <p>This class is final simply to allow HotSpot to inline the methods.</p>
 *
 * @version $Revision: 1.1 $ $Date: 2004-01-08 15:58:31 $
 */
public final class TraceContext {
    private final int size;
    private final Object[] events;
    private final int[] intValue1;
    private final int[] intValue2;
    private final Object[] objectValue1;
    private final Object[] objectValue2;
    private final Object[] objectValue3;
    private int first;
    private int last;

    /**
     * Constructor specifying the size of the internal ring buffer. Once this
     * number of events have been traced, then older ones will be overwritten.
     * @param size the number of events that can be stored
     */
    public TraceContext(int size) {
        this.size = size;
        this.events = new Object[size];
        this.intValue1 = new int[size];
        this.intValue2 = new int[size];
        this.objectValue1 = new Object[size];
        this.objectValue2 = new Object[size];
        this.objectValue3 = new Object[size];
        first = last = 0;
    }

    /**
     * Trace an event with no parameters
     * @param event the event
     */
    public void trace(Object event) {
        events[last] = event;
        next();
    }

    /**
     * Trace an event with a single Object parameter
     * @param event the event
     * @param oparam1 the parameter
     */
    public void trace(Object event, Object oparam1) {
        events[last] = event;
        objectValue1[last] = oparam1;
        next();
    }

    /**
     * Trace an event with a single Object and an single int primitive
     * @param event the event
     * @param oparam1 the Object parameter
     * @param iparam1 the int parameter
     */
    public void trace(Object event, Object oparam1, int iparam1) {
        events[last] = event;
        objectValue1[last] = oparam1;
        intValue1[last] = iparam1;
        next();
    }

    /**
     * Trace with all parameters
     * @param event the event
     * @param oparam1 first Object param
     * @param oparam2 second Object param
     * @param oparam3 third Object param
     * @param iparam1 first int param
     * @param iparam2 second int param
     */
    public void trace(Object event, Object oparam1, Object oparam2, Object oparam3, int iparam1, int iparam2) {
        events[last] = event;
        objectValue1[last] = oparam1;
        objectValue2[last] = oparam2;
        objectValue3[last] = oparam3;
        intValue1[last] = iparam1;
        intValue2[last] = iparam2;
        next();
    }

    private void next() {
        if (++last == size) {
            last = 0;
        }
        if (last == first) {
            if (++first == size) {
                first = 0;
            }
        }
    }

    /**
     * Reset the context to flush the event history. This does not clear the
     * buffers, relying on reuse to discard the Object references
     */
    public void reset() {
        first = last;
    }

    /**
     * Dump the events to the supplied TraceDumper and then reset the history.
     * @param dumper the dumper to dump events to
     */
    public void dump(TraceDumper dumper) {
        for (int i=first; i != last; i = (i+1 == size) ? 0 : i+1) {
            dumper.dump(events[i], objectValue1[i], objectValue2[i], objectValue3[i], intValue1[i], intValue2[i]);
        }
        dumper.flush();
        reset();
    }

    /**
     * Clear all the object references in this context
     */
    public void clear() {
        Arrays.fill(events, null);
        Arrays.fill(objectValue1, null);
        Arrays.fill(objectValue2, null);
        Arrays.fill(objectValue3, null);
    }
}
