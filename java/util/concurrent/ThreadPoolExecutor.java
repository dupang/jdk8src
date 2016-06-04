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

package java.util.concurrent;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

/**
 * An {@link ExecutorService} that executes each submitted task using
 * one of possibly several pooled threads, normally configured
 * using {@link Executors} factory methods.
 *
 * 使用线程池中的可用的线程执行每一个提交的任务，线程池通常使用Executors的方法来配置。
 *
 * <p>Thread pools address two different problems: they usually
 * provide improved performance when executing large numbers of
 * asynchronous tasks, due to reduced per-task invocation overhead,
 * and they provide a means of bounding and managing the resources,
 * including threads, consumed when executing a collection of tasks.
 * Each {@code ThreadPoolExecutor} also maintains some basic
 * statistics, such as the number of completed tasks.
 *
 * 线程池解决两个不同的问题:在执行大量的异步任务的时候他们通常能提高性能，
 * 因为减小了每一个任务的调用开销，并且提供了一个绑定和管理资源的方法，包括线程，
 * 在执行一个组任务时消费。每一个ThreadPoolExecutor也维护一些基本的数据，例如
 * 完成任务的数量。
 *
 * <p>To be useful across a wide range of contexts, this class
 * provides many adjustable parameters and extensibility
 * hooks. However, programmers are urged to use the more convenient
 * {@link Executors} factory methods {@link
 * Executors#newCachedThreadPool} (unbounded thread pool, with
 * automatic thread reclamation), {@link Executors#newFixedThreadPool}
 * (fixed size thread pool) and {@link
 * Executors#newSingleThreadExecutor} (single background thread), that
 * preconfigure settings for the most common usage
 * scenarios. Otherwise, use the following guide when manually
 * configuring and tuning this class:
 *
 * 为了在广大范围的上下主文中有用，这个类提供了很多可调试的参数和可扩展的hooks.然而，
 * 强烈要求程序员使用更方便的Executors工厂方法Executors.newCachedThreadPool().
 * (没有边界的线程池，自动线程重复利用)，Executors#newFixedThreadPool(固定数量的线程池)
 * 和Executors#newSingleThreadExecutor(单一的后台线程)，这些都是为了大部分常用的使用场景
 * 事先配置好的。否则，当手动配置和调试这个类的时候使用下面的指导。
 * <dl>
 *
 * <dt>Core and maximum pool sizes</dt>
 *
 * 核心和最大池数量
 *
 * <dd>A {@code ThreadPoolExecutor} will automatically adjust the
 * pool size (see {@link #getPoolSize})
 * according to the bounds set by
 * corePoolSize (see {@link #getCorePoolSize}) and
 * maximumPoolSize (see {@link #getMaximumPoolSize}).
 *
 * 一个ThreadPoolExecutor将自动地调用池数量(参考getPoolSize)根据设置的corePoolSize
 * 和maximumPoolSize。
 *
 * When a new task is submitted in method {@link #execute(Runnable)},
 * and fewer than corePoolSize threads are running, a new thread is
 * created to handle the request, even if other worker threads are
 * idle.  If there are more than corePoolSize but less than
 * maximumPoolSize threads running, a new thread will be created only
 * if the queue is full.  By setting corePoolSize and maximumPoolSize
 * the same, you create a fixed-size thread pool. By setting
 * maximumPoolSize to an essentially unbounded value such as {@code
 * Integer.MAX_VALUE}, you allow the pool to accommodate an arbitrary
 * number of concurrent tasks. Most typically, core and maximum pool
 * sizes are set only upon construction, but they may also be changed
 * dynamically using {@link #setCorePoolSize} and {@link
 * #setMaximumPoolSize}. </dd>
 *
 * 当一个新的任务在方法中execute(Runnable)被提交,并且运行中的线程小于corePoolSize,
 * 一个新的线程被创建来处理这个请求。即使其它工作线程是空闲的。如果运行中的线程大于corePoolSize
 * 但是小于maximumPoolSize，只有在队列满的时候才会创建一个新线程。通过调用corePoolSize和maximumPoolSize
 * 一样大小，你创建一个固定数量的线程池。通过设置maximunPoolSize到一个无限的值比如Integer.MAX_VALUE,
 * 你允许线程池累计任意数量的并发任务。更典型地，core和maximum池数量只在构造的时候设置，但是他们
 * 也可以动态地改变使用setCorePoolSize和setMaximumPoolSize.
 *
 * <dt>On-demand construction</dt>
 *
 * 按需构造
 *
 * <dd>By default, even core threads are initially created and
 * started only when new tasks arrive, but this can be overridden
 * dynamically using method {@link #prestartCoreThread} or {@link
 * #prestartAllCoreThreads}.  You probably want to prestart threads if
 * you construct the pool with a non-empty queue. </dd>
 *
 * 默认地，尽管core thread 被初始化创建并且只在有新任务的时候才开始，但是这可以被
 * 动态地重写使用prestartCoreThread方法或prestartAllCoreThreads。
 * 你可能想提前启动线程如果你用一个非空的队列构建池。
 *
 * <dt>Creating new threads</dt>
 *
 * 创建一个新线程。
 *
 * <dd>New threads are created using a {@link ThreadFactory}.  If not
 * otherwise specified, a {@link Executors#defaultThreadFactory} is
 * used, that creates threads to all be in the same {@link
 * ThreadGroup} and with the same {@code NORM_PRIORITY} priority and
 * non-daemon status. By supplying a different ThreadFactory, you can
 * alter the thread's name, thread group, priority, daemon status,
 * etc. If a {@code ThreadFactory} fails to create a thread when asked
 * by returning null from {@code newThread}, the executor will
 * continue, but might not be able to execute any tasks. Threads
 * should possess the "modifyThread" {@code RuntimePermission}. If
 * worker threads or other threads using the pool do not possess this
 * permission, service may be degraded: configuration changes may not
 * take effect in a timely manner, and a shutdown pool may remain in a
 * state in which termination is possible but not completed.</dd>
 *
 * 新线程被创建使用ThreadFactory.如果不另外指定，使用Executors#defaultThreadFactory
 * 创建的所有线程都是在同一个ThreadGroup,相同的优先级，并且都是非守护状态。
 * 通过提供一个不同的线程工厂，你可以修改线程的名字，线程的组，优先级，守护状态等等。
 * 如果ThreadFactory创建线程失败，executor将继续，但是可能不会执行任何任务。
 * 线程应该掌握"modifyThread"RuntimePermission.如果池中的工作线程或其它线程
 * 没有持有这个许可，服务可能会被降级：配置改变可能不会及时生效，并且一个关闭的池可能处于
 * 已经终止但是没有完成的状态。
 *
 * <dt>Keep-alive times</dt>
 *
 *  活跃的时间
 *
 * <dd>If the pool currently has more than corePoolSize threads,
 * excess threads will be terminated if they have been idle for more
 * than the keepAliveTime (see {@link #getKeepAliveTime(TimeUnit)}).
 * This provides a means of reducing resource consumption when the
 * pool is not being actively used. If the pool becomes more active
 * later, new threads will be constructed. This parameter can also be
 * changed dynamically using method {@link #setKeepAliveTime(long,
 * TimeUnit)}.  Using a value of {@code Long.MAX_VALUE} {@link
 * TimeUnit#NANOSECONDS} effectively disables idle threads from ever
 * terminating prior to shut down. By default, the keep-alive policy
 * applies only when there are more than corePoolSize threads. But
 * method {@link #allowCoreThreadTimeOut(boolean)} can be used to
 * apply this time-out policy to core threads as well, so long as the
 * keepAliveTime value is non-zero. </dd>
 *
 * 如果当前的池有多于corePoolSize的线程，超出的线程将会被终止，如果他们空闲了多于
 * keepAliveTime的时间。这提供了一个减少资源浪费的手段，当线程池没有被使用的时候。
 * 如果线程池后来变得更活跃了，新线程将会构建。这个参数也可以通过方法动态地创建。
 * 使用Long.MAX_VALUE这个值实际上就会禁用空闲的线程变得终止在关闭前。默认的，
 * keep-alive的策略只应用在有多于corePoolSize个线程的时候。但是方法allowCoreThreadTimeOut(boolean)
 * 也可以用来对core thread的超时策略，只要keepAliveTime的值不是0;
 *
 * <dt>Queuing</dt>
 *
 * 队列
 *
 * <dd>Any {@link BlockingQueue} may be used to transfer and hold
 * submitted tasks.  The use of this queue interacts with pool sizing:
 *
 * 一个BlockingQueue可以用来救困转移和持有提交的任务。队列的使用和池大小相互作用。
 *
 * <ul>
 *
 * <li> If fewer than corePoolSize threads are running, the Executor
 * always prefers adding a new thread
 * rather than queuing.</li>
 *
 * 如果运行的线程数小于corePoolSize,Executor总是增加一个新线程而不是加入到队列。
 *
 * <li> If corePoolSize or more threads are running, the Executor
 * always prefers queuing a request rather than adding a new
 * thread.</li>
 *
 * 如果大于等于corePoolSize的运行线程，Executo总是倾向于排队请求而不是新建一个线程。
 *
 * <li> If a request cannot be queued, a new thread is created unless
 * this would exceed maximumPoolSize, in which case, the task will be
 * rejected.</li>
 *
 * 如果一个请求不能被排队，就会创建一个新的线程除非这会超过maximumPoolSize,如果
 * 超过maximumPoolSize就会被拒绝。
 *
 * </ul>
 *
 * There are three general strategies for queuing:
 * 有三种入队的策略
 * <ol>
 *
 * <li> <em> Direct handoffs.</em> A good default choice for a work
 * queue is a {@link SynchronousQueue} that hands off tasks to threads
 * without otherwise holding them. Here, an attempt to queue a task
 * will fail if no threads are immediately available to run it, so a
 * new thread will be constructed. This policy avoids lockups when
 * handling sets of requests that might have internal dependencies.
 * Direct handoffs generally require unbounded maximumPoolSizes to
 * avoid rejection of new submitted tasks. This in turn admits the
 * possibility of unbounded thread growth when commands continue to
 * arrive on average faster than they can be processed.  </li>
 *
 * 直接切换。对工作队列来说一个好的默认选择是SynchronousQueue,直接把任务交给
 * 线程而不是持有他们。这里，如果没有线程立刻运行任务，试图把任务加入队列就会失败。
 * 所以一个新的将会被构建。这个策略避免了寻找当持有一组可能有内部依赖的请求的时候。
 * 直接切换通常需要没有边界的maximumPoolSize来避免拒绝新提交的任务。这反过来也
 * 说了线程无限增加的可能性，当命令持续到来超过处理的速度。
 *
 * <li><em> Unbounded queues.</em> Using an unbounded queue (for
 * example a {@link LinkedBlockingQueue} without a predefined
 * capacity) will cause new tasks to wait in the queue when all
 * corePoolSize threads are busy. Thus, no more than corePoolSize
 * threads will ever be created. (And the value of the maximumPoolSize
 * therefore doesn't have any effect.)  This may be appropriate when
 * each task is completely independent of others, so tasks cannot
 * affect each others execution; for example, in a web page server.
 * While this style of queuing can be useful in smoothing out
 * transient bursts of requests, it admits the possibility of
 * unbounded work queue growth when commands continue to arrive on
 * average faster than they can be processed.  </li>
 *
 * 没有边界的队列，使用一个没有预定容量的无界的队列(例如，一个LinkedBlockingQueue).
 * 将使新任务在队列中等待在所有corePoolSize线程忙的时候。因为多于corePoolSize
 * 的线程将不会被创建。(并且maximumPoolSize的值不会有效果)。当每一个任务是完全独立
 * 的时候可能是合适的。所以任务的执行不会相互影响。例如，一个web页面服务。
 * 这种队列的风格在短时的爆发请求的时候是有用的，我也允许了线程无限增加的可能性，
 * 当命令持续到来超过处理的速度。
 *
 * <li><em>Bounded queues.</em> A bounded queue (for example, an
 * {@link ArrayBlockingQueue}) helps prevent resource exhaustion when
 * used with finite maximumPoolSizes, but can be more difficult to
 * tune and control.  Queue sizes and maximum pool sizes may be traded
 * off for each other: Using large queues and small pools minimizes
 * CPU usage, OS resources, and context-switching overhead, but can
 * lead to artificially low throughput.  If tasks frequently block (for
 * example if they are I/O bound), a system may be able to schedule
 * time for more threads than you otherwise allow. Use of small queues
 * generally requires larger pool sizes, which keeps CPUs busier but
 * may encounter unacceptable scheduling overhead, which also
 * decreases throughput.  </li>
 *
 * 有界的队列。一个有界的队列(例如ArrayBlockingQueue)帮助防止资源用尽当使用一个无限的maximumPoolSize的时候。
 * 但是可能更困难调试和控制。队列大小和maximumPoolSize可以相互替换:使用大队列和小池减小CPU使用，OS资源，
 * 和上下文切换的开销，但是可能导致人为地低的吞吐量。如果任务经常地阻塞(例如如果I/O限制)，一个系统可能
 * 调度更多的线程比你允许的。使用小的队列通常需要大的池大小，这将使CPU更忙但是可能遇到不可接受的调度开销，
 * 但是也同时也增加了吞吐量。
 *
 * </ol>
 *
 * </dd>
 *
 * <dt>Rejected tasks</dt>
 *
 * <dd>New tasks submitted in method {@link #execute(Runnable)} will be
 * <em>rejected</em> when the Executor has been shut down, and also when
 * the Executor uses finite bounds for both maximum threads and work queue
 * capacity, and is saturated.  In either case, the {@code execute} method
 * invokes the {@link
 * RejectedExecutionHandler#rejectedExecution(Runnable, ThreadPoolExecutor)}
 * method of its {@link RejectedExecutionHandler}.  Four predefined handler
 * policies are provided:
 *
 * 拒绝任务
 * 当Executor已经关闭的时候，和当Executor使用有界的Maximum thread和工作队列容量并且已经饱和时、
 * ，新提交的任务将会被拒绝。任一一种情况，execute方法调用RejectedExecutionHandler#rejectedExecution(Runnable, ThreadPoolExecutor)
 * 方法。提供了四个预定义的处理策略。
 * <ol>
 *
 * <li> In the default {@link ThreadPoolExecutor.AbortPolicy}, the
 * handler throws a runtime {@link RejectedExecutionException} upon
 * rejection. </li>
 *
 * 默认的ThreadPoolExecutor.AbortPolicy，在拒绝的时候抛出一个运行时异常RejectedExecutionException。
 *
 * <li> In {@link ThreadPoolExecutor.CallerRunsPolicy}, the thread
 * that invokes {@code execute} itself runs the task. This provides a
 * simple feedback control mechanism that will slow down the rate that
 * new tasks are submitted. </li>
 *
 * ThreadPoolExecutor.CallerRunsPolicy，调用execute的线程运行任务。这提供
 * 了一个简单的反馈机制，这将会减慢任务被提交的速率。
 *
 * <li> In {@link ThreadPoolExecutor.DiscardPolicy}, a task that
 * cannot be executed is simply dropped.  </li>
 *
 * <li>In {@link ThreadPoolExecutor.DiscardOldestPolicy}, if the
 * executor is not shut down, the task at the head of the work queue
 * is dropped, and then execution is retried (which can fail again,
 * causing this to be repeated.) </li>
 *
 * ThreadPoolExecutor.DiscardOldestPolicy，如果executor没有关闭，处理队列的
 * 头的任务被丢弃，并且执行被重试(这可能再次失败，导致这个重复)
 *
 * </ol>
 *
 * It is possible to define and use other kinds of {@link
 * RejectedExecutionHandler} classes. Doing so requires some care
 * especially when policies are designed to work only under particular
 * capacity or queuing policies. </dd>
 *
 * 可以定义和使用其它的RejectedExecutionHandler类。这样做需要注意尤其策略被设计在
 * 只工作在待定的容量或排队策略。
 *
 * <dt>Hook methods</dt>
 *
 * 钩子方法
 *
 * <dd>This class provides {@code protected} overridable
 * {@link #beforeExecute(Thread, Runnable)} and
 * {@link #afterExecute(Runnable, Throwable)} methods that are called
 * before and after execution of each task.  These can be used to
 * manipulate the execution environment; for example, reinitializing
 * ThreadLocals, gathering statistics, or adding log entries.
 * Additionally, method {@link #terminated} can be overridden to perform
 * any special processing that needs to be done once the Executor has
 * fully terminated.
 *
 * 这个类提供了受保护的或被重写的beforeExecute(Thread, Runnable)和afterExecute(Runnable, Throwable)
 * 在执行每一个任务之前和之后被调用。这可以被用来控制执行环境;例如，初始化ThreadLocal,收集通计数据，
 * 或增加日志。另外terminated方法可以被重写来执行任务特别的处理，在Executor已经完全终止后。
 *
 * <p>If hook or callback methods throw exceptions, internal worker
 * threads may in turn fail and abruptly terminate.</dd>
 *
 * 如果钩子或回调方法抛出异常，内部的工作线程可能会失败并且突然终止。
 *
 * <dt>Queue maintenance</dt>
 *
 * 队列维护
 *
 * <dd>Method {@link #getQueue()} allows access to the work queue
 * for purposes of monitoring and debugging.  Use of this method for
 * any other purpose is strongly discouraged.  Two supplied methods,
 * {@link #remove(Runnable)} and {@link #purge} are available to
 * assist in storage reclamation when large numbers of queued tasks
 * become cancelled.</dd>
 *
 * getQueue方法允许为了监控和调试来访问工作队列。为了其它目的而使用这个方法是非常不
 * 鼓励的。提供了两个remove(Runnable)和purge()可以来帮助回收存储当大量的入队的
 * 任务被取消的时候。
 *
 * <dt>Finalization</dt>
 *  最后
 * <dd>A pool that is no longer referenced in a program <em>AND</em>
 * has no remaining threads will be {@code shutdown} automatically. If
 * you would like to ensure that unreferenced pools are reclaimed even
 * if users forget to call {@link #shutdown}, then you must arrange
 * that unused threads eventually die, by setting appropriate
 * keep-alive times, using a lower bound of zero core threads and/or
 * setting {@link #allowCoreThreadTimeOut(boolean)}.  </dd>
 *
 * 线程池不再被程序里引用并且没有剩下的线程将被自动地关闭。如果你想你想保证
 * 不在引用的池被回收即使用户忘记调用shutdown()，那么你必须安排没使用的线程
 * 最后死掉，通过设置适当的keep-alive时间，使用一个接近0的core thread 和/和
 * 设置allowCoreThreadTimeOut(boolean).
 *
 * </dl>
 *
 * <p><b>Extension example</b>. Most extensions of this class
 * override one or more of the protected hook methods. For example,
 * here is a subclass that adds a simple pause/resume feature:
 *
 *  <pre> {@code
 * class PausableThreadPoolExecutor extends ThreadPoolExecutor {
 *   private boolean isPaused;
 *   private ReentrantLock pauseLock = new ReentrantLock();
 *   private Condition unpaused = pauseLock.newCondition();
 *
 *   public PausableThreadPoolExecutor(...) { super(...); }
 *
 *   protected void beforeExecute(Thread t, Runnable r) {
 *     super.beforeExecute(t, r);
 *     pauseLock.lock();
 *     try {
 *       while (isPaused) unpaused.await();
 *     } catch (InterruptedException ie) {
 *       t.interrupt();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void pause() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = true;
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void resume() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = false;
 *       unpaused.signalAll();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ThreadPoolExecutor extends AbstractExecutorService {
    /**
     * The main pool control state, ctl, is an atomic integer packing
     * two conceptual fields
     *   workerCount, indicating the effective number of threads
     *   runState,    indicating whether running, shutting down etc
     *
     * 最主要的池控制状态，ctl,是一个包含两个概念字段的原子的Integer
     *
     *  工作者数量，表示线程的有效数量
     *  运行状态，  表示是否正在运行，关闭等等
     *
     * In order to pack them into one int, we limit workerCount to
     * (2^29)-1 (about 500 million) threads rather than (2^31)-1 (2
     * billion) otherwise representable. If this is ever an issue in
     * the future, the variable can be changed to be an AtomicLong,
     * and the shift/mask constants below adjusted. But until the need
     * arises, this code is a bit faster and simpler using an int.
     *
     * 为了把他们打包成一个Int,我们限制工作者数量为(2^29)-1(大概5千万)
     * 个线程而不是(2^31)-1(20亿)。如果在未来这是一个问题，变量可以变
     * 成一个AtomicLong,并且调整下面的shift/mask。但是在需要改变之前，
     * 这代码使用int有一点快，并且简单.
     *
     * The workerCount is the number of workers that have been
     * permitted to start and not permitted to stop.  The value may be
     * transiently different from the actual number of live threads,
     * for example when a ThreadFactory fails to create a thread when
     * asked, and when exiting threads are still performing
     * bookkeeping before terminating. The user-visible pool size is
     * reported as the current size of the workers set.
     *
     * workerCount是已经被允许开始但是还没结束的工作者数量。这个值可能
     * 跟真实线程数量短暂地不同，例如当一个ThreadFactor创建线程失败，
     * 并且当正在退出的线程在退出之前仍然执行记账。用户可见的池数量为
     * 当前工作者集合的数量。
     *
     * The runState provides the main lifecycle control, taking on values:
     *
     * 提供最主要的生命周期控制的运行状态，取值如下：
     *
     *   RUNNING:  Accept new tasks and process queued tasks
     *   SHUTDOWN: Don't accept new tasks, but process queued tasks
     *   STOP:     Don't accept new tasks, don't process queued tasks,
     *             and interrupt in-progress tasks
     *   TIDYING:  All tasks have terminated, workerCount is zero,
     *             the thread transitioning to state TIDYING
     *             will run the terminated() hook method
     *   TERMINATED: terminated() has completed
     *
     *   RUNNING: 接受新任务和处理队列中的任务。
     *   SHUTDOWN: 不在接受新的任务，但是处理队列中的任务
     *   STOP:    不在接受新任务，不在处理队列中的任务，
     *            并且中断处理中的任务
     *   TIDYING: 所有任务已经被终止，workerCount为0,
     *            线程状态为TIDYING将会运行terminated()方法。
     *   TERMINATED: terminated()已经完成。
     *
     * The numerical order among these values matters, to allow
     * ordered comparisons. The runState monotonically increases over
     * time, but need not hit each state. The transitions are:
     *
     * 为了有序比较，这些值的数字顺序是重要的。runState随着时间单调递增，但是不需要
     * 碰撞每一个状态。他们的转换是:
     *
     * RUNNING -> SHUTDOWN
     *    On invocation of shutdown(), perhaps implicitly in finalize()
     *    在调用shutdown()方法的时候，也许隐式地在finalize()
     *
     * (RUNNING or SHUTDOWN) -> STOP
     *    On invocation of shutdownNow()
     *    在调用shutdownNow()的时候。
     *
     * SHUTDOWN -> TIDYING
     *    When both queue and pool are empty
     *    在队列和池都为空的时候。
     *
     * STOP -> TIDYING
     *    When pool is empty
     *    在池为空的时候
     * TIDYING -> TERMINATED
     *    When the terminated() hook method has completed
     *    在terminated()方法已经完成的时候。
     *
     * Threads waiting in awaitTermination() will return when the
     * state reaches TERMINATED.
     *
     * 在awaitTermination()方法中等待的线程在状态为TERMINATED的时候将返回。
     *
     * Detecting the transition from SHUTDOWN to TIDYING is less
     * straightforward than you'd like because the queue may become
     * empty after non-empty and vice versa during SHUTDOWN state, but
     * we can only terminate if, after seeing that it is empty, we see
     * that workerCount is 0 (which sometimes entails a recheck -- see
     * below).
     *
     * 从SHUTDOWN到TIDYING的转换不是很直接比你想像的因为队列可能变成空在非空和在SHUTDOWN的状态，
     * 但是我们只可以终止如果在看到它的空的时候，我们看到workerCount为0(有时候需要重新检查--参考下面)
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

    // runState is stored in the high-order bits
    // runState被存储在高阶位
    private static final int RUNNING    = -1 << COUNT_BITS;
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    private static final int STOP       =  1 << COUNT_BITS;
    private static final int TIDYING    =  2 << COUNT_BITS;
    private static final int TERMINATED =  3 << COUNT_BITS;

    // Packing and unpacking ctl
    // 装箱和拆拆箱
    private static int runStateOf(int c)     { return c & ~CAPACITY; }
    private static int workerCountOf(int c)  { return c & CAPACITY; }
    private static int ctlOf(int rs, int wc) { return rs | wc; }

    /*
     * Bit field accessors that don't require unpacking ctl.
     * These depend on the bit layout and on workerCount being never negative.
     */

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    /**
     * Attempts to CAS-increment the workerCount field of ctl.
     * 试图增加workCount字段值
     */
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    /**
     * Attempts to CAS-decrement the workerCount field of ctl.
     * 试图减小workCount字段值
     */
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    /**
     * Decrements the workerCount field of ctl. This is called only on
     * abrupt termination of a thread (see processWorkerExit). Other
     * decrements are performed within getTask.
     *
     * 递减workerCount的值。这只在线程突然中断的时候被调用(参考processWorkerExit)。
     * 其它递减在getTask()中操作。
     *
     */
    private void decrementWorkerCount() {
        do {} while (! compareAndDecrementWorkerCount(ctl.get()));
    }

    /**
     * The queue used for holding tasks and handing off to worker
     * threads.  We do not require that workQueue.poll() returning
     * null necessarily means that workQueue.isEmpty(), so rely
     * solely on isEmpty to see if the queue is empty (which we must
     * do for example when deciding whether to transition from
     * SHUTDOWN to TIDYING).  This accommodates special-purpose
     * queues such as DelayQueues for which poll() is allowed to
     * return null even if it may later return non-null when delays
     * expire.
     *
     * 保存任务和把任务交给工作线程的队列。我们不要求必要时workQueue.poll()返回
     * null(意为着workQueue.isEmpty()),所以只依赖isEmpty()来查看队列是否为空
     * (例如当我们决定是否从SHUTDOWN转到TIDYING时必须这样做)。
     * 这使特定目的的队列例如DelayQueue()在poll()的时候返回null即使它可能后来不返回
     * null当延迟到期的时候。
     *
     */
    private final BlockingQueue<Runnable> workQueue;

    /**
     * Lock held on access to workers set and related bookkeeping.
     * While we could use a concurrent set of some sort, it turns out
     * to be generally preferable to use a lock. Among the reasons is
     * that this serializes interruptIdleWorkers, which avoids
     * unnecessary interrupt storms, especially during shutdown.
     * Otherwise exiting threads would concurrently interrupt those
     * that have not yet interrupted. It also simplifies some of the
     * associated statistics bookkeeping of largestPoolSize etc. We
     * also hold mainLock on shutdown and shutdownNow, for the sake of
     * ensuring workers set is stable while separately checking
     * permission to interrupt and actually interrupting.
     *
     * 锁定对工作集合访问和相关的记账的锁。
     * 虽然我们可以使用一些并发集合，一般来说最好使用一个锁。原因是序列化interruptIdleWorkers，
     * 避免不需要的中断风暴，特别在shutdown的时候。否则退出线程将并发地中断还没有被中断的线程。
     * 这也简化了一些largeestPoolSize的相关的统计讨账。我们在shutdown和shutdownNow的时候也
     * 持有锁，为了确保工作集合是稳定的在单独检查中断权限和实际中断的时候。
     *
     */
    private final ReentrantLock mainLock = new ReentrantLock();

    /**
     * Set containing all worker threads in pool. Accessed only when
     * holding mainLock.
     *
     * 包含池中所有工作线程的集合。只在持有锁的时候访问。
     */
    private final HashSet<Worker> workers = new HashSet<Worker>();

    /**
     * Wait condition to support awaitTermination
     * 等待条件
     */
    private final Condition termination = mainLock.newCondition();

    /**
     * Tracks largest attained pool size. Accessed only under
     * mainLock.
     * 追踪最大的池大小。只在mainLock下访问
     */
    private int largestPoolSize;

    /**
     * Counter for completed tasks. Updated only on termination of
     * worker threads. Accessed only under mainLock.
     *
     * 完成的任务的数量。只在工作线程终止睥时候更新。只在mainLock下访问
     *
     */
    private long completedTaskCount;

    /*
     * All user control parameters are declared as volatiles so that
     * ongoing actions are based on freshest values, but without need
     * for locking, since no internal invariants depend on them
     * changing synchronously with respect to other actions.
     *
     * 所有用户操控的参数被声明为volatiles所以正在进行的动作都是基于最新的值，
     * 但是不需要加锁，因为没有内部的不变性依赖他们同步地改变根据其它动作。
     */

    /**
     * Factory for new threads. All threads are created using this
     * factory (via method addWorker).  All callers must be prepared
     * for addWorker to fail, which may reflect a system or user's
     * policy limiting the number of threads.  Even though it is not
     * treated as an error, failure to create threads may result in
     * new tasks being rejected or existing ones remaining stuck in
     * the queue.
     *
     * We go further and preserve pool invariants even in the face of
     * errors such as OutOfMemoryError, that might be thrown while
     * trying to create threads.  Such errors are rather common due to
     * the need to allocate a native stack in Thread.start, and users
     * will want to perform clean pool shutdown to clean up.  There
     * will likely be enough memory available for the cleanup code to
     * complete without encountering yet another OutOfMemoryError.
     *
     * 为了创建新线程的工厂。所有线程被创建使用这个工厂(通过addWorker)。所有的调用
     * 都必须准备处理addWorker失败的情况，这种情况可能影响一个系统或用户的限制线程数量
     * 的策略。尽管它不被当作错误对待，创建线程失败可能导致正在被拒绝或正在退出的任务
     * 保留在队列中。
     *
     * 我们进一步保护池的一致性，即使在出现例如OutOfMemoryError错误的时候,这种错误
     * 可能被抛出在试图创建线程的时候。这种错误相当普遍由于需要分配本地栈在Thread.start里，
     * 用户将想执行清除池在shutdown的时候。这可能有足够可用的内存清理代码来完成而没有
     * 遇到另一个OutOfMemoryError。
     *
     */
    private volatile ThreadFactory threadFactory;

    /**
     * Handler called when saturated or shutdown in execute.
     *
     * 在执行中当饱和或关闭时被调用的拦截器。
     */
    private volatile RejectedExecutionHandler handler;

    /**
     * Timeout in nanoseconds for idle threads waiting for work.
     * Threads use this timeout when there are more than corePoolSize
     * present or if allowCoreThreadTimeOut. Otherwise they wait
     * forever for new work.
     *
     * 空间的线程等待任务的超时时间。线程使用这个超时时间当有多于corePoolSize个线程
     * 或者如果allowCoreThreadTimeOut为true.否则他们永远等待新的工作。
     *
     */
    private volatile long keepAliveTime;

    /**
     * If false (default), core threads stay alive even when idle.
     * If true, core threads use keepAliveTime to time out waiting
     * for work.
     *
     * 如果false(默认),core streads保持活着即使当空闲的时候。如果true，
     * core thread使用keepAliveTime来超时等待工作。
     *
     */
    private volatile boolean allowCoreThreadTimeOut;

    /**
     * Core pool size is the minimum number of workers to keep alive
     * (and not allow to time out etc) unless allowCoreThreadTimeOut
     * is set, in which case the minimum is zero.
     *
     * Core pool size是保持活着的线程的最小值(并且不允许超时)除非设置了
     * allowCoreThreadTimeOut,这时候最小值为0;
     */
    private volatile int corePoolSize;

    /**
     * Maximum pool size. Note that the actual maximum is internally
     * bounded by CAPACITY.
     *
     * 最大的线程池大小。注意真正的最大值内部被界定为CAPACITY
     */
    private volatile int maximumPoolSize;

    /**
     * The default rejected execution handler
     *
     * 默认的拒绝处理拦截器
     */
    private static final RejectedExecutionHandler defaultHandler =
        new AbortPolicy();

    /**
     * Permission required for callers of shutdown and shutdownNow.
     * We additionally require (see checkShutdownAccess) that callers
     * have permission to actually interrupt threads in the worker set
     * (as governed by Thread.interrupt, which relies on
     * ThreadGroup.checkAccess, which in turn relies on
     * SecurityManager.checkAccess). Shutdowns are attempted only if
     * these checks pass.
     *
     * All actual invocations of Thread.interrupt (see
     * interruptIdleWorkers and interruptWorkers) ignore
     * SecurityExceptions, meaning that the attempted interrupts
     * silently fail. In the case of shutdown, they should not fail
     * unless the SecurityManager has inconsistent policies, sometimes
     * allowing access to a thread and sometimes not. In such cases,
     * failure to actually interrupt threads may disable or delay full
     * termination. Other uses of interruptIdleWorkers are advisory,
     * and failure to actually interrupt will merely delay response to
     * configuration changes so is not handled exceptionally.
     *
     * 调用shutdown和shutdownNow的调用者需要的许可。我们另外需要(参考checkShutdownAccess)
     * 调用者有许可来真实地中断工作集合中的线程（受Thread.interrupt的支配，它依赖ThreadGroup.checkAccess
     * 它反过来依赖SecurityManager.checkAccess）.只有这些检查通过Shutdowns才被试图。
     *
     * 所有真实Thread.interrupt的调用()忽略SecurityExceptions,也就是说试图的中断静默地失败。在关闭的情况下，
     * 他们不会失败除非SecurityManager有不一致的策略，有时候允许访问有时候不允许访问线程。在这种情况下，
     * 真正中断线程失败可能不会或延迟终止。其它interruptIdleWorkers的使用是劝告的，
     * 并且中断失败将仅仅延迟响应配置改变所以不会异常地处理。
     */
    private static final RuntimePermission shutdownPerm =
        new RuntimePermission("modifyThread");

    /**
     * Class Worker mainly maintains interrupt control state for
     * threads running tasks, along with other minor bookkeeping.
     * This class opportunistically extends AbstractQueuedSynchronizer
     * to simplify acquiring and releasing a lock surrounding each
     * task execution.  This protects against interrupts that are
     * intended to wake up a worker thread waiting for a task from
     * instead interrupting a task being run.  We implement a simple
     * non-reentrant mutual exclusion lock rather than use
     * ReentrantLock because we do not want worker tasks to be able to
     * reacquire the lock when they invoke pool control methods like
     * setCorePoolSize.  Additionally, to suppress interrupts until
     * the thread actually starts running tasks, we initialize lock
     * state to a negative value, and clear it upon start (in
     * runWorker).
     *
     * Worker类主要为运行任务的线程维护中断控制状态，还有其它小的记账。
     * 这个类适当地继承AbstractQueuedSynchronizer来简化获取和释放一个包围
     * 每一个任务执行的锁。这阻止了想唤醒正在等待任务的工作线程的中断相反地中断了
     * 正在运行的任务。我们实现了一个简单的非重入的互斥锁而不是使用ReentrantLock
     * 因为我们不想工作任务能够再次获取锁当他们调用像setCorePoolSize这样的池控制方法。
     * 另外，为了防止中断直到线程真正开始运行任务，我们初始化锁状态为一个负数，并且在start的时候清除它(在runWorker).
     */
    private final class Worker
        extends AbstractQueuedSynchronizer
        implements Runnable
    {
        /**
         * This class will never be serialized, but we provide a
         * serialVersionUID to suppress a javac warning.
         *
         * 这个类将永远不会被序列化，但是我们提供了一个serialVersionUID
         * 来防止javac提醒。
         */
        private static final long serialVersionUID = 6138294804551838833L;

        /** Thread this worker is running in.  Null if factory fails. */
        //正在这个worker中运行的线程。如果factory失败就是null.
        final Thread thread;
        /** Initial task to run.  Possibly null. */
        //运行的初始任务，可能null
        Runnable firstTask;
        /** Per-thread task counter */
        //每一个任务的任务数量
        volatile long completedTasks;

        /**
         * Creates with given first task and thread from ThreadFactory.
         *
         * 用给定的第一个任务和从ThreadFactor创建的线程创建。
         * @param firstTask the first task (null if none)
         */
        Worker(Runnable firstTask) {
            setState(-1); // inhibit interrupts until runWorker
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this);
        }

        /** Delegates main run loop to outer runWorker  */
        //代理主要的运行循环到外面的runWorker
        public void run() {
            runWorker(this);
        }

        // Lock methods
        //
        // The value 0 represents the unlocked state.
        // The value 1 represents the locked state.
        //锁方法
        //0值表示未锁住状态
        //1值表示销售状态

        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock()        { acquire(1); }
        public boolean tryLock()  { return tryAcquire(1); }
        public void unlock()      { release(1); }
        public boolean isLocked() { return isHeldExclusively(); }

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }

    /*
     * Methods for setting control state
     *
     * 设置控制状态的方法
     */

    /**
     * Transitions runState to given target, or leaves it alone if
     * already at least the given target.
     *
     * 转换runState到一个给定的目标，或者不做任务操作如果已经处于目标状态。
     *
     * @param targetState the desired state, either SHUTDOWN or STOP
     *        (but not TIDYING or TERMINATED -- use tryTerminate for that)
     */
    private void advanceRunState(int targetState) {
        for (;;) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) ||
                ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
                break;
        }
    }

    /**
     * Transitions to TERMINATED state if either (SHUTDOWN and pool
     * and queue empty) or (STOP and pool empty).  If otherwise
     * eligible to terminate but workerCount is nonzero, interrupts an
     * idle worker to ensure that shutdown signals propagate. This
     * method must be called following any action that might make
     * termination possible -- reducing worker count or removing tasks
     * from the queue during shutdown. The method is non-private to
     * allow access from ScheduledThreadPoolExecutor.
     *
     * 转换为TERMINATED状态如果(SHUTDOWN和池和队列为空)或者(STOP和池为空)。
     * 如果有资格终止但是workerCount不为0，中断一个空闲的worker确保关闭信号
     * 传播。这个方法必须调用在任何可能使终止的动作之后--减少工作者数量或
     * 关闭时从队列中移除任务。这个方法不是私有的，允许从ScheduledThreadPoolExecutor
     * 中访问。
     */
    final void tryTerminate() {
        for (;;) {
            int c = ctl.get();
            if (isRunning(c) ||
                runStateAtLeast(c, TIDYING) ||
                (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
                return;
            if (workerCountOf(c) != 0) { // Eligible to terminate
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        ctl.set(ctlOf(TERMINATED, 0));
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }
            // else retry on failed CAS
        }
    }

    /*
     * Methods for controlling interrupts to worker threads.
     * 控制中断工作者线程的方法。
     */

    /**
     * If there is a security manager, makes sure caller has
     * permission to shut down threads in general (see shutdownPerm).
     * If this passes, additionally makes sure the caller is allowed
     * to interrupt each worker thread. This might not be true even if
     * first check passed, if the SecurityManager treats some threads
     * specially.
     *
     * 如果有一个安全管理者，确保调用者有许可关闭线程(参考shutdownPerm).
     * 如果这个通过，另外确保调用者被允许中断每一个工作者线程。这可能不为
     * true即使第一个检查通过，如果SecurityManager特殊对待一些线程。
     *
     */
    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (Worker w : workers)
                    security.checkAccess(w.thread);
            } finally {
                mainLock.unlock();
            }
        }
    }

    /**
     * Interrupts all threads, even if active. Ignores SecurityExceptions
     * (in which case some threads may remain uninterrupted).
     *
     * 中断所有的线程，即使是正在活跃。忽略SecurityExceptions
     * (这个情况一些线程可能仍处于没有被打断的状态)
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Interrupts threads that might be waiting for tasks (as
     * indicated by not being locked) so they can check for
     * termination or configuration changes. Ignores
     * SecurityExceptions (in which case some threads may remain
     * uninterrupted).
     *
     * 中断可能正在等待任务的线程(如未被锁定)所以他们可以检查
     * 终止或配置改变。忽略SecurityExceptions(这个情况一些线程可能仍处于没有被打断的状态)
     *
     * @param onlyOne If true, interrupt at most one worker. This is
     * called only from tryTerminate when termination is otherwise
     * enabled but there are still other workers.  In this case, at
     * most one waiting worker is interrupted to propagate shutdown
     * signals in case all threads are currently waiting.
     * Interrupting any arbitrary thread ensures that newly arriving
     * workers since shutdown began will also eventually exit.
     * To guarantee eventual termination, it suffices to always
     * interrupt only one idle worker, but shutdown() interrupts all
     * idle workers so that redundant workers exit promptly, not
     * waiting for a straggler task to finish.
     *
     * 如果true,最多中断一个worker.这个方法只能从tryTerminate方法
     * 调用，当终止时仍有其它worker.这种情况，最多一个等待中的worker被
     *                中断来传播关闭信号以防所有线程都在等待。
     *                中断任何线程确保新到的workers从关闭开始也将
     *                最终退出。
     *                为了保证最终终止，它满足总是只中断一个空闲的worker,
     *                但是shutdown()中断所有的空闲workers以便过剩的worder
     *                及时地退出，不等待一个落伍的任务结束。
     *
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers) {
                Thread t = w.thread;
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne)
                    break;
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Common form of interruptIdleWorkers, to avoid having to
     * remember what the boolean argument means.
     *
     * interruptIdleWorker一般模式，避免不得不记住参数的意思。
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    private static final boolean ONLY_ONE = true;

    /*
     * Misc utilities, most of which are also exported to
     * ScheduledThreadPoolExecutor
     *
     * 其它工具，其中的部分暴露给ScheduledThreadPoolExecutor。
     */

    /**
     * Invokes the rejected execution handler for the given command.
     * Package-protected for use by ScheduledThreadPoolExecutor.
     *
     * 带着给定的命令调用拒绝执行拦截器。
     * 可被ScheduledThreadPoolExecutor使用。
     */
    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    /**
     * Performs any further cleanup following run state transition on
     * invocation of shutdown.  A no-op here, but used by
     * ScheduledThreadPoolExecutor to cancel delayed tasks.
     *
     * 执行更进一步的清理在调用shutdown的运行状态转换之后。
     * 这里无须操作，但是被ScheduledThreadPoolExecutor用来
     * 取消延迟的任务。
     */
    void onShutdown() {
    }

    /**
     * State check needed by ScheduledThreadPoolExecutor to
     * enable running tasks during shutdown.
     *
     * ScheduledThreadPoolExecutor需要的状态检查来使在关闭的
     * 时候运行任务。
     *
     * @param shutdownOK true if should return true if SHUTDOWN
     */
    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    /**
     * Drains the task queue into a new list, normally using
     * drainTo. But if the queue is a DelayQueue or any other kind of
     * queue for which poll or drainTo may fail to remove some
     * elements, it deletes them one by one.
     *
     * 使任务队列变组织成一个新的list,通常使用drainTo。但是如果队列是
     * 一个DelayQueue或任何poll或drainTo可能移除一些元素失败的其它队列，
     * 它一个接一个地删除元素。
     */
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r))
                    taskList.add(r);
            }
        }
        return taskList;
    }

    /*
     * Methods for creating, running and cleaning up after workers
     */

    /**
     * Checks if a new worker can be added with respect to current
     * pool state and the given bound (either core or maximum). If so,
     * the worker count is adjusted accordingly, and, if possible, a
     * new worker is created and started, running firstTask as its
     * first task. This method returns false if the pool is stopped or
     * eligible to shut down. It also returns false if the thread
     * factory fails to create a thread when asked.  If the thread
     * creation fails, either due to the thread factory returning
     * null, or due to an exception (typically OutOfMemoryError in
     * Thread.start()), we roll back cleanly.
     *
     * 检查一个新的worker是否可以被加进来根据当前池的状态和给定的
     * 边界(core或maximum).如果是这样，worker数量相应地调整，并且如果
     * 可能，一个新的worker被创建和开始，运行firstTask作为它的第一个任务。
     * 如果池被停止或关闭，这个方法返回false.如果线程工厂创建线程失败
     * 也会返回false.如果线程创建失败，由于线程工厂返回null或由于
     * 一个异常(通常是Thread.start()OutOfMemoryError),我们干净地回滚。
     *
     * @param firstTask the task the new thread should run first (or
     * null if none). Workers are created with an initial first task
     * (in method execute()) to bypass queuing when there are fewer
     * than corePoolSize threads (in which case we always start one),
     * or when the queue is full (in which case we must bypass queue).
     * Initially idle threads are usually created via
     * prestartCoreThread or to replace other dying workers.
     *
     *                  新线程应该首先运行的任务(如果没有就是null).Workers
     *                  被创建带着初始化第一个任务(在方法execute()中)来绕过
     *                  排队当有小于corePoolSize的线程(这时候总是start一个)，
     *                  或者当队列满的时候(这时候我必须绕过队列)
     *                  初始化空闲的线程一般通过prestartCoreThread或替换其它死
     *                  的workers来创建。
     *
     * @param core if true use corePoolSize as bound, else
     * maximumPoolSize. (A boolean indicator is used here rather than a
     * value to ensure reads of fresh values after checking other pool
     * state).
     *             如果true使用corePoolSize作为界限，否则使用maximumPoolSize.
     *             (一个布尔指示符在这里使用而不是一个值来确保在检查其它池的状态后读到最新的值)
     * @return true if successful
     */
    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.
            if (rs >= SHUTDOWN &&
                ! (rs == SHUTDOWN &&
                   firstTask == null &&
                   ! workQueue.isEmpty()))
                return false;

            for (;;) {
                int wc = workerCountOf(c);
                if (wc >= CAPACITY ||
                    wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
                if (compareAndIncrementWorkerCount(c))
                    break retry;
                c = ctl.get();  // Re-read ctl
                if (runStateOf(c) != rs)
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop
            }
        }

        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            w = new Worker(firstTask);
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    // Recheck while holding lock.
                    // Back out on ThreadFactory failure or if
                    // shut down before lock acquired.
                    int rs = runStateOf(ctl.get());

                    if (rs < SHUTDOWN ||
                        (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) // precheck that t is startable
                            throw new IllegalThreadStateException();
                        workers.add(w);
                        int s = workers.size();
                        if (s > largestPoolSize)
                            largestPoolSize = s;
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (! workerStarted)
                addWorkerFailed(w);
        }
        return workerStarted;
    }

    /**
     * Rolls back the worker thread creation.
     * - removes worker from workers, if present
     * - decrements worker count
     * - rechecks for termination, in case the existence of this
     *   worker was holding up termination
     *
     *   回滚worker线程创建。
     *   -- 从workers中删除worker,如果存在
     *   -- 递减worker数量
     *   -- 重新检查终止，以防这个worker的退出正在持有termination.
     */
    private void addWorkerFailed(Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null)
                workers.remove(w);
            decrementWorkerCount();
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Performs cleanup and bookkeeping for a dying worker. Called
     * only from worker threads. Unless completedAbruptly is set,
     * assumes that workerCount has already been adjusted to account
     * for exit.  This method removes thread from worker set, and
     * possibly terminates the pool or replaces the worker if either
     * it exited due to user task exception or if fewer than
     * corePoolSize workers are running or queue is non-empty but
     * there are no workers.
     *
     * 为死去的worker执行清除和记账。只能从worker线程中被调用。除非设置了completedAbruptly，
     * 假设workerCount已经被调整考虑退出。这个方法从工作集合中删除线程，并且可能终止线程
     * 池或替换worker如果它由于用户任务异常而退出或如果小于corePoolSize工作者正在运行或
     * 队列不为空但是没有工作者。
     *
     * @param w the worker
     * @param completedAbruptly if the worker died due to user exception
     */
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted
            decrementWorkerCount();

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            completedTaskCount += w.completedTasks;
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }

        tryTerminate();

        int c = ctl.get();
        if (runStateLessThan(c, STOP)) {
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && ! workQueue.isEmpty())
                    min = 1;
                if (workerCountOf(c) >= min)
                    return; // replacement not needed
            }
            addWorker(null, false);
        }
    }

    /**
     * Performs blocking or timed wait for a task, depending on
     * current configuration settings, or returns null if this worker
     * must exit because of any of:
     * 1. There are more than maximumPoolSize workers (due to
     *    a call to setMaximumPoolSize).
     * 2. The pool is stopped.
     * 3. The pool is shutdown and the queue is empty.
     * 4. This worker timed out waiting for a task, and timed-out
     *    workers are subject to termination (that is,
     *    {@code allowCoreThreadTimeOut || workerCount > corePoolSize})
     *    both before and after the timed wait, and if the queue is
     *    non-empty, this worker is not the last thread in the pool.
     *
     * 执行阻塞或超时等待任务，取决于当前配置，或者返回null如果这个worker必须退出因为
     * 如下其中一个原因：
     * 1. 有多于maximumPoolSize个workers(由于调用setMaximumPoolSize).
     * 2. 线程池被停止。
     * 3. 线程池被关闭并且队列为空。
     * 4. worker等待任务的时候超时，并且超时的worker终止(也就是说，allowCoreThreadTimeOut||workerCount > corePoolSize)
     *    在超时等待之前和之后，并且如果队列不为空，这个worker不是池中的最后一个线程。
     *
     * @return task, or null if the worker must exit, in which case
     *         workerCount is decremented
     */
    private Runnable getTask() {
        boolean timedOut = false; // Did the last poll() time out?

        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }

            int wc = workerCountOf(c);

            // Are workers subject to culling?
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            if ((wc > maximumPoolSize || (timed && timedOut))
                && (wc > 1 || workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c))
                    return null;
                continue;
            }

            try {
                Runnable r = timed ?
                    workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                    workQueue.take();
                if (r != null)
                    return r;
                timedOut = true;
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }

    /**
     * Main worker run loop.  Repeatedly gets tasks from queue and
     * executes them, while coping with a number of issues:
     *
     * 主要的worker运行逻辑。不断地从队列中获取任务并且执行它们，同时应对一些问题：
     *
     * 1. We may start out with an initial task, in which case we
     * don't need to get the first one. Otherwise, as long as pool is
     * running, we get tasks from getTask. If it returns null then the
     * worker exits due to changed pool state or configuration
     * parameters.  Other exits result from exception throws in
     * external code, in which case completedAbruptly holds, which
     * usually leads processWorkerExit to replace this thread.
     *
     * 1.我们可以从最初的任务开始，这时候我们不需要获取第一个任务。否则，只要池正在
     * 运行，我们从getTask获取任务。如果返回null那么worker退出由于改变的池状态或
     * 配置参数。从外部代码抛出的异常中退出，这时候completedAbruptly持有，
     * 通常导致processWorkerExit来代替这个线程。
     *
     * 2. Before running any task, the lock is acquired to prevent
     * other pool interrupts while the task is executing, and then we
     * ensure that unless pool is stopping, this thread does not have
     * its interrupt set.
     *
     * 2. 在运行任何任务之前，锁被获取来防止在任务执行的时候其它池中断，并且然后
     * 我们确保除非池正在停止，这个线程没有设置中断。
     *
     * 3. Each task run is preceded by a call to beforeExecute, which
     * might throw an exception, in which case we cause thread to die
     * (breaking loop with completedAbruptly true) without processing
     * the task.
     *
     * 3. 每一个任务运行前都会调用beforeExecute，这可能抛出一个异常，这时候我们引起
     *    线程死亡(打断循环带着completedAbruptly为true)不处理这个任务。
     *
     * 4. Assuming beforeExecute completes normally, we run the task,
     * gathering any of its thrown exceptions to send to afterExecute.
     * We separately handle RuntimeException, Error (both of which the
     * specs guarantee that we trap) and arbitrary Throwables.
     * Because we cannot rethrow Throwables within Runnable.run, we
     * wrap them within Errors on the way out (to the thread's
     * UncaughtExceptionHandler).  Any thrown exception also
     * conservatively causes thread to die.
     *
     * 4.如果beforeExecute正常地完成，我们运行这上任务，收集任何它抛出的异常送给afterExecute。
     *   我们分别处理RuntimeException,错误(规范保证两种我们都可以捕获)和任何Throwables。
     *   因为我们不能再次抛出Throwables在Runnable.run中，我们包装他们在Errors中()
     *   任何抛出的异常也引起线程死亡。
     *
     * 5. After task.run completes, we call afterExecute, which may
     * also throw an exception, which will also cause thread to
     * die. According to JLS Sec 14.20, this exception is the one that
     * will be in effect even if task.run throws.
     *
     * 5. 在task.run完成之后，我们调用afterExecute，这也可能抛出一个异常，也可能导致
     * 线程死去。根据JLS Sec 14.20，这个异常是将会起作用的一个，即使task.run抛出异常。
     *
     * The net effect of the exception mechanics is that afterExecute
     * and the thread's UncaughtExceptionHandler have as accurate
     * information as we can provide about any problems encountered by
     * user code.
     *
     * 异常机制的效果是afterExecute和线程的UncaughtExceptionHandler具有和我们可能提供
     * 的在用户代码中遇到的任何问题一样的正确的情报。
     *
     * @param w the worker
     */
    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock(); // allow interrupts
        boolean completedAbruptly = true;
        try {
            while (task != null || (task = getTask()) != null) {
                w.lock();
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.  This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt
                if ((runStateAtLeast(ctl.get(), STOP) ||
                     (Thread.interrupted() &&
                      runStateAtLeast(ctl.get(), STOP))) &&
                    !wt.isInterrupted())
                    wt.interrupt();
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    w.completedTasks++;
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }
    }

    // Public constructors and methods

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory and rejected execution handler.
     * It may be more convenient to use one of the {@link Executors} factory
     * methods instead of this general purpose constructor.
     *
     * 用给定的初始化参数和默认的线程工厂和拒绝执行拦截器来创建一个新的ThreadPoolExecutor。
     * 用Executors的工厂方法而不是这种通用的构造函数可能更方便一些。
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     *
     *                     保持在池中的线程数量，即使它们是空闲的，除非设置了allowCoreThreadTimeOut
     *
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     *                        池中允许的最大线程数量
     *
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     *
     *                      当线程数量大于core,在空闲的超过的线程数量终止之间
     *                      等待的时间
     *
     * @param unit the time unit for the {@code keepAliveTime} argument
     *
     *             keepAliveTime参数的时间单位
     *
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     *
     *                  在任务被执行前持有任务的队列。这个队列将只持有被execute方法
     *                  提交的Runnable任务
     *
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), defaultHandler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default rejected execution handler.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             threadFactory, defaultHandler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), handler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        if (corePoolSize < 0 ||
            maximumPoolSize <= 0 ||
            maximumPoolSize < corePoolSize ||
            keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    /**
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     *
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@code RejectedExecutionHandler}.
     *
     * 执行给定的任务在不久的某个时间。任务可能在新的线程或一个存在的线程中执行。
     * 如果任务不能被提交或执行，因为这个executor已经关闭或因为已经达到最大容量了，
     * 任务被当前的RejectedExecutionHandler处理
     *
     * @param command the task to execute
     * @throws RejectedExecutionException at discretion of
     *         {@code RejectedExecutionHandler}, if the task
     *         cannot be accepted for execution
     * @throws NullPointerException if {@code command} is null
     */
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
        /*
         * Proceed in 3 steps:
         *
         * 1. If fewer than corePoolSize threads are running, try to
         * start a new thread with the given command as its first
         * task.  The call to addWorker atomically checks runState and
         * workerCount, and so prevents false alarms that would add
         * threads when it shouldn't, by returning false.
         *
         * 2. If a task can be successfully queued, then we still need
         * to double-check whether we should have added a thread
         * (because existing ones died since last checking) or that
         * the pool shut down since entry into this method. So we
         * recheck state and if necessary roll back the enqueuing if
         * stopped, or start a new thread if there are none.
         *
         * 3. If we cannot queue task, then we try to add a new
         * thread.  If it fails, we know we are shut down or saturated
         * and so reject the task.
         *
         * 用3步来处理：
         * 1. 如果正在运行的线程小于corePoolSize,试图开启一个新线程，用给定的命令作为它的
         *    第一个任务。调用addWorker自动地检查runState和workerCount,并且防止false
         *    alarm加入线程当它不能的时候，通过返回false.
         * 2. 如果一个任务可以成功地加入队列，那么我们仍然需要再次检查是否我们应该加入一个线程
         *    (因为正在退出的死掉从上一次检查之后)或者池关闭因为进入这个方法。所以我们重新检查
         *    状态并且如果必要回滚入队如果已经停止，或者开启一个新线程如果没有线程。
         * 3. 如果我们不能把任务加入队列，那么我们试图增加一个新线程。如果失败，我们知道
         *    我们正在关闭或饱和并且拒绝任务。
         *
         */
        int c = ctl.get();
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true))
                return;
            c = ctl.get();
        }
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            if (! isRunning(recheck) && remove(command))
                reject(command);
            else if (workerCountOf(recheck) == 0)
                addWorker(null, false);
        }
        else if (!addWorker(command, false))
            reject(command);
    }

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     *
     * 启动一个顺序的关闭以先前提交的任务执行的顺序，但是不再接受新的任务。
     * 如果已经关闭，再次调用不会有另外的效果。
     *
     * 这个方法不不等待先前提交的任务完成执行。如果需要提交的任务完成
     * 使用awaitTermination
     *
     * @throws SecurityException {@inheritDoc}
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(SHUTDOWN);
            interruptIdleWorkers();
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  This implementation
     * cancels tasks via {@link Thread#interrupt}, so any task that
     * fails to respond to interrupts may never terminate.
     *
     * 试图停止所有正在执行的任务，停止处理所有等待的任务，并且返回
     * 正在等待执行的list.在从这个方法返回的时候这些任务这些任务
     * 被从队列中删除。
     *
     * 这个方法不等正在执行的任务结束。如果想这样可以使用awaitTermination。
     * 不保证能够停止所有正在处理的任务。这个实现通过Thread.interupt取消
     * 任务，所以没有响应中断的任何任务可能永远不会中止。
     *
     * @throws SecurityException {@inheritDoc}
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(STOP);
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }

    public boolean isShutdown() {
        return ! isRunning(ctl.get());
    }

    /**
     * Returns true if this executor is in the process of terminating
     * after {@link #shutdown} or {@link #shutdownNow} but has not
     * completely terminated.  This method may be useful for
     * debugging. A return of {@code true} reported a sufficient
     * period after shutdown may indicate that submitted tasks have
     * ignored or suppressed interruption, causing this executor not
     * to properly terminate.
     *
     * 如果executor在shutdown或shutdownNow之后正在终止的处理中但是
     * 还没有完全终止，那么返回true。这个方法可能对调试有用。
     * 在shutdown之后一段时间后返回ture可能表示提交的任务已经忽略
     * 或阻止了中断，导致这个executor没有适当的终止。
     *
     * @return {@code true} if terminating but not yet terminated
     */
    public boolean isTerminating() {
        int c = ctl.get();
        return ! isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (;;) {
                if (runStateAtLeast(ctl.get(), TERMINATED))
                    return true;
                if (nanos <= 0)
                    return false;
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Invokes {@code shutdown} when this executor is no longer
     * referenced and it has no threads.
     *
     * 当这个executor不再被引用没有线程时，调用shutdown.
     */
    protected void finalize() {
        shutdown();
    }

    /**
     * Sets the thread factory used to create new threads.
     *
     * 设置创建线程的线程工厂。
     *
     * @param threadFactory the new thread factory
     * @throws NullPointerException if threadFactory is null
     * @see #getThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new NullPointerException();
        this.threadFactory = threadFactory;
    }

    /**
     * Returns the thread factory used to create new threads.
     *
     * 返回创建线程的线程工厂。
     * @return the current thread factory
     * @see #setThreadFactory(ThreadFactory)
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * Sets a new handler for unexecutable tasks.
     *
     * 为不能执行的任务设置新的拦截器
     *
     * @param handler the new handler
     * @throws NullPointerException if handler is null
     * @see #getRejectedExecutionHandler
     */
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;
    }

    /**
     * Returns the current handler for unexecutable tasks.
     *
     * 返回当前不可执行任务的拦截器
     *
     * @return the current handler
     * @see #setRejectedExecutionHandler(RejectedExecutionHandler)
     */
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }

    /**
     * Sets the core number of threads.  This overrides any value set
     * in the constructor.  If the new value is smaller than the
     * current value, excess existing threads will be terminated when
     * they next become idle.  If larger, new threads will, if needed,
     * be started to execute any queued tasks.
     *
     * 设置线程的core数量。这个重写任何在构造方法中的值。如果新值
     * 比当前的值小，超过的存在的线程将会终止当他们下次变为空闲的时候
     * 如果大于，如果需要新线程将会被开启来执行任务除队的任务。
     *
     * @param corePoolSize the new core size
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @see #getCorePoolSize
     */
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0)
            throw new IllegalArgumentException();
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize)
            interruptIdleWorkers();
        else if (delta > 0) {
            // We don't really know how many new threads are "needed".
            // As a heuristic, prestart enough new workers (up to new
            // core size) to handle the current number of tasks in
            // queue, but stop if queue becomes empty while doing so.
            int k = Math.min(delta, workQueue.size());
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty())
                    break;
            }
        }
    }

    /**
     * Returns the core number of threads.
     *
     * 返回线程的core数量。
     *
     * @return the core number of threads
     * @see #setCorePoolSize
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * Starts a core thread, causing it to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed. This method will return {@code false}
     * if all core threads have already been started.
     *
     * 开启一个core线程，使它空闲地等待任务。这覆盖了只在新任务被执行时
     * 开启core线程的策略。如果所有core线程已经开始了这个方法返回false.
     *
     * @return {@code true} if a thread was started
     */
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize &&
            addWorker(null, true);
    }

    /**
     * Same as prestartCoreThread except arranges that at least one
     * thread is started even if corePoolSize is 0.
     *
     * 和prestartCoreThread一样，除了至少有一个线程被开始即使corePoolSize为0
     */
    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize)
            addWorker(null, true);
        else if (wc == 0)
            addWorker(null, false);
    }

    /**
     * Starts all core threads, causing them to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed.
     *
     * 开始所有的core线程，使他们空闲地等待工作。这覆盖了只在新伤被执行时
     * 开始core线程的策略
     *
     * @return the number of threads started
     */
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))
            ++n;
        return n;
    }

    /**
     * Returns true if this pool allows core threads to time out and
     * terminate if no tasks arrive within the keepAlive time, being
     * replaced if needed when new tasks arrive. When true, the same
     * keep-alive policy applying to non-core threads applies also to
     * core threads. When false (the default), core threads are never
     * terminated due to lack of incoming tasks.
     *
     * 如果这个池允许core线程超时并且终止如果没有任务到来在keepAlive时间内
     * 返回true,当新任务到来时如果需要可以代替。当为true的时候，同样的
     * keep-alive策略应用于非core的线程，同时也应用于core线程，当为false
     * 的时候(默认)，core线程永远不会因为没有任务而终止。
     *
     * @return {@code true} if core threads are allowed to time out,
     *         else {@code false}
     *
     * @since 1.6
     */
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    /**
     * Sets the policy governing whether core threads may time out and
     * terminate if no tasks arrive within the keep-alive time, being
     * replaced if needed when new tasks arrive. When false, core
     * threads are never terminated due to lack of incoming
     * tasks. When true, the same keep-alive policy applying to
     * non-core threads applies also to core threads. To avoid
     * continual thread replacement, the keep-alive time must be
     * greater than zero when setting {@code true}. This method
     * should in general be called before the pool is actively used.
     *
     * 设置控制如果没有没任务到来时是否core线程时间和终止的策略，在新任务
     * 到来时如果需要可以代替。在为false的时候，core线程永远不会终止由于没有
     * 任务到来。当为true的时候，同样的keep-alive策略适用于非core的线程，
     * 同样应用于core线程。为了避免不断地线程切换，keep-alive时间必须
     * 大于0当设置为true的时候。这方法应该通常在池被使用之前调用
     *
     * @param value {@code true} if should time out, else {@code false}
     * @throws IllegalArgumentException if value is {@code true}
     *         and the current keep-alive time is not greater than zero
     *
     * @since 1.6
     */
    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0)
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            if (value)
                interruptIdleWorkers();
        }
    }

    /**
     * Sets the maximum allowed number of threads. This overrides any
     * value set in the constructor. If the new value is smaller than
     * the current value, excess existing threads will be
     * terminated when they next become idle.
     *
     * 设置最大线程数量。这个方法覆盖任何在构建方法设置的值。如果新值
     * 小于当前值，超过的存在的线程将终止当他们下次变得空间的时候
     *
     * @param maximumPoolSize the new maximum
     * @throws IllegalArgumentException if the new maximum is
     *         less than or equal to zero, or
     *         less than the {@linkplain #getCorePoolSize core pool size}
     * @see #getMaximumPoolSize
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize)
            interruptIdleWorkers();
    }

    /**
     * Returns the maximum allowed number of threads.
     *
     * 返回最大允许的线程数量
     *
     * @return the maximum allowed number of threads
     * @see #setMaximumPoolSize
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * Sets the time limit for which threads may remain idle before
     * being terminated.  If there are more than the core number of
     * threads currently in the pool, after waiting this amount of
     * time without processing a task, excess threads will be
     * terminated.  This overrides any value set in the constructor.
     *
     * 设置线程在终止前可以保持空闲的时间。如果有多于core个的线程，
     * 在没有处理任务等待这个时间后，超过的线程将被终止。这覆盖
     * 任何在构造方法中设置的值。
     * @param time the time to wait.  A time value of zero will cause
     *        excess threads to terminate immediately after executing tasks.
     * @param unit the time unit of the {@code time} argument
     * @throws IllegalArgumentException if {@code time} less than zero or
     *         if {@code time} is zero and {@code allowsCoreThreadTimeOut}
     * @see #getKeepAliveTime(TimeUnit)
     */
    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0)
            throw new IllegalArgumentException();
        if (time == 0 && allowsCoreThreadTimeOut())
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if (delta < 0)
            interruptIdleWorkers();
    }

    /**
     * Returns the thread keep-alive time, which is the amount of time
     * that threads in excess of the core pool size may remain
     * idle before being terminated.
     *
     * 返回线程的keep-alive时间，
     *
     * @param unit the desired time unit of the result
     * @return the time limit
     * @see #setKeepAliveTime(long, TimeUnit)
     */
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    /* User-level queue utilities */

    /**
     * Returns the task queue used by this executor. Access to the
     * task queue is intended primarily for debugging and monitoring.
     * This queue may be in active use.  Retrieving the task queue
     * does not prevent queued tasks from executing.
     *
     * 返回这个executor使用的任务队列。访问任务队列主要用来调试和监控。
     * 这个队列可能正在使用。检索任务任务队列不会阻止队列中的任务执行。
     *
     * @return the task queue
     */
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /**
     * Removes this task from the executor's internal queue if it is
     * present, thus causing it not to be run if it has not already
     * started.
     *
     * <p>This method may be useful as one part of a cancellation
     * scheme.  It may fail to remove tasks that have been converted
     * into other forms before being placed on the internal queue. For
     * example, a task entered using {@code submit} might be
     * converted into a form that maintains {@code Future} status.
     * However, in such cases, method {@link #purge} may be used to
     * remove those Futures that have been cancelled.
     *
     * 从executor的内部队列中移除这个任务，因为导致它不会执行如果它还没有执行。
     * 这个方法可能在取消中有用。这可能失败在进入内部队列之前这个任务已经转换为
     * 其它形式。例如，一个被提前的任务可以转换为保持Future状态的形式。
     * 但是这种情况下，可以使用purge方法来移除已经被取消的future.
     *
     * @param task the task to remove
     * @return {@code true} if the task was removed
     */
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty
        return removed;
    }

    /**
     * Tries to remove from the work queue all {@link Future}
     * tasks that have been cancelled. This method can be useful as a
     * storage reclamation operation, that has no other impact on
     * functionality. Cancelled tasks are never executed, but may
     * accumulate in work queues until worker threads can actively
     * remove them. Invoking this method instead tries to remove them now.
     * However, this method may fail to remove tasks in
     * the presence of interference by other threads.
     *
     * 试图删除队列中已经被取消的Future.这个方法作为存储回收操作很有用，
     * 也没有其它影响对功能性。取消的任务永远不会被执行，但是可能在队列中
     * 累加直到线程删除它们。调用这个方法试图现在删除它们。但是，这个方法可能
     * 失败由于其它线程的干扰。
     */
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    it.remove();
            }
        } catch (ConcurrentModificationException fallThrough) {
            // Take slow path if we encounter interference during traversal.
            // Make copy for traversal and call remove for cancelled entries.
            // The slow path is more likely to be O(N*N).
            for (Object r : q.toArray())
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    q.remove(r);
        }

        tryTerminate(); // In case SHUTDOWN and now empty
    }

    /* Statistics */
    //统计


    /**
     * Returns the current number of threads in the pool.
     *
     * 返回池中的线程数量
     *
     * @return the number of threads
     */
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // Remove rare and surprising possibility of
            // isTerminated() && getPoolSize() > 0
            return runStateAtLeast(ctl.get(), TIDYING) ? 0
                : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate number of threads that are actively
     * executing tasks.
     *
     * 返回池中正在执行任务的估计。
     *
     * @return the number of threads
     */
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked())
                    ++n;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the largest number of threads that have ever
     * simultaneously been in the pool.
     *
     * 返回池中曾经同时存在的线程最大数量
     * @return the number of threads
     */
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate total number of tasks that have ever been
     * scheduled for execution. Because the states of tasks and
     * threads may change dynamically during computation, the returned
     * value is only an approximation.
     *
     * 返回曾经被调度执行的任务数量。因为计算的过程中线程和任务的状态会动态
     * 地改变，返回的值只是估计值
     * @return the number of tasks
     */
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked())
                    ++n;
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate total number of tasks that have
     * completed execution. Because the states of tasks and threads
     * may change dynamically during computation, the returned value
     * is only an approximation, but one that does not ever decrease
     * across successive calls.
     *
     * 返回已经完成执行的数量。因为任务和线程的状态会动态地改变，返回的值
     * 只是一个大概值，但是不是减小。
     * @return the number of tasks
     */
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers)
                n += w.completedTasks;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns a string identifying this pool, as well as its state,
     * including indications of run state and estimated worker and
     * task counts.
     *
     * @return a string identifying this pool, as well as its state
     */
    public String toString() {
        long ncompleted;
        int nworkers, nactive;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked())
                    ++nactive;
            }
        } finally {
            mainLock.unlock();
        }
        int c = ctl.get();
        String rs = (runStateLessThan(c, SHUTDOWN) ? "Running" :
                     (runStateAtLeast(c, TERMINATED) ? "Terminated" :
                      "Shutting down"));
        return super.toString() +
            "[" + rs +
            ", pool size = " + nworkers +
            ", active threads = " + nactive +
            ", queued tasks = " + workQueue.size() +
            ", completed tasks = " + ncompleted +
            "]";
    }

    /* Extension hooks */

    /**
     * Method invoked prior to executing the given Runnable in the
     * given thread.  This method is invoked by thread {@code t} that
     * will execute task {@code r}, and may be used to re-initialize
     * ThreadLocals, or to perform logging.
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.beforeExecute} at the end of
     * this method.
     *
     * 在给定的线程中执行给定的Runnable之前调用这个方法。这个方法被线程
     * t调用，并且可能 被用来重新初始化ThreadLocals或者执行日志。
     *
     * 这个实现 不做任何事，但是可以在子类中自定义。注意：为了适当地多个
     * 重写，子类通常应该调用super.beforeExecute在这个方法的最后。
     * @param t the thread that will run task {@code r}
     * @param r the task that will be executed
     */
    protected void beforeExecute(Thread t, Runnable r) { }

    /**
     * Method invoked upon completion of execution of the given Runnable.
     * This method is invoked by the thread that executed the task. If
     * non-null, the Throwable is the uncaught {@code RuntimeException}
     * or {@code Error} that caused execution to terminate abruptly.
     *
     * 在给定的Runnable的执行完成时调用的方法。这个方法被执行任务的线程
     * 调用。如果非null,Throwable是非捕获的RuntimeException或Error引起执行
     * 突然中断。
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.afterExecute} at the
     * beginning of this method.
     *
     * 这个实现不做任何事，但是可以在子类中自定义。注意：为了适当地
     * 嵌套多个重写，子类通常应该调用super.afterExecute在这个方法的前面。
     *
     * <p><b>Note:</b> When actions are enclosed in tasks (such as
     * {@link FutureTask}) either explicitly or via methods such as
     * {@code submit}, these task objects catch and maintain
     * computational exceptions, and so they do not cause abrupt
     * termination, and the internal exceptions are <em>not</em>
     * passed to this method. If you would like to trap both kinds of
     * failures in this method, you can further probe for such cases,
     * as in this sample subclass that prints either the direct cause
     * or the underlying exception if a task has been aborted:
     *
     * 注意：当动作电电显式地或通过例如submit的方法封装在任务(例如FutureTask)
     * ，这些任务对象捕获和维护计算的异常，并且所以它们不捕获突然地终止，
     * 和被没有被传给这个方法的内部导演。如果 你想捕获两种失败在这个方法中，
     * 你可以进一步地刺探这样的情况，就像这个子类的例子打印直接的cause或底层的
     * 异常，如果一个任务已经被终止。
     *  <pre> {@code
     * class ExtendedExecutor extends ThreadPoolExecutor {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);
     *     if (t == null && r instanceof Future<?>) {
     *       try {
     *         Object result = ((Future<?>) r).get();
     *       } catch (CancellationException ce) {
     *           t = ce;
     *       } catch (ExecutionException ee) {
     *           t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *           Thread.currentThread().interrupt(); // ignore/reset
     *       }
     *     }
     *     if (t != null)
     *       System.out.println(t);
     *   }
     * }}</pre>
     *
     * @param r the runnable that has completed
     * @param t the exception that caused termination, or null if
     * execution completed normally
     */
    protected void afterExecute(Runnable r, Throwable t) { }

    /**
     * Method invoked when the Executor has terminated.  Default
     * implementation does nothing. Note: To properly nest multiple
     * overridings, subclasses should generally invoke
     * {@code super.terminated} within this method.
     *
     * 当executor已经被终止时调用的方法。默认的实现 不做任何事。
     * 注意：为了适当地嵌套重写，子类通常应该调用super.terminated在
     * 这个方法中。
     */
    protected void terminated() { }

    /* Predefined RejectedExecutionHandlers */

    /**
     * A handler for rejected tasks that runs the rejected task
     * directly in the calling thread of the {@code execute} method,
     * unless the executor has been shut down, in which case the task
     * is discarded.
     *
     * 为拒绝的任务使用的拦截器
     */
    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code CallerRunsPolicy}.
         */
        public CallerRunsPolicy() { }

        /**
         * Executes task r in the caller's thread, unless the executor
         * has been shut down, in which case the task is discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    /**
     * A handler for rejected tasks that throws a
     * {@code RejectedExecutionException}.
     */
    public static class AbortPolicy implements RejectedExecutionHandler {
        /**
         * Creates an {@code AbortPolicy}.
         */
        public AbortPolicy() { }

        /**
         * Always throws RejectedExecutionException.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         * @throws RejectedExecutionException always
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() +
                                                 " rejected from " +
                                                 e.toString());
        }
    }

    /**
     * A handler for rejected tasks that silently discards the
     * rejected task.
     */
    public static class DiscardPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardPolicy}.
         */
        public DiscardPolicy() { }

        /**
         * Does nothing, which has the effect of discarding task r.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }

    /**
     * A handler for rejected tasks that discards the oldest unhandled
     * request and then retries {@code execute}, unless the executor
     * is shut down, in which case the task is discarded.
     */
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardOldestPolicy} for the given executor.
         */
        public DiscardOldestPolicy() { }

        /**
         * Obtains and ignores the next task that the executor
         * would otherwise execute, if one is immediately available,
         * and then retries execution of task r, unless the executor
         * is shut down, in which case task r is instead discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }
}
