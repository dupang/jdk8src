/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.lang;

/**
 * An instance of {@code ThreadDeath} is thrown in the victim thread
 * when the (deprecated) {@link Thread#stop()} method is invoked.
 *
 * 当废弃的Thread.stop()方法被调用的时候，ThreadDeath实现被线程抛出。
 *
 * <p>An application should catch instances of this class only if it
 * must clean up after being terminated asynchronously.  If
 * {@code ThreadDeath} is caught by a method, it is important that it
 * be rethrown so that the thread actually dies.
 *
 * 一处应用程序应该捕获这个类的实例，当它已经异步中止做清理的时候。
 * 如果ThreadDeath被一个方法捕获，重要的是它应该被抛出以便这个线程真正的死掉。
 *
 * <p>The {@linkplain ThreadGroup#uncaughtException top-level error
 * handler} does not print out a message if {@code ThreadDeath} is
 * never caught.
 *
 * ThreadGroup的uncaughtException(最高层的错误拦截器)没有打印出任何信息如果
 * ThreadDeath没有被捕获。
 *
 * <p>The class {@code ThreadDeath} is specifically a subclass of
 * {@code Error} rather than {@code Exception}, even though it is a
 * "normal occurrence", because many applications catch all
 * occurrences of {@code Exception} and then discard the exception.
 *
 * 类ThreadDeath具体来说是一个Error的子类而不是Exception,尽管它是一个“正常发生”，
 * 因为很多应用程序捕获所有的异常然后丢弃异常。
 * @since   JDK1.0
 */

public class ThreadDeath extends Error {
    private static final long serialVersionUID = -4417128565033088268L;
}
