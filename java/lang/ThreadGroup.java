/*
 * Copyright (c) 1995, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintStream;
import java.util.Arrays;
import sun.misc.VM;

/**
 * A thread group represents a set of threads. In addition, a thread
 * group can also include other thread groups. The thread groups form
 * a tree in which every thread group except the initial thread group
 * has a parent.
 * 一个线程组代表一组线程。此外，一个线程组可以包含其它线程组。线程组形成一个树状结构
 * ，这个结构中每一个线程组都有一个父亲，除非初始线程组。
 * <p>
 * A thread is allowed to access information about its own thread
 * group, but not to access information about its thread group's
 * parent thread group or any other thread groups.
 *
 * 一个线程允许访问它自己的线程组。但不能访问线程组的父线程组或其它线程组。
 *
 * @author  unascribed
 * @since   JDK1.0
 */
/* The locking strategy for this code is to try to lock only one level of the
 * tree wherever possible, but otherwise to lock from the bottom up.
 * That is, from child thread groups to parents.
 * This has the advantage of limiting the number of locks that need to be held
 * and in particular avoids having to grab the lock for the root thread group,
 * (or a global lock) which would be a source of contention on a
 * multi-processor system with many thread groups.
 * This policy often leads to taking a snapshot of the state of a thread group
 * and working off of that snapshot, rather than holding the thread group locked
 * while we work on the children.
 *
 * 这个代码的锁策略是试图只锁树的任何可能的一层，但从树的底部开始。
 * 也就是说，从孩子线程组到父线程组。这有限制需要持有的锁的好处。并且特别是避免不得不获取根线程组的锁(全局锁)
 * 。在有很多线程组的多处理器系统中这可能是竞争的源头。
 * 这个策略经常导致持有的是一个线程组的状态快照，并在这个快照上操作，而不是保持线程组被锁着当我们在孩子上面操作的时候。
 *
 *
 */
public
class ThreadGroup implements Thread.UncaughtExceptionHandler {
    private final ThreadGroup parent;
    String name;
    int maxPriority;
    boolean destroyed;
    boolean daemon;
    boolean vmAllowSuspension;

    int nUnstartedThreads = 0;
    int nthreads;
    Thread threads[];

    int ngroups;
    ThreadGroup groups[];

    /**
     * Creates an empty Thread group that is not in any Thread group.
     * This method is used to create the system Thread group.
     * 创建一个没有任何线程组的空线程组。
     * 这个方法被用来创建系统线程组。
     */
    private ThreadGroup() {     // called from C code
        this.name = "system";
        this.maxPriority = Thread.MAX_PRIORITY;
        this.parent = null;
    }

    /**
     * Constructs a new thread group. The parent of this new group is
     * the thread group of the currently running thread.
     * <p>
     * The <code>checkAccess</code> method of the parent thread group is
     * called with no arguments; this may result in a security exception.
     *
     * 构建一个新的线程组，这个新线程组的父亲是当前运行线程的线程组。
     * 父线程组的没有参数的checkAccess方法被调用,这可能导致抛出安全异常。
     *
     * @param   name   the name of the new thread group.
     * @exception  SecurityException  if the current thread cannot create a
     *               thread in the specified thread group.
     * @see     java.lang.ThreadGroup#checkAccess()
     * @since   JDK1.0
     */
    public ThreadGroup(String name) {
        this(Thread.currentThread().getThreadGroup(), name);
    }

    /**
     * Creates a new thread group. The parent of this new group is the
     * specified thread group.
     * <p>
     * The <code>checkAccess</code> method of the parent thread group is
     * called with no arguments; this may result in a security exception.
     *
     * 创建一个新的线程组，新线程组的父亲，是参数指定的线程组。
     * 父线程组的没有参数的checkAccess方法被调用，这可能导致抛出安全异常。
     *
     * @param     parent   the parent thread group.
     * @param     name     the name of the new thread group.
     * @exception  NullPointerException  if the thread group argument is
     *               <code>null</code>.
     * @exception  SecurityException  if the current thread cannot create a
     *               thread in the specified thread group.
     * @see     java.lang.SecurityException
     * @see     java.lang.ThreadGroup#checkAccess()
     * @since   JDK1.0
     */
    public ThreadGroup(ThreadGroup parent, String name) {
        this(checkParentAccess(parent), parent, name);
    }

    private ThreadGroup(Void unused, ThreadGroup parent, String name) {
        this.name = name;
        this.maxPriority = parent.maxPriority;
        this.daemon = parent.daemon;
        this.vmAllowSuspension = parent.vmAllowSuspension;
        this.parent = parent;
        parent.add(this);
    }

    /*
     * @throws  NullPointerException  if the parent argument is {@code null} 如果parent参数为null
     * @throws  SecurityException     if the current thread cannot create a
     *                                thread in the specified thread group. 如果当前线程不能在指定的线程组中创建一个线程
     */
    private static Void checkParentAccess(ThreadGroup parent) {
        parent.checkAccess();
        return null;
    }

    /**
     * Returns the name of this thread group.
     *
     * 返回这个线程组的名字。
     *
     * @return  the name of this thread group.
     * @since   JDK1.0
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the parent of this thread group.
     *
     * 返回这个线程组的父亲。
     *
     * <p>
     * First, if the parent is not <code>null</code>, the
     * <code>checkAccess</code> method of the parent thread group is
     * called with no arguments; this may result in a security exception.
     *
     * 首先，如果父亲不为null,父线程组的没有参数的checkAccess方法被调用，这可能导致安全异常。
     *
     * @return  the parent of this thread group. The top-level thread group
     *          is the only thread group whose parent is <code>null</code>.
     *          这个线程组的父亲。最高层的线程组是仅有的线程组，这个线程组的父亲是null.
     * @exception  SecurityException  if the current thread cannot modify
     *               this thread group.
     * @see        java.lang.ThreadGroup#checkAccess()
     * @see        java.lang.SecurityException
     * @see        java.lang.RuntimePermission
     * @since   JDK1.0
     */
    public final ThreadGroup getParent() {
        if (parent != null)
            parent.checkAccess();
        return parent;
    }

    /**
     * Returns the maximum priority of this thread group. Threads that are
     * part of this group cannot have a higher priority than the maximum
     * priority.
     *
     * 返回这个线程组的最大优先级。在这个线程组中的线程的优先级不能比最大优先级更高。
     *
     * @return  the maximum priority that a thread in this thread group
     *          can have.
     * @see     #setMaxPriority
     * @since   JDK1.0
     */
    public final int getMaxPriority() {
        return maxPriority;
    }

    /**
     * Tests if this thread group is a daemon thread group. A
     * daemon thread group is automatically destroyed when its last
     * thread is stopped or its last thread group is destroyed.
     *
     * 检测这个线程组是否是一个守护线程组。一个守护线程组被自动地销毁，当它的最后一个
     * 线程结束或它的最后一个线程组销毁。
     *
     * @return  <code>true</code> if this thread group is a daemon thread group;
     *          <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public final boolean isDaemon() {
        return daemon;
    }

    /**
     * Tests if this thread group has been destroyed.
     *
     * 检测这个线程组是否被销毁。
     *
     * @return  true if this object is destroyed
     * @since   JDK1.1
     */
    public synchronized boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Changes the daemon status of this thread group.
     *
     * 改变这个线程组的守护状态。
     *
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     * <p>
     * A daemon thread group is automatically destroyed when its last
     * thread is stopped or its last thread group is destroyed.
     *
     * 首先，这个线程组的没有参数的checkAccess方法被执行，这可能导致安全异常。
     * 一个守护线程组被自动地销毁，当它的最后一个线程结束或它的最后一个线程组销毁。
     *
     * @param      daemon   if <code>true</code>, marks this thread group as
     *                      a daemon thread group; otherwise, marks this
     *                      thread group as normal.
     * @exception  SecurityException  if the current thread cannot modify
     *               this thread group.
     * @see        java.lang.SecurityException
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      JDK1.0
     */
    public final void setDaemon(boolean daemon) {
        checkAccess();
        this.daemon = daemon;
    }

    /**
     * Sets the maximum priority of the group. Threads in the thread
     * group that already have a higher priority are not affected.
     *
     * 设置这个线程组的最大优先级。这个线程组里面的线程的优先级比这个高也不受影响。
     *
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     * <p>
     * If the <code>pri</code> argument is less than
     * {@link Thread#MIN_PRIORITY} or greater than
     * {@link Thread#MAX_PRIORITY}, the maximum priority of the group
     * remains unchanged.
     *
     * 首先，这个线程组的没有参数的checkAccess方法被执行，这可能导致安全异常。
     * 如果这优先级参数比MIN_PRIORITY小或者比MAX_PRIORITY大，这个线程组的最
     * 大优先级不会改变。
     *
     * <p>
     * Otherwise, the priority of this ThreadGroup object is set to the
     * smaller of the specified <code>pri</code> and the maximum permitted
     * priority of the parent of this thread group. (If this thread group
     * is the system thread group, which has no parent, then its maximum
     * priority is simply set to <code>pri</code>.) Then this method is
     * called recursively, with <code>pri</code> as its argument, for
     * every thread group that belongs to this thread group.
     *
     * 否则，这个线程组的优先级被设置成指定的值和这个线程组的父亲允许的最大值中的最小的一个。
     * (如果这个线程组是一个系统线程组，这样就没有父亲，那么它的最大优先级就简单地设置成指定的
     * 优先级)，然后这个方法被递归地调用，带着pri参数，为每一个属于这个线程组的组线组。
     *
     * @param      pri   the new priority of the thread group.
     * @exception  SecurityException  if the current thread cannot modify
     *               this thread group.
     * @see        #getMaxPriority
     * @see        java.lang.SecurityException
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      JDK1.0
     */
    public final void setMaxPriority(int pri) {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            checkAccess();
            if (pri < Thread.MIN_PRIORITY || pri > Thread.MAX_PRIORITY) {
                return;
            }
            maxPriority = (parent != null) ? Math.min(pri, parent.maxPriority) : pri;
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0 ; i < ngroupsSnapshot ; i++) {
            groupsSnapshot[i].setMaxPriority(pri);
        }
    }

    /**
     * Tests if this thread group is either the thread group
     * argument or one of its ancestor thread groups.
     *
     * 检测这个线程组是否是线程组参数或是这个线程组的祖先
     *
     * @param   g   a thread group.
     * @return  <code>true</code> if this thread group is the thread group
     *          argument or one of its ancestor thread groups;
     *          <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public final boolean parentOf(ThreadGroup g) {
        for (; g != null ; g = g.parent) {
            if (g == this) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the currently running thread has permission to
     * modify this thread group.
     *
     * 检查是否当前运行的线程被允许修改这个线程组。
     *
     * <p>
     * If there is a security manager, its <code>checkAccess</code> method
     * is called with this thread group as its argument. This may result
     * in throwing a <code>SecurityException</code>.
     *
     * 如果安全管理器存在，带着线程组的参数的checkAccess方法被调用。这可能导致抛出
     * SecurityException异常。
     *
     * @exception  SecurityException  if the current thread is not allowed to
     *               access this thread group.
     * @see        java.lang.SecurityManager#checkAccess(java.lang.ThreadGroup)
     * @since      JDK1.0
     */
    public final void checkAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkAccess(this);
        }
    }

    /**
     * Returns an estimate of the number of active threads in this thread
     * group and its subgroups. Recursively iterates over all subgroups in
     * this thread group.
     *
     * <p> The value returned is only an estimate because the number of
     * threads may change dynamically while this method traverses internal
     * data structures, and might be affected by the presence of certain
     * system threads. This method is intended primarily for debugging
     * and monitoring purposes.
     *
     * 返回一个这个线程组和子线程组中活跃的线程估算值。递归遍历这个线程组中所有的子线程组。
     *
     * 返回值只是一个估算值，因为线程数可能动态地改变，当这个方法遍历内部数据结构的时候，
     * 并且可能受系统线程存在的影响。这个方法主要用来调试和监控目的。
     *
     * @return  an estimate of the number of active threads in this thread
     *          group and in any other thread group that has this thread
     *          group as an ancestor
     *
     * @since   JDK1.0
     */
    public int activeCount() {
        int result;
        // Snapshot sub-group data so we don't hold this lock
        // while our children are computing.
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            if (destroyed) {
                return 0;
            }
            result = nthreads;
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0 ; i < ngroupsSnapshot ; i++) {
            result += groupsSnapshot[i].activeCount();
        }
        return result;
    }

    /**
     * Copies into the specified array every active thread in this
     * thread group and its subgroups.
     *
     * <p> An invocation of this method behaves in exactly the same
     * way as the invocation
     *
     * <blockquote>
     * {@linkplain #enumerate(Thread[], boolean) enumerate}{@code (list, true)}
     * </blockquote>
     *
     * 把这个线程组和子线程组的所有活跃线程拷贝到指定的数组中。
     *
     * @param  list
     *         an array into which to put the list of threads
     *
     * @return  the number of threads put into the array
     *
     * @throws  SecurityException
     *          if {@linkplain #checkAccess checkAccess} determines that
     *          the current thread cannot access this thread group
     *
     * @since   JDK1.0
     */
    public int enumerate(Thread list[]) {
        checkAccess();
        return enumerate(list, 0, true);
    }

    /**
     * Copies into the specified array every active thread in this
     * thread group. If {@code recurse} is {@code true},
     * this method recursively enumerates all subgroups of this
     * thread group and references to every active thread in these
     * subgroups are also included. If the array is too short to
     * hold all the threads, the extra threads are silently ignored.
     *
     * 把这个线程组的所有活跃的线程拷贝到指定的数组中。
     * 如果recurse是true,这个方法递归地拷贝这个线程组的子线程组，包括子线程组的每一个
     * 活跃线程的引用。
     * 如果数组不小而不能放下所胡的线程，那么多出的线程将被忽略。
     *
     * <p> An application might use the {@linkplain #activeCount activeCount}
     * method to get an estimate of how big the array should be, however
     * <i>if the array is too short to hold all the threads, the extra threads
     * are silently ignored.</i>  If it is critical to obtain every active
     * thread in this thread group, the caller should verify that the returned
     * int value is strictly less than the length of {@code list}.
     *
     * 一个应该程序可以用activeCount方法来获取这个数据应该多大的大概值，然而如果
     * 数组太短而不能保存所有的线程，多余的线程被忽略。如果获取当前线程和子线程
     * 的每一个线程是重要的，调用者应该验证返回值比数组长度小。
     *
     * <p> Due to the inherent race condition in this method, it is recommended
     * that the method only be used for debugging and monitoring purposes.
     *
     * 因为这个方法天生地竞争条件，建议这个方法仅仅用来调试和监控目的。
     *
     * @param  list
     *         an array into which to put the list of threads
     *
     * @param  recurse
     *         if {@code true}, recursively enumerate all subgroups of this
     *         thread group
     *
     * @return  the number of threads put into the array
     *
     * @throws  SecurityException
     *          if {@linkplain #checkAccess checkAccess} determines that
     *          the current thread cannot access this thread group
     *
     * @since   JDK1.0
     */
    public int enumerate(Thread list[], boolean recurse) {
        checkAccess();
        return enumerate(list, 0, recurse);
    }

    private int enumerate(Thread list[], int n, boolean recurse) {
        int ngroupsSnapshot = 0;
        ThreadGroup[] groupsSnapshot = null;
        synchronized (this) {
            if (destroyed) {
                return 0;
            }
            int nt = nthreads;
            if (nt > list.length - n) {
                nt = list.length - n;
            }
            for (int i = 0; i < nt; i++) {
                if (threads[i].isAlive()) {
                    list[n++] = threads[i];
                }
            }
            if (recurse) {
                ngroupsSnapshot = ngroups;
                if (groups != null) {
                    groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
                } else {
                    groupsSnapshot = null;
                }
            }
        }
        if (recurse) {
            for (int i = 0 ; i < ngroupsSnapshot ; i++) {
                n = groupsSnapshot[i].enumerate(list, n, true);
            }
        }
        return n;
    }

    /**
     * Returns an estimate of the number of active groups in this
     * thread group and its subgroups. Recursively iterates over
     * all subgroups in this thread group.
     *
     * <p> The value returned is only an estimate because the number of
     * thread groups may change dynamically while this method traverses
     * internal data structures. This method is intended primarily for
     * debugging and monitoring purposes.
     *
     * 返回当前线程所在的组和它的子组里活跃线程的一个估算值，遍历当前线程
     * 所有的子组。
     * 返回的值仅仅是一个估算值，因为线程数量可能动态地改变，当这个方法
     * 遍历内部数据结构的时候，并且可能受一些系统线程存在的影响。这个方法
     * 主要用来高度和监控目的。
     *
     * @return  the number of active thread groups with this thread group as
     *          an ancestor
     *
     * @since   JDK1.0
     */
    public int activeGroupCount() {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            if (destroyed) {
                return 0;
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        int n = ngroupsSnapshot;
        for (int i = 0 ; i < ngroupsSnapshot ; i++) {
            n += groupsSnapshot[i].activeGroupCount();
        }
        return n;
    }

    /**
     * Copies into the specified array references to every active
     * subgroup in this thread group and its subgroups.
     *
     * <p> An invocation of this method behaves in exactly the same
     * way as the invocation
     *
     * <blockquote>
     * {@linkplain #enumerate(ThreadGroup[], boolean) enumerate}{@code (list, true)}
     * </blockquote>
     *
     * @param  list
     *         an array into which to put the list of thread groups
     *
     * @return  the number of thread groups put into the array
     *
     * @throws  SecurityException
     *          if {@linkplain #checkAccess checkAccess} determines that
     *          the current thread cannot access this thread group
     *
     * @since   JDK1.0
     */
    public int enumerate(ThreadGroup list[]) {
        checkAccess();
        return enumerate(list, 0, true);
    }

    /**
     * Copies into the specified array references to every active
     * subgroup in this thread group. If {@code recurse} is
     * {@code true}, this method recursively enumerates all subgroups of this
     * thread group and references to every active thread group in these
     * subgroups are also included.
     *
     * <p> An application might use the
     * {@linkplain #activeGroupCount activeGroupCount} method to
     * get an estimate of how big the array should be, however <i>if the
     * array is too short to hold all the thread groups, the extra thread
     * groups are silently ignored.</i>  If it is critical to obtain every
     * active subgroup in this thread group, the caller should verify that
     * the returned int value is strictly less than the length of
     * {@code list}.
     *
     * <p> Due to the inherent race condition in this method, it is recommended
     * that the method only be used for debugging and monitoring purposes.
     *
     * @param  list
     *         an array into which to put the list of thread groups
     *
     * @param  recurse
     *         if {@code true}, recursively enumerate all subgroups
     *
     * @return  the number of thread groups put into the array
     *
     * @throws  SecurityException
     *          if {@linkplain #checkAccess checkAccess} determines that
     *          the current thread cannot access this thread group
     *
     * @since   JDK1.0
     */
    public int enumerate(ThreadGroup list[], boolean recurse) {
        checkAccess();
        return enumerate(list, 0, recurse);
    }

    private int enumerate(ThreadGroup list[], int n, boolean recurse) {
        int ngroupsSnapshot = 0;
        ThreadGroup[] groupsSnapshot = null;
        synchronized (this) {
            if (destroyed) {
                return 0;
            }
            int ng = ngroups;
            if (ng > list.length - n) {
                ng = list.length - n;
            }
            if (ng > 0) {
                System.arraycopy(groups, 0, list, n, ng);
                n += ng;
            }
            if (recurse) {
                ngroupsSnapshot = ngroups;
                if (groups != null) {
                    groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
                } else {
                    groupsSnapshot = null;
                }
            }
        }
        if (recurse) {
            for (int i = 0 ; i < ngroupsSnapshot ; i++) {
                n = groupsSnapshot[i].enumerate(list, n, true);
            }
        }
        return n;
    }

    /**
     * Stops all threads in this thread group.
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     * <p>
     * This method then calls the <code>stop</code> method on all the
     * threads in this thread group and in all of its subgroups.
     *
     * 停止这个线程组中的所有线程。
     * 首先，这个线程组的没有参数的checkAccess方法被执行，这可能导致安全异常。
     * 然后这个方法调用这个线程组里面的所有线程的stop方法。
     *
     * @exception  SecurityException  if the current thread is not allowed
     *               to access this thread group or any of the threads in
     *               the thread group.
     * @see        java.lang.SecurityException
     * @see        java.lang.Thread#stop()
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      JDK1.0
     * @deprecated    This method is inherently unsafe.  See
     *     {@link Thread#stop} for details.
     */
    @Deprecated
    public final void stop() {
        if (stopOrSuspend(false))
            Thread.currentThread().stop();
    }

    /**
     * Interrupts all threads in this thread group.
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     * <p>
     * This method then calls the <code>interrupt</code> method on all the
     * threads in this thread group and in all of its subgroups.
     *
     * 中断这个线程组里所有的线程。
     * 首先，这个线程组的没有参数的checkAccess方法被执行，这可能导致安全异常。
     * 然后这个方法调用这个线程组里面的所有线程的interrupt方法。
     *
     * @exception  SecurityException  if the current thread is not allowed
     *               to access this thread group or any of the threads in
     *               the thread group.
     * @see        java.lang.Thread#interrupt()
     * @see        java.lang.SecurityException
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      1.2
     */
    public final void interrupt() {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            checkAccess();
            for (int i = 0 ; i < nthreads ; i++) {
                threads[i].interrupt();
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0 ; i < ngroupsSnapshot ; i++) {
            groupsSnapshot[i].interrupt();
        }
    }

    /**
     * Suspends all threads in this thread group.
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     * <p>
     * This method then calls the <code>suspend</code> method on all the
     * threads in this thread group and in all of its subgroups.
     *
     * 暂停这个线程组里所有的线程。
     * 首先，这个线程组的没有参数的checkAccess方法被执行，这可能导致安全异常。
     * 然后这个方法调用这个线程组里面的所有线程的suspend方法。
     *
     * @exception  SecurityException  if the current thread is not allowed
     *               to access this thread group or any of the threads in
     *               the thread group.
     * @see        java.lang.Thread#suspend()
     * @see        java.lang.SecurityException
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      JDK1.0
     * @deprecated    This method is inherently deadlock-prone.  See
     *     {@link Thread#suspend} for details.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public final void suspend() {
        if (stopOrSuspend(true))
            Thread.currentThread().suspend();
    }

    /**
     * Helper method: recursively stops or suspends (as directed by the
     * boolean argument) all of the threads in this thread group and its
     * subgroups, except the current thread.  This method returns true
     * if (and only if) the current thread is found to be in this thread
     * group or one of its subgroups.
     *
     * 帮助方法:递归停止或暂停这个线程组里所有的线程并且它的子线程组，除了当前线程。
     * 这个方法返回true如果当前线程在这个线程组里或子线程组里。
     */
    @SuppressWarnings("deprecation")
    private boolean stopOrSuspend(boolean suspend) {
        boolean suicide = false;
        Thread us = Thread.currentThread();
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot = null;
        synchronized (this) {
            checkAccess();
            for (int i = 0 ; i < nthreads ; i++) {
                if (threads[i]==us)
                    suicide = true;
                else if (suspend)
                    threads[i].suspend();
                else
                    threads[i].stop();
            }

            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            }
        }
        for (int i = 0 ; i < ngroupsSnapshot ; i++)
            suicide = groupsSnapshot[i].stopOrSuspend(suspend) || suicide;

        return suicide;
    }

    /**
     * Resumes all threads in this thread group.
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     * <p>
     * This method then calls the <code>resume</code> method on all the
     * threads in this thread group and in all of its sub groups.
     *
     * 恢复这个线程组里所有的线程。
     * 首先，这个线程组的没有参数的checkAccess方法被执行，这可能导致安全异常。
     * 然后这个方法调用这个线程组里面的所有线程的resume方法。
     *
     * @exception  SecurityException  if the current thread is not allowed to
     *               access this thread group or any of the threads in the
     *               thread group.
     * @see        java.lang.SecurityException
     * @see        java.lang.Thread#resume()
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      JDK1.0
     * @deprecated    This method is used solely in conjunction with
     *      <tt>Thread.suspend</tt> and <tt>ThreadGroup.suspend</tt>,
     *       both of which have been deprecated, as they are inherently
     *       deadlock-prone.  See {@link Thread#suspend} for details.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public final void resume() {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            checkAccess();
            for (int i = 0 ; i < nthreads ; i++) {
                threads[i].resume();
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0 ; i < ngroupsSnapshot ; i++) {
            groupsSnapshot[i].resume();
        }
    }

    /**
     * Destroys this thread group and all of its subgroups. This thread
     * group must be empty, indicating that all threads that had been in
     * this thread group have since stopped.
     * <p>
     * First, the <code>checkAccess</code> method of this thread group is
     * called with no arguments; this may result in a security exception.
     *
     * 销毁这个线程组和所有子线程组。这个线程组必需是空的，表示这个线程组里的线程已经被停止了。
     * 首先，这个线程组的没有参数的checkAccess方法被执行，这可能导致安全异常。
     *
     * @exception  IllegalThreadStateException  if the thread group is not
     *               empty or if the thread group has already been destroyed.
     * @exception  SecurityException  if the current thread cannot modify this
     *               thread group.
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      JDK1.0
     */
    public final void destroy() {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            checkAccess();
            if (destroyed || (nthreads > 0)) {
                throw new IllegalThreadStateException();
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
            if (parent != null) {
                destroyed = true;
                ngroups = 0;
                groups = null;
                nthreads = 0;
                threads = null;
            }
        }
        for (int i = 0 ; i < ngroupsSnapshot ; i += 1) {
            groupsSnapshot[i].destroy();
        }
        if (parent != null) {
            parent.remove(this);
        }
    }

    /**
     * Adds the specified Thread group to this group.
     *
     * 把指定的线程组加入到这个线程组中。
     *
     * @param g the specified Thread group to be added
     * @exception IllegalThreadStateException If the Thread group has been destroyed.
     */
    private final void add(ThreadGroup g){
        synchronized (this) {
            if (destroyed) {
                throw new IllegalThreadStateException();
            }
            if (groups == null) {
                groups = new ThreadGroup[4];
            } else if (ngroups == groups.length) {
                groups = Arrays.copyOf(groups, ngroups * 2);
            }
            groups[ngroups] = g;

            // This is done last so it doesn't matter in case the
            // thread is killed
            ngroups++;
        }
    }

    /**
     * Removes the specified Thread group from this group.
     *
     * 从这个线程组中移除指定的线程组。
     *
     * @param g the Thread group to be removed
     * @return if this Thread has already been destroyed.
     */
    private void remove(ThreadGroup g) {
        synchronized (this) {
            if (destroyed) {
                return;
            }
            for (int i = 0 ; i < ngroups ; i++) {
                if (groups[i] == g) {
                    ngroups -= 1;
                    System.arraycopy(groups, i + 1, groups, i, ngroups - i);
                    // Zap dangling reference to the dead group so that
                    // the garbage collector will collect it.
                    groups[ngroups] = null;
                    break;
                }
            }
            if (nthreads == 0) {
                notifyAll();
            }
            if (daemon && (nthreads == 0) &&
                (nUnstartedThreads == 0) && (ngroups == 0))
            {
                destroy();
            }
        }
    }


    /**
     * Increments the count of unstarted threads in the thread group.
     * Unstarted threads are not added to the thread group so that they
     * can be collected if they are never started, but they must be
     * counted so that daemon thread groups with unstarted threads in
     * them are not destroyed.
     * 增加这个线程组中没有开始的线程数量。没有开始的线程没有被加入到线程组中，所以他们
     * 可以被收集如果他们从来没有开始，但是他们必需被通计，所以带着没有开始线程的线程组
     * 没有被销毁。
     */
    void addUnstarted() {
        synchronized(this) {
            if (destroyed) {
                throw new IllegalThreadStateException();
            }
            nUnstartedThreads++;
        }
    }

    /**
     * Adds the specified thread to this thread group.
     *
     * 把指定的线程加入到这个线程组里面
     *
     * <p> Note: This method is called from both library code
     * and the Virtual Machine. It is called from VM to add
     * certain system threads to the system thread group.
     *
     * 注意:这个方法被库代码和虚拟机调用。它被虚拟机调用用来把一些系统
     * 线程加入到系统线程组。
     *
     * @param  t
     *         the Thread to be added
     *
     * @throws  IllegalThreadStateException
     *          if the Thread group has been destroyed
     */
    void add(Thread t) {
        synchronized (this) {
            if (destroyed) {
                throw new IllegalThreadStateException();
            }
            if (threads == null) {
                threads = new Thread[4];
            } else if (nthreads == threads.length) {
                threads = Arrays.copyOf(threads, nthreads * 2);
            }
            threads[nthreads] = t;

            // This is done last so it doesn't matter in case the
            // thread is killed
            nthreads++;

            // The thread is now a fully fledged member of the group, even
            // though it may, or may not, have been started yet. It will prevent
            // the group from being destroyed so the unstarted Threads count is
            // decremented.
            nUnstartedThreads--;
        }
    }

    /**
     * Notifies the group that the thread {@code t} has failed
     * an attempt to start.
     *
     * 通过线程组这个线程启动失败。
     *
     * <p> The state of this thread group is rolled back as if the
     * attempt to start the thread has never occurred. The thread is again
     * considered an unstarted member of the thread group, and a subsequent
     * attempt to start the thread is permitted.
     *
     * 这个线程组的状态被回滚，如果不再试图启动一个线程。这个线程再次被当成线程组没有启动的线程，
     * 并且允许以后启动线程。
     *
     * @param  t
     *         the Thread whose start method was invoked
     */
    void threadStartFailed(Thread t) {
        synchronized(this) {
            remove(t);
            nUnstartedThreads++;
        }
    }

    /**
     * Notifies the group that the thread {@code t} has terminated.
     *
     * 通知这个线程组这个线程已经被终止了。
     *
     * <p> Destroy the group if all of the following conditions are
     * true: this is a daemon thread group; there are no more alive
     * or unstarted threads in the group; there are no subgroups in
     * this thread group.
     *
     * 销毁这个组如果下面的条件都成立:这个一个守护线程，没有活跃的或没启动的线程在这个
     * 组里面，没有子线程组在这个线程组里。
     *
     * @param  t
     *         the Thread that has terminated
     */
    void threadTerminated(Thread t) {
        synchronized (this) {
            remove(t);

            if (nthreads == 0) {
                notifyAll();
            }
            if (daemon && (nthreads == 0) &&
                (nUnstartedThreads == 0) && (ngroups == 0))
            {
                destroy();
            }
        }
    }

    /**
     * Removes the specified Thread from this group. Invoking this method
     * on a thread group that has been destroyed has no effect.
     *
     * 从线程组里移除指定的线程，从已经销毁的线程组中调用这个方法没有任何效果;
     *
     * @param  t
     *         the Thread to be removed
     */
    private void remove(Thread t) {
        synchronized (this) {
            if (destroyed) {
                return;
            }
            for (int i = 0 ; i < nthreads ; i++) {
                if (threads[i] == t) {
                    System.arraycopy(threads, i + 1, threads, i, --nthreads - i);
                    // Zap dangling reference to the dead thread so that
                    // the garbage collector will collect it.
                    threads[nthreads] = null;
                    break;
                }
            }
        }
    }

    /**
     * Prints information about this thread group to the standard
     * output. This method is useful only for debugging.
     *
     * 打印关于这个线程组的信息到标准输出中，这个方法对调试很有用。
     *
     * @since   JDK1.0
     */
    public void list() {
        list(System.out, 0);
    }
    void list(PrintStream out, int indent) {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            for (int j = 0 ; j < indent ; j++) {
                out.print(" ");
            }
            out.println(this);
            indent += 4;
            for (int i = 0 ; i < nthreads ; i++) {
                for (int j = 0 ; j < indent ; j++) {
                    out.print(" ");
                }
                out.println(threads[i]);
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0 ; i < ngroupsSnapshot ; i++) {
            groupsSnapshot[i].list(out, indent);
        }
    }

    /**
     * Called by the Java Virtual Machine when a thread in this
     * thread group stops because of an uncaught exception, and the thread
     * does not have a specific {@link Thread.UncaughtExceptionHandler}
     * installed.
     *
     * 被虚拟机调用当线程组的线程因为未捕获的异常而停止，并且线程没有设置 Thread.UncaughtExceptionHandler
     * 的时候。
     *
     * <p>
     * The <code>uncaughtException</code> method of
     * <code>ThreadGroup</code> does the following:
     * <ul>
     * 线程组的uncaughtException方法做了如下的事情：
     * 如果线程组有一个父线程组，父线程组的uncaughtException方法被调用，带着相同的
     * 两个参数。
     * 否则，这个方法调用Thread.getDefaultUncaughtExceptionHandler方法看是不是
     * 有默认的未捕获异常拦截器，如果有它的uncaughtException被调用，带着两个相同的方法。
     * 否则，这个方法判断Throwable参数是不是ThreadDeath实例，如果是，不做任何操作。
     * 否则从Thread.getName方法返回的包含线程名的信息和栈回溯，使用Throwable的printStackTrace
     * 方法，被打印到系统的标准错误流。
     * <li>If this thread group has a parent thread group, the
     *     <code>uncaughtException</code> method of that parent is called
     *     with the same two arguments.
     * <li>Otherwise, this method checks to see if there is a
     *     {@linkplain Thread#getDefaultUncaughtExceptionHandler default
     *     uncaught exception handler} installed, and if so, its
     *     <code>uncaughtException</code> method is called with the same
     *     two arguments.
     * <li>Otherwise, this method determines if the <code>Throwable</code>
     *     argument is an instance of {@link ThreadDeath}. If so, nothing
     *     special is done. Otherwise, a message containing the
     *     thread's name, as returned from the thread's {@link
     *     Thread#getName getName} method, and a stack backtrace,
     *     using the <code>Throwable</code>'s {@link
     *     Throwable#printStackTrace printStackTrace} method, is
     *     printed to the {@linkplain System#err standard error stream}.
     * </ul>
     * <p>
     * Applications can override this method in subclasses of
     * <code>ThreadGroup</code> to provide alternative handling of
     * uncaught exceptions.
     *
     * ThreadGroup的子类可以重写这个方法来提供其它的未捕获异常处理方法。
     *
     * @param   t   the thread that is about to exit.
     * @param   e   the uncaught exception.
     * @since   JDK1.0
     */
    public void uncaughtException(Thread t, Throwable e) {
        if (parent != null) {
            parent.uncaughtException(t, e);
        } else {
            Thread.UncaughtExceptionHandler ueh =
                Thread.getDefaultUncaughtExceptionHandler();
            if (ueh != null) {
                ueh.uncaughtException(t, e);
            } else if (!(e instanceof ThreadDeath)) {
                System.err.print("Exception in thread \""
                                 + t.getName() + "\" ");
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * Used by VM to control lowmem implicit suspension.
     *
     * @param b boolean to allow or disallow suspension
     * @return true on success
     * @since   JDK1.1
     * @deprecated The definition of this call depends on {@link #suspend},
     *             which is deprecated.  Further, the behavior of this call
     *             was never specified.
     */
    @Deprecated
    public boolean allowThreadSuspension(boolean b) {
        this.vmAllowSuspension = b;
        if (!b) {
            VM.unsuspendSomeThreads();
        }
        return true;
    }

    /**
     * Returns a string representation of this Thread group.
     *
     * @return  a string representation of this thread group.
     * @since   JDK1.0
     */
    public String toString() {
        return getClass().getName() + "[name=" + getName() + ",maxpri=" + maxPriority + "]";
    }
}
