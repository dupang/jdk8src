/*
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

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import sun.misc.Unsafe;

/**
 * Basic thread blocking primitives for creating locks and other
 * synchronization classes.
 *
 * 创建锁和其它同步类的基本线程阻塞原语。
 *
 * <p>This class associates, with each thread that uses it, a permit
 * (in the sense of the {@link java.util.concurrent.Semaphore
 * Semaphore} class). A call to {@code park} will return immediately
 * if the permit is available, consuming it in the process; otherwise
 * it <em>may</em> block.  A call to {@code unpark} makes the permit
 * available, if it was not already available. (Unlike with Semaphores
 * though, permits do not accumulate. There is at most one.)
 *
 * 这个类关联每一个使用它的线程一个许可。调用part将立刻返回如果许可可用，在这个过程中
 * 消费它;否则它可能阻塞。调用unpark使许可可用，如果它还不可用。（可是不像Semaphores
 * ,许可不累加。最多一个）
 *
 * <p>Methods {@code park} and {@code unpark} provide efficient
 * means of blocking and unblocking threads that do not encounter the
 * problems that cause the deprecated methods {@code Thread.suspend}
 * and {@code Thread.resume} to be unusable for such purposes: Races
 * between one thread invoking {@code park} and another thread trying
 * to {@code unpark} it will preserve liveness, due to the
 * permit. Additionally, {@code park} will return if the caller's
 * thread was interrupted, and timeout versions are supported. The
 * {@code park} method may also return at any other time, for "no
 * reason", so in general must be invoked within a loop that rechecks
 * conditions upon return. In this sense {@code park} serves as an
 * optimization of a "busy wait" that does not waste as much time
 * spinning, but must be paired with an {@code unpark} to be
 * effective.
 *
 * 由于许可park和unpark方法提供有效率的阻塞和解锁线程的方法，并且不会遇到引起过时的方法
 * Thread.suspend和Thread.resume为这样目的不可用的问题，一个调用park的线程和另一个调用
 * unpark将它全保持活性的线程之间的竞争。另外，park将会返回，如果调用的线程被中断，并且支持
 * 超时的版本。park方法也可能在任何时间返回，没有"任何原因"，所以通常必须在一个循环中调用
 * 并在返回时重新检测条件。在这个意义上来说park就像一个"忙等待"的优化而不会浪费太多时间自旋，
 * 但是为了有效必须和unpark配对使用。
 *
 * <p>The three forms of {@code park} each also support a
 * {@code blocker} object parameter. This object is recorded while
 * the thread is blocked to permit monitoring and diagnostic tools to
 * identify the reasons that threads are blocked. (Such tools may
 * access blockers using method {@link #getBlocker(Thread)}.)
 * The use of these forms rather than the original forms without this
 * parameter is strongly encouraged. The normal argument to supply as
 * a {@code blocker} within a lock implementation is {@code this}.
 *
 * 三种形式的park每一个都支持一个blocker对象参数。这个对象当线程获取监听被
 * 阻塞时被记录下来，并且分析工具可以使用它分析线程被阻塞的原因。(这些工具
 * 可以通过 方法getBlocker(Thread) 访问blocker).强烈建议使用这三种形式的方法，
 * 而不使用没有参数的原始形式。Lock实现里提供作为blocker的普通参数是this.
 *
 * <p>These methods are designed to be used as tools for creating
 * higher-level synchronization utilities, and are not in themselves
 * useful for most concurrency control applications.  The {@code park}
 * method is designed for use only in constructions of the form:
 *
 *  <pre> {@code
 * while (!canProceed()) { ... LockSupport.park(this); }}</pre>
 *
 * where neither {@code canProceed} nor any other actions prior to the
 * call to {@code park} entail locking or blocking.  Because only one
 * permit is associated with each thread, any intermediary uses of
 * {@code park} could interfere with its intended effects.
 *
 * 这些方法被设计用来为工具创建高级别的同步工具，并且不是用来并发控制应用。
 * park方法被设计只使用下面的形式
 *
 * 没有在canProceed或其它任何方法之前调用park而获取锁或阻塞。
 * 因为只一个许可跟一个线程关联，任何居间地使用park可能影响它的最终效果。
 *
 * <p><b>Sample Usage.</b> Here is a sketch of a first-in-first-out
 * non-reentrant lock class:
 *  <pre> {@code
 * class FIFOMutex {
 *   private final AtomicBoolean locked = new AtomicBoolean(false);
 *   private final Queue<Thread> waiters
 *     = new ConcurrentLinkedQueue<Thread>();
 *
 *   public void lock() {
 *     boolean wasInterrupted = false;
 *     Thread current = Thread.currentThread();
 *     waiters.add(current);
 *
 *     // Block while not first in queue or cannot acquire lock
 *     while (waiters.peek() != current ||
 *            !locked.compareAndSet(false, true)) {
 *       LockSupport.park(this);
 *       if (Thread.interrupted()) // ignore interrupts while waiting
 *         wasInterrupted = true;
 *     }
 *
 *     waiters.remove();
 *     if (wasInterrupted)          // reassert interrupt status on exit
 *       current.interrupt();
 *   }
 *
 *   public void unlock() {
 *     locked.set(false);
 *     LockSupport.unpark(waiters.peek());
 *   }
 * }}</pre>
 */
public class LockSupport {
    private LockSupport() {} // Cannot be instantiated.

    private static void setBlocker(Thread t, Object arg) {
        // Even though volatile, hotspot doesn't need a write barrier here.
        UNSAFE.putObject(t, parkBlockerOffset, arg);
    }

    /**
     * Makes available the permit for the given thread, if it
     * was not already available.  If the thread was blocked on
     * {@code park} then it will unblock.  Otherwise, its next call
     * to {@code park} is guaranteed not to block. This operation
     * is not guaranteed to have any effect at all if the given
     * thread has not been started.
     *
     * 使用许可可用对给定的线程，如果它还不可用。如果因为park被阻塞,
     * 那么它将解锁。否则，它下一次调用park，保证不会阻塞。如果给定
     * 的线程还没有开始，那么这个操作将不会有什么效果。
     *
     * @param thread the thread to unpark, or {@code null}, in which case
     *        this operation has no effect
     */
    public static void unpark(Thread thread) {
        if (thread != null)
            UNSAFE.unpark(thread);
    }

    /**
     * Disables the current thread for thread scheduling purposes unless the
     * permit is available.
     *
     * 使当前线程对线程调度来说不可用，除非许可可用。
     * <p>If the permit is available then it is consumed and the call returns
     * immediately; otherwise
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * 如果许可可用，那么它被消费并且方法调用立刻返回;不然当前线程对线程调度变得
     * 不可用，休眠直到下面其中的事情发生。
     *
     * 其它线程调用unpark方法并且以当前线程作为参数。或者其它线程中断了当前线程
     * 或者调用虚假地(没有任何原因)返回
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread upon return.
     *
     * 这个方法没有报告这些中的那一个导致方法返回。调用都应该首先重新检查
     * 导致线程阻塞的条件。调用者也可以确定，例如，返回时线程的中断状态。
     *
     * @param blocker the synchronization object responsible for this
     *        thread parking
     * @since 1.6
     */
    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(false, 0L);
        setBlocker(t, null);
    }

    /**
     * Disables the current thread for thread scheduling purposes, for up to
     * the specified waiting time, unless the permit is available.
     *
     * 使当前线程到了指定的等待时间后对线程调度不可用，除非许可可用。
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     *  如果许可可用，那么它被消费并且方法立刻返回，否则当前线程变成不可用，
     *  对线程调度来说，并且休眠直到下面其中四种的一个发生。
     *
     *  其它线程调用了unpark并以当前线程为目标;或
     *  其它线程中断了当前线程，
     *  指定的等待时间用完，
     *  调用虚假地(没有任何原因)返回
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * 这个方法没有报告这些中的那一个导致方法返回。调用都应该首先重新检查
     * 导致线程阻塞的条件。调用者也可以确定，例如，返回时线程的中断状态。
     *
     * @param blocker the synchronization object responsible for this
     *        thread parking
     * @param nanos the maximum number of nanoseconds to wait
     * @since 1.6
     */
    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            UNSAFE.park(false, nanos);
            setBlocker(t, null);
        }
    }

    /**
     * Disables the current thread for thread scheduling purposes, until
     * the specified deadline, unless the permit is available.
     *
     * 使当前线程到了指定的最后时间对线程调度不可用，除非许可可用。
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread; or
     *
     * <li>The specified deadline passes; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * 如果许可可用，那么它被消费并且方法调用立刻返回;不然当前线程对线程调度变得
     * 不可用，休眠直到下面其中的事情发生。
     *
     * 其它线程调用unpark方法并且以当前线程作为参数。或者其它线程中断了当前线程
     * 或者调用虚假地(没有任何原因)返回
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the current time
     * upon return.
     *
     * 这个方法没有报告这些中的那一个导致方法返回。调用都应该首先重新检查
     * 导致线程阻塞的条件。调用者也可以确定，例如，返回时线程的中断状态。
     *
     * @param blocker the synchronization object responsible for this
     *        thread parking
     * @param deadline the absolute time, in milliseconds from the Epoch,
     *        to wait until
     * @since 1.6
     */
    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(true, deadline);
        setBlocker(t, null);
    }

    /**
     * Returns the blocker object supplied to the most recent
     * invocation of a park method that has not yet unblocked, or null
     * if not blocked.  The value returned is just a momentary
     * snapshot -- the thread may have since unblocked or blocked on a
     * different blocker object.
     *
     * 返回blocker对象提供给最近调用的park方法或null如果没有被阻塞，返回值仅仅是一个
     * 瞬间的快照。线程也许已经解锁或阻塞在不同的阻塞对象上。
     *
     * @param t the thread
     * @return the blocker
     * @throws NullPointerException if argument is null
     * @since 1.6
     */
    public static Object getBlocker(Thread t) {
        if (t == null)
            throw new NullPointerException();
        return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
    }

    /**
     * Disables the current thread for thread scheduling purposes unless the
     * permit is available.
     *
     * 使当前线程到了对线程调度不可用，除非许可可用。
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of three
     * things happens:
     *
     * <ul>
     *
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * *  如果许可可用，那么它被消费并且方法立刻返回，否则当前线程变成不可用，
     *  对线程调度来说，并且休眠直到下面其中四种的一个发生。
     *
     *  其它线程调用了unpark并以当前线程为目标;或
     *  其它线程中断了当前线程，
     *  调用虚假地(没有任何原因)返回
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread upon return.
     *
     * 这个方法没有报告这些中的那一个导致方法返回。调用都应该首先重新检查
     * 导致线程阻塞的条件。调用者也可以确定，例如，返回时线程的中断状态。
     *
     */
    public static void park() {
        UNSAFE.park(false, 0L);
    }

    /**
     * Disables the current thread for thread scheduling purposes, for up to
     * the specified waiting time, unless the permit is available.
     *
     * 使当前线程到了指定的等待时间后对线程调度不可用，除非许可可用。
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     *  如果许可可用，那么它被消费并且方法立刻返回，否则当前线程变成不可用，
     *  对线程调度来说，并且休眠直到下面其中四种的一个发生。
     *
     *  其它线程调用了unpark并以当前线程为目标;或
     *  其它线程中断了当前线程，
     *  指定的等待时间用完，
     *  调用虚假地(没有任何原因)返回
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * 这个方法没有报告这些中的那一个导致方法返回。调用都应该首先重新检查
     * 导致线程阻塞的条件。调用者也可以确定，例如，返回时线程的中断状态。
     *
     * @param nanos the maximum number of nanoseconds to wait
     */
    public static void parkNanos(long nanos) {
        if (nanos > 0)
            UNSAFE.park(false, nanos);
    }

    /**
     * Disables the current thread for thread scheduling purposes, until
     * the specified deadline, unless the permit is available.
     *
     * 使当前线程到了指定的最后时间对线程调度不可用，除非许可可用。
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified deadline passes; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * 如果许可可用，那么它被消费并且方法调用立刻返回;不然当前线程对线程调度变得
     * 不可用，休眠直到下面其中的事情发生。
     *
     * 其它线程调用unpark方法并且以当前线程作为参数。或者其它线程中断了当前线程
     * 或者调用虚假地(没有任何原因)返回
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the current time
     * upon return.
     *
     * 这个方法没有报告这些中的那一个导致方法返回。调用都应该首先重新检查
     * 导致线程阻塞的条件。调用者也可以确定，例如，返回时线程的中断状态。
     *
     * @param deadline the absolute time, in milliseconds from the Epoch,
     *        to wait until
     */
    public static void parkUntil(long deadline) {
        UNSAFE.park(true, deadline);
    }

    /**
     * Returns the pseudo-randomly initialized or updated secondary seed.
     * Copied from ThreadLocalRandom due to package access restrictions.
     *
     * 返回伪伪随机初始化或更新的次要种子
     * 从ThreadLocalRandom拷贝过来的，因为包访问限制。
     */
    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        if ((r = UNSAFE.getInt(t, SECONDARY)) != 0) {
            r ^= r << 13;   // xorshift
            r ^= r >>> 17;
            r ^= r << 5;
        }
        else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0)
            r = 1; // avoid zero
        UNSAFE.putInt(t, SECONDARY, r);
        return r;
    }

    // Hotspot implementation via intrinsics API
    private static final sun.misc.Unsafe UNSAFE;
    private static final long parkBlockerOffset;
    private static final long SEED;
    private static final long PROBE;
    private static final long SECONDARY;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            parkBlockerOffset = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("parkBlocker"));
            SEED = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSeed"));
            PROBE = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomProbe"));
            SECONDARY = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (Exception ex) { throw new Error(ex); }
    }

}
