/*
 * Copyright (c) Core Developers Network LLC, All rights reserved
 */
package org.objectweb.howl;

import junit.framework.TestCase;
import org.objectweb.howl.trace.TraceContext;

/**
 *
 *
 *
 * @version $Revision: 1.1 $ $Date: 2004-01-08 15:58:31 $
 */
public class SpeedTest extends TestCase {
    private static final Integer one = new Integer(1);
    private static final Integer two = new Integer(2);
    private static final Integer three = new Integer(3);
    private static final Integer four = new Integer(4);

    public void testMethodInvoke() {
        Target target = new Target();
        long start = System.currentTimeMillis();
        for (int i=0; i < 1000000000; i++) {
            target.method(1,2,3,4,one,two,three,four);
        }
        long end = System.currentTimeMillis();
        System.out.println("testMethodInvoke: "+(end-start)/1000);
    }

    public void testSyncInvoke() {
        Target target = new Target();
        long start = System.currentTimeMillis();
        for (int i=0; i < 100000000; i++) {
            target.method3(1,2,3,4,one,two,three,four);
        }
        long end = System.currentTimeMillis();
        System.out.println("testMethodInvoke: "+(end-start)/100);
    }

    public void testMethodInvokeWithArray() {
        Target target = new Target();
        long start = System.currentTimeMillis();
        for (int i=0; i < 1000000; i++) {
            target.method2(new Object[] { new Integer(1),new Integer(2),new Integer(3),new Integer(4),one,two,three,four});
        }
        long end = System.currentTimeMillis();
        System.out.println("testMethodInvokeWithArray: "+(end-start)/1);
    }

    public void testThreadLocalGet() {
        ThreadLocal tl = new ThreadLocal();
        tl.set(new Integer(1));
        long start = System.currentTimeMillis();
        for (int i=0; i < 1000000; i++) {
            Object o = tl.get();
        }
        long end = System.currentTimeMillis();
        System.out.println("testThreadLocalGet: "+(end-start)/1);
    }

    public void testLogger() {
        TraceContext trace = new TraceContext(1000);
        long start = System.currentTimeMillis();
        for (int i=0; i < 1000000; i++) {
            for (int j=0; j < 50; j++) {
                trace.trace("A", null);
            }
            trace.reset();
        }
        long end = System.currentTimeMillis();
        System.out.println("testLogger: "+(end-start)/1);
    }

    private static class Target {
        private int i;
        public final void method(int i1, int i2, int i3, int i4, Object o1, Object o2, Object o3, Object o4) {
            i += i2;
        }

        public final void method2(Object[] args) {
            i++;
        }

        public synchronized final void method3(int i1, int i2, int i3, int i4, Object o1, Object o2, Object o3, Object o4) {
            i++;
        }
    }
}
