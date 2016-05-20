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
import java.util.concurrent.TimeUnit;
import java.util.Date;

/**
 * {@code Condition} factors out the {@code Object} monitor
 * methods ({@link Object#wait() wait}, {@link Object#notify notify}
 * and {@link Object#notifyAll notifyAll}) into distinct objects to
 * give the effect of having multiple wait-sets per object, by
 * combining them with the use of arbitrary {@link Lock} implementations.
 * Where a {@code Lock} replaces the use of {@code synchronized} methods
 * and statements, a {@code Condition} replaces the use of the Object
 * monitor methods.
 *
 * Condition把Object监视方法wait(),notify(),notifyAll()分为不同的对象。
 * 通过和Lock实现的结合使用，使每一个对象具有可以持有多个等待集，
 * 在这里，Lock代替了同步方法和同步语句块的使用，Condition代替了对象监视方法。
 *
 * <p>Conditions (also known as <em>condition queues</em> or
 * <em>condition variables</em>) provide a means for one thread to
 * suspend execution (to &quot;wait&quot;) until notified by another
 * thread that some state condition may now be true.  Because access
 * to this shared state information occurs in different threads, it
 * must be protected, so a lock of some form is associated with the
 * condition. The key property that waiting for a condition provides
 * is that it <em>atomically</em> releases the associated lock and
 * suspends the current thread, just like {@code Object.wait}.
 *
 * Conditions(亦称为条件队列或者条件变量)提供一个方法，一个线程可能暂停执行直到
 * 被另一个一些状态条件也许现在是true的线程通知唤醒。因为访问这个共享的状态信息发生
 * 在不同的线程，这必须被保护，所以一些形式的锁跟条件关联。等待提供的条件的主要特性是
 * 它自动释放关联的锁并且暂停当前线程，就像Object.wait().
 *
 * <p>A {@code Condition} instance is intrinsically bound to a lock.
 * To obtain a {@code Condition} instance for a particular {@link Lock}
 * instance use its {@link Lock#newCondition newCondition()} method.
 *
 * 一个Condition实例本质上跟一个锁绑定。为了获取一个选定锁的Condition实例，Lock实例
 * 使用它的Lock.newCondition()方法。
 *
 * <p>As an example, suppose we have a bounded buffer which supports
 * {@code put} and {@code take} methods.  If a
 * {@code take} is attempted on an empty buffer, then the thread will block
 * until an item becomes available; if a {@code put} is attempted on a
 * full buffer, then the thread will block until a space becomes available.
 * We would like to keep waiting {@code put} threads and {@code take}
 * threads in separate wait-sets so that we can use the optimization of
 * only notifying a single thread at a time when items or spaces become
 * available in the buffer. This can be achieved using two
 * {@link Condition} instances.
 *
 * 例如，假如我们有一个有上限的缓存，支持put和take方法。如果试图take一个空的buffer，那么
 * 线程将会阻塞直到有元素可用。如果试图put一个满的buffer，那么线程将会被阻塞，直到有空闲空间可用。
 * 我们会使等待put和take的线程为不同的等待集合，所以我们可以一次只唤醒一个线程当元素或
 * 空间在buffer中变得可用，这可以使用两个Condition实例来实现。
 * <pre>
 * class BoundedBuffer {
 *   <b>final Lock lock = new ReentrantLock();</b>
 *   final Condition notFull  = <b>lock.newCondition(); </b>
 *   final Condition notEmpty = <b>lock.newCondition(); </b>
 *
 *   final Object[] items = new Object[100];
 *   int putptr, takeptr, count;
 *
 *   public void put(Object x) throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == items.length)
 *         <b>notFull.await();</b>
 *       items[putptr] = x;
 *       if (++putptr == items.length) putptr = 0;
 *       ++count;
 *       <b>notEmpty.signal();</b>
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 *
 *   public Object take() throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == 0)
 *         <b>notEmpty.await();</b>
 *       Object x = items[takeptr];
 *       if (++takeptr == items.length) takeptr = 0;
 *       --count;
 *       <b>notFull.signal();</b>
 *       return x;
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 * }
 * </pre>
 *
 * (The {@link java.util.concurrent.ArrayBlockingQueue} class provides
 * this functionality, so there is no reason to implement this
 * sample usage class.)
 *
 * <p>A {@code Condition} implementation can provide behavior and semantics
 * that is
 * different from that of the {@code Object} monitor methods, such as
 * guaranteed ordering for notifications, or not requiring a lock to be held
 * when performing notifications.
 * If an implementation provides such specialized semantics then the
 * implementation must document those semantics.
 *
 * Condition 实现可以提供Object监视方法不同的行为和语义，例如通知顺序，或者唤醒的时候不需要
 * 持有锁。
 * 如果实现提供了这些特定的语义那么实现必需记录这些语义。
 *
 * <p>Note that {@code Condition} instances are just normal objects and can
 * themselves be used as the target in a {@code synchronized} statement,
 * and can have their own monitor {@link Object#wait wait} and
 * {@link Object#notify notification} methods invoked.
 * Acquiring the monitor lock of a {@code Condition} instance, or using its
 * monitor methods, has no specified relationship with acquiring the
 * {@link Lock} associated with that {@code Condition} or the use of its
 * {@linkplain #await waiting} and {@linkplain #signal signalling} methods.
 * It is recommended that to avoid confusion you never use {@code Condition}
 * instances in this way, except perhaps within their own implementation.
 *
 * 注意Condition实例仅仅是一个普通的对象，并且他们自己也可以作为目标被用在同步语句中，
 * 并且可以有他们自己的wait,notify方法被调用。
 * 获取Condition实例的监视锁，或者使用它的监视方法，没有特别的关系和获取Condition有关的锁，
 * 或者使用await方法和singanl方法。
 * 建设你为了避免混淆，永远不要这个用，除非在它们自己的实现中。
 *
 * <p>Except where noted, passing a {@code null} value for any parameter
 * will result in a {@link NullPointerException} being thrown.
 *
 * 除了上面提到的注意事项，给参数传null值将导致NullPointerException。
 *
 * <h3>Implementation Considerations</h3>
 *
 * 实现注意事项。
 *
 * <p>When waiting upon a {@code Condition}, a &quot;<em>spurious
 * wakeup</em>&quot; is permitted to occur, in
 * general, as a concession to the underlying platform semantics.
 * This has little practical impact on most application programs as a
 * {@code Condition} should always be waited upon in a loop, testing
 * the state predicate that is being waited for.  An implementation is
 * free to remove the possibility of spurious wakeups but it is
 * recommended that applications programmers always assume that they can
 * occur and so always wait in a loop.
 *
 * 在等待一个Condition的时候，允许出现"虚假唤醒"，通常，作为底层平台的语义的让步。
 * 这在大部分实现中有一点实际影响，因为Condition应该总是被等待在循环中，检查等待的状态断言。
 * 实现上可以自由地删除虚假的唤醒，但是建议应该编程者总是假定虚假唤醒可以发生，并且总是在循环中
 * 等待。
 *
 * <p>The three forms of condition waiting
 * (interruptible, non-interruptible, and timed) may differ in their ease of
 * implementation on some platforms and in their performance characteristics.
 * In particular, it may be difficult to provide these features and maintain
 * specific semantics such as ordering guarantees.
 * Further, the ability to interrupt the actual suspension of the thread may
 * not always be feasible to implement on all platforms.
 *
 * 三种形式的条件等待(可中断，不可中断，定时)可能在一些平台上的实现和性能特点不同。特别地，
 * 在提供一些特性并且保有特定的语义，比如顺序保证，可能是困难的。
 *
 * 还有，中断暂定的线程可能在所有的平台上都实现是不可行的。
 *
 * <p>Consequently, an implementation is not required to define exactly the
 * same guarantees or semantics for all three forms of waiting, nor is it
 * required to support interruption of the actual suspension of the thread.
 *
 * <p>An implementation is required to
 * clearly document the semantics and guarantees provided by each of the
 * waiting methods, and when an implementation does support interruption of
 * thread suspension then it must obey the interruption semantics as defined
 * in this interface.
 *
 * <p>As interruption generally implies cancellation, and checks for
 * interruption are often infrequent, an implementation can favor responding
 * to an interrupt over normal method return. This is true even if it can be
 * shown that the interrupt occurred after another action that may have
 * unblocked the thread. An implementation should document this behavior.
 *
 * 因此，实现不要求为所有三种形式的等待定义安全相同的担保或语义，也不要求支持中断暂停的线程。
 * 要求实现清楚地记录每一种等待方法的语义和担保，并且当实现确实支持暂停线程的中断，那么它必需
 * 遵循这个接口中定义的中断语义。
 *
 * 因为中断通常意为着取消，并且检查中断是罕见的，实现可以优先响应中断，而不是普通的返回方法。
 * 尽管可以被证明在另一个行为之后的中断可能不会阻塞这个线程，也应该这样做。实现应该记录这个行为。
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Condition {

    /**
     * Causes the current thread to wait until it is signalled or
     * {@linkplain Thread#interrupt interrupted}.
     *
     * 导致当前线程等待，直到它被唤醒或中断。
     *
     * <p>The lock associated with this {@code Condition} is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of four things happens:
     * 关联到这个Conditon的锁被自动地释放，并且线程对线程调度变得不可用，休眠直到下面其中的
     * 事情发生:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * 1. 其它线程调用这个Condition的signal方法，并且当前线程被选择为被唤醒的线程，或者
     * 2. 其它线程调用了这个Condition的signalAll方法，或者
     * 3. 其它线程中断了当前线程，并且支持暂停线程的中断，
     * 4. 发生虚假唤醒
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * 在所有的情况中，这个方法返回前当前线程必需重新获取关联这个条件的锁。当线程返回时保证持有这个锁。
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     * 如果当前线程：
     * 进入这个方法的时候带着中断状态，或者 当等待的时候被中断，并且支持暂停线程的中断，那么
     * 导致抛出InterruptedException，并且当前线程的中断状态被清除。在第一种情况中，没有指明
     * 在线程释放之前是否检测了线程的中断。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal. In that case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * 实现注意事项
     *
     * 当这个方法调用的时候，当前线程被假定持有跟这个条件关联的锁。确定是否是这种情况依赖实现，并且如果不是
     * 怎么作出响应。通常，将抛出异常并且实现必须记录这种事实。
     *
     * 实现可以优先响应中断，而不是普通的返回方法，这种情况实现必须保证信号被更定向到其它等待的线程，如果有
     * 的话。
     *
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    void await() throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled.
     *
     * 当前线程等待直到它被唤醒。
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of three things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * 跟这个条件关联的锁被自动地释放，并且对于线程调度变得不可用，休眠直到下面其中一件事发生：
     * 1. 其它线程调用这个Condition的singnal方法，并且当前线程正好被选为将要被唤醒的线程，或者
     * 2. 其它线程调用了这个Condition的singnalAll方法，或者
     * 3. 发生虚假唤醒
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * <p>If the current thread's interrupted status is set when it enters
     * this method, or it is {@linkplain Thread#interrupt interrupted}
     * while waiting, it will continue to wait until signalled. When it finally
     * returns from this method its interrupted status will still
     * be set.
     *
     * 在所有的情况中，在这个方法返回之前，当前线程被重新获取到跟这个条件关联的锁。当前这个线程返回的时候保证持有这个锁。
     *
     * 如果当前线程的中断状态被设置当进入这个方法的时候，或者等待的时候被中断，它将会继续等待直到被唤醒。当它最后从这个方法
     * 返回的时候，它的中断状态仍然被设置。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * 实现注意事项
     * 当这个方法调用的时候，当前线程被假设持有跟这个条件关联的锁。
     * 确定是不是这种情况依赖于实现，如果不是这样，怎么响应。通常来说，将会抛出一个异常。并且实现必须记录这种情况。
     */
    void awaitUninterruptibly();

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses.
     *
     * 当前线程等待直到它被唤醒或被中断，或者指定的等待时间用完。
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified waiting time elapses; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * 跟这个条件关联的锁被自动地释放，并且对于线程调度变得不可用，休眠直到下面其中一件事发生：
     * 1. 其它线程调用这个Condition的singnal方法，并且当前线程正好被选为将要被唤醒的线程，或者
     * 2. 其它线程调用了这个Condition的singnalAll方法，或者
     * 3. 其它线程中断了当前线程，并且暂停线程的中断被支持
     * 4. 指定的等待时间用完
     * 5. 发生虚假唤醒
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * 在所有的情况中，在这个方法返回之前，当前线程被重新获取到跟这个条件关联的锁。当前这个线程返回的时候保证持有这个锁。
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     * 如果当前线程：
     * 进入这个方法的时候带着中断状态，或者 当等待的时候被中断，并且支持暂停线程的中断，那么
     * 导致抛出InterruptedException，并且当前线程的中断状态被清除。在第一种情况中，没有指明
     * 在线程释放之前是否检测了线程的中断。
     *
     * <p>The method returns an estimate of the number of nanoseconds
     * remaining to wait given the supplied {@code nanosTimeout}
     * value upon return, or a value less than or equal to zero if it
     * timed out. This value can be used to determine whether and how
     * long to re-wait in cases where the wait returns but an awaited
     * condition still does not hold. Typical uses of this method take
     * the following form:
     *
     * 返回时这个方法返回一个估算的剩下纳秒数值根据提供的nanosTimeout值，或者一个小于或等于0的
     * 值，如果超时。这个值可以被用来确定是否需要和再次等待的时间，在等待返回但是awaited的条件没有持有的情况下。
     * 这个方法的通常用法像下面这样：
     *  <pre> {@code
     * boolean aMethod(long timeout, TimeUnit unit) {
     *   long nanos = unit.toNanos(timeout);
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (nanos <= 0L)
     *         return false;
     *       nanos = theCondition.awaitNanos(nanos);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p>Design note: This method requires a nanosecond argument so
     * as to avoid truncation errors in reporting remaining times.
     * Such precision loss would make it difficult for programmers to
     * ensure that total waiting times are not systematically shorter
     * than specified when re-waits occur.
     *
     * 设计注意事项：在返回剩余时间的时候为了避免截断错误，这个方法需要一个纳秒的参数。
     * 这种精度的丢失将使程序员非常困难，确定所有的等待时间不比指定的时间短，当再次等待发生的时候。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the elapse
     * of the specified waiting time. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * 实现注意事项:
     * 当这个方法调用的时候，当前线程被假设持有跟这个条件关联的锁。
     * 确定是不是这种情况依赖于实现，如果不是这样，怎么响应。通常来说，将会抛出一个异常。并且实现必须记录这种情况。
     * 实现可以优先响应中断，而不是普通的返回方法或者表示指定等待时间的用完，这种情况实现必须保证信号被更定向到
     * 其它等待的线程，如果有的话。
     *
     * @param nanosTimeout the maximum time to wait, in nanoseconds
     * @return an estimate of the {@code nanosTimeout} value minus
     *         the time spent waiting upon return from this method.
     *         A positive value may be used as the argument to a
     *         subsequent call to this method to finish waiting out
     *         the desired time.  A value less than or equal to zero
     *         indicates that no time remains.
     *         在从这个方法返回时减去等待的时间的一个估算纳秒值。一个正的值可以被使用
     *         传给随后的调用这个方法，来在渴望的时间内结束等待。小于或等于0的值表示他
     *         没有剩余时间了。
     *
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses. This method is behaviorally
     * equivalent to:
     *  <pre> {@code awaitNanos(unit.toNanos(time)) > 0}</pre>
     *
     * @param time the maximum time to wait
     * @param unit the time unit of the {@code time} argument
     * @return {@code false} if the waiting time detectably elapsed
     *         before return from the method, else {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified deadline elapses.
     *
     * 当前线程等待直到它被唤醒或中断或者到了指定的最后期限。
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified deadline elapses; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * 跟这个条件关联的锁被自动地释放，并且对于线程调度变得不可用，休眠直到下面其中一件事发生：
     * 1. 其它线程调用这个Condition的singnal方法，并且当前线程正好被选为将要被唤醒的线程，或者
     * 2. 其它线程调用了这个Condition的singnalAll方法，或者
     * 3. 其它线程中断了当前线程，并且暂停线程的中断被支持
     * 4. 到了指定的最后时间。
     * 5. 发生虚假唤醒
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * 在所有的情况中，在这个方法返回之前，当前线程被重新获取到跟这个条件关联的锁。当前这个线程返回的时候保证持有这个锁。
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     * 如果当前线程：
     * 进入这个方法的时候带着中断状态，或者 当等待的时候被中断，并且支持暂停线程的中断，那么
     * 导致抛出InterruptedException，并且当前线程的中断状态被清除。在第一种情况中，没有指明
     * 在线程释放之前是否检测了线程的中断。
     *
     * <p>The return value indicates whether the deadline has elapsed,
     * which can be used as follows:
     * 返回值表示是否已经到了最后时间，可以像下面使用
     *
     *  <pre> {@code
     * boolean aMethod(Date deadline) {
     *   boolean stillWaiting = true;
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (!stillWaiting)
     *         return false;
     *       stillWaiting = theCondition.awaitUntil(deadline);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the passing
     * of the specified deadline. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * 实现注意事项:
     * 当这个方法调用的时候，当前线程被假设持有跟这个条件关联的锁。
     * 确定是不是这种情况依赖于实现，如果不是这样，怎么响应。通常来说，将会抛出一个异常。并且实现必须记录这种情况。
     * 实现可以优先响应中断，而不是普通的返回方法或者表示到了指定等待时间，这种情况实现必须保证信号被更定向到
     * 其它等待的线程，如果有的话。
     *
     * @param deadline the absolute time to wait until
     * @return {@code false} if the deadline has elapsed upon return, else
     *         {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * Wakes up one waiting thread.
     *
     * <p>If any threads are waiting on this condition then one
     * is selected for waking up. That thread must then re-acquire the
     * lock before returning from {@code await}.
     *
     * 唤醒一个等待的线程
     * 如果有线程正在等待这个条件，那么其中一个线程被选择来唤醒。这个线程必须重新获取锁
     * 在从await返回的时候。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     *
     * 实现注意事项：
     * 实现可能(通常是这样)要求当前线程持有关联这个条件的锁，当这个方法被调用的时候。实现必须
     * 记录这个先决条件，并且采取一些措施如果锁没有被持有。通常抛出一个异常。
     *
     */
    void signal();

    /**
     * Wakes up all waiting threads.
     *
     * <p>If any threads are waiting on this condition then they are
     * all woken up. Each thread must re-acquire the lock before it can
     * return from {@code await}.
     *
     * 唤醒所有等待的线程
     * 如果有线程正在等待这个条件，那么所有线程被唤醒。这个线程必须重新获取锁
     * 在从await返回的时候。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     *
     * 实现注意事项：
     * 实现可能(通常是这样)要求当前线程持有关联这个条件的锁，当这个方法被调用的时候。实现必须
     * 记录这个先决条件，并且采取一些措施如果锁没有被持有。通常抛出一个异常。
     *
     */
    void signalAll();
}
