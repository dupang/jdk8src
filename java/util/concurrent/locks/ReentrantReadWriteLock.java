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
import java.util.Collection;

/**
 * An implementation of {@link ReadWriteLock} supporting similar
 * semantics to {@link ReentrantLock}.
 *
 * ReadWriteLock的实现，支持跟ReentrantLock类似的语义。
 *
 * <p>This class has the following properties:
 *
 * 这个类有以下的属性。
 *
 * <ul>
 * <li><b>Acquisition order</b>
 *
 * 获取顺序
 *
 * <p>This class does not impose a reader or writer preference
 * ordering for lock access.  However, it does support an optional
 * <em>fairness</em> policy.
 *
 * 这个类对锁的访问没有读或写的优先顺序。但是，它确实支持可选的
 * 公平策略。
 *
 * <dl>
 * <dt><b><i>Non-fair mode (default)</i></b>
 * <dd>When constructed as non-fair (the default), the order of entry
 * to the read and write lock is unspecified, subject to reentrancy
 * constraints.  A nonfair lock that is continuously contended may
 * indefinitely postpone one or more reader or writer threads, but
 * will normally have higher throughput than a fair lock.
 *
 * 默认的是非公平的模式
 * 当以非公平的方式构建的时候，并没有指定进入读写锁的顺序，受制于重入约束。
 * 一个不停的竞争的非公平的锁可能无限制的推迟一个或更多的读或写的线程，
 * 但是通常具有比公平锁更高的吞吐量
 *
 * <dt><b><i>Fair mode</i></b>
 * <dd>When constructed as fair, threads contend for entry using an
 * approximately arrival-order policy. When the currently held lock
 * is released, either the longest-waiting single writer thread will
 * be assigned the write lock, or if there is a group of reader threads
 * waiting longer than all waiting writer threads, that group will be
 * assigned the read lock.
 * 公平模式
 * 当以公平方式构建时，线程使用一个先后顺序的策略来竞争。当当前的线程
 * 持有的锁释放了，等待时间最长的单个写线程将获取写锁，或者如果有一组读
 * 线程比所有的写线程待的时候都长，这组读线程将获取这个锁。
 *
 * <p>A thread that tries to acquire a fair read lock (non-reentrantly)
 * will block if either the write lock is held, or there is a waiting
 * writer thread. The thread will not acquire the read lock until
 * after the oldest currently waiting writer thread has acquired and
 * released the write lock. Of course, if a waiting writer abandons
 * its wait, leaving one or more reader threads as the longest waiters
 * in the queue with the write lock free, then those readers will be
 * assigned the read lock.
 *
 * 如果写锁被别的线程持有，或者有一个正在等待的写线程，
 * 一个线程试图获取一个公平的读锁(非重入的)将被阻塞。直到等待时间最长的
 * 当前的写线程获取并释放了写锁，这个线程才会获取到读锁。当然，如果一个
 * 等待写线程放弃等待，在队列中等待时间最长的没有写锁竞争的一个或多个读线程
 * 将会获取读锁。
 *
 * <p>A thread that tries to acquire a fair write lock (non-reentrantly)
 * will block unless both the read lock and write lock are free (which
 * implies there are no waiting threads).  (Note that the non-blocking
 * {@link ReadLock#tryLock()} and {@link WriteLock#tryLock()} methods
 * do not honor this fair setting and will immediately acquire the lock
 * if it is possible, regardless of waiting threads.)
 * <p>
 * </dl>
 *
 * 一个线程试图获取一个公平的写锁(不可重入的)将会阻塞，除非读锁和写锁都
 * 被释放了（也就是没有等待的线程）。(注意非阻塞的ReadLock.tryLock()和WriteLock.
 * tryLock()不管这个公平策略，并且将立刻获取锁，如果可能的话，不管是否有正在等待的
 * 线程)
 *
 * <li><b>Reentrancy</b>
 *
 * 可重入性
 *
 * <p>This lock allows both readers and writers to reacquire read or
 * write locks in the style of a {@link ReentrantLock}. Non-reentrant
 * readers are not allowed until all write locks held by the writing
 * thread have been released.
 *
 * 这个锁允许读和写像ReentrantLock那样获取读或写锁。不可重入的读不被
 * 允许只到所有的被写线程持有的写锁全部释放了。
 *
 * <p>Additionally, a writer can acquire the read lock, but not
 * vice-versa.  Among other applications, reentrancy can be useful
 * when write locks are held during calls or callbacks to methods that
 * perform reads under read locks.  If a reader tries to acquire the
 * write lock it will never succeed.
 *
 * 另外，一个写线程可以获取读锁，但反过来却不行。在其它程序中，
 * 可重入性可能有用当调用时写锁被持有或在读锁下回调执行读操作。如果一个读线程试图获取
 * 写锁，它将不会成功。
 *
 * <li><b>Lock downgrading</b>
 * <p>Reentrancy also allows downgrading from the write lock to a read lock,
 * by acquiring the write lock, then the read lock and then releasing the
 * write lock. However, upgrading from a read lock to the write lock is
 * <b>not</b> possible.
 *
 * 锁降级
 * 可重入性也允许从写锁降级到读锁，通过获取写锁，然后获取读锁，然后释放写锁。
 * 但是从读锁升级到写锁是不可能的。
 *
 * <li><b>Interruption of lock acquisition</b>
 * <p>The read lock and write lock both support interruption during lock
 * acquisition.
 *
 * 锁获取的中断。读锁和写锁都支持获取锁的过程中中断。
 *
 *
 * <li><b>{@link Condition} support</b>
 * <p>The write lock provides a {@link Condition} implementation that
 * behaves in the same way, with respect to the write lock, as the
 * {@link Condition} implementation provided by
 * {@link ReentrantLock#newCondition} does for {@link ReentrantLock}.
 * This {@link Condition} can, of course, only be used with the write lock.
 *
 * 条件支持
 * 写锁提供了一个Condition实现，它的行为和为reentrantLock提供的ReentrantLock.newCondition是一样的。
 * 这个条件当然只能被写锁使用。
 *
 * <p>The read lock does not support a {@link Condition} and
 * {@code readLock().newCondition()} throws
 * {@code UnsupportedOperationException}.
 *
 * 写锁不直持条件，并且readLock().newCondition()会抛出异常。
 *
 * <li><b>Instrumentation</b>
 * <p>This class supports methods to determine whether locks
 * are held or contended. These methods are designed for monitoring
 * system state, not for synchronization control.
 * </ul>
 *
 * Instrumentation(监控)
 * 这个类提供了几个方法，这个方法可能检查锁是否被持有或竞争。这个方法被
 * 设计用来监控系统状态，而不是为了同步控制。
 *
 * <p>Serialization of this class behaves in the same way as built-in
 * locks: a deserialized lock is in the unlocked state, regardless of
 * its state when serialized.
 *
 * 这个类的序列化行为跟内置锁的一样：一个锁的反序列化处于非锁住的状态，
 * 不管序列化的时候是什么状态。
 *
 * <p><b>Sample usages</b>. Here is a code sketch showing how to perform
 * lock downgrading after updating a cache (exception handling is
 * particularly tricky when handling multiple locks in a non-nested
 * fashion):
 *
 * <pre> {@code
 * class CachedData {
 *   Object data;
 *   volatile boolean cacheValid;
 *   final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *
 *   void processCachedData() {
 *     rwl.readLock().lock();
 *     if (!cacheValid) {
 *       // Must release read lock before acquiring write lock
 *       rwl.readLock().unlock();
 *       rwl.writeLock().lock();
 *       try {
 *         // Recheck state because another thread might have
 *         // acquired write lock and changed state before we did.
 *         if (!cacheValid) {
 *           data = ...
 *           cacheValid = true;
 *         }
 *         // Downgrade by acquiring read lock before releasing write lock
 *         rwl.readLock().lock();
 *       } finally {
 *         rwl.writeLock().unlock(); // Unlock write, still hold read
 *       }
 *     }
 *
 *     try {
 *       use(data);
 *     } finally {
 *       rwl.readLock().unlock();
 *     }
 *   }
 * }}</pre>
 *
 * ReentrantReadWriteLocks can be used to improve concurrency in some
 * uses of some kinds of Collections. This is typically worthwhile
 * only when the collections are expected to be large, accessed by
 * more reader threads than writer threads, and entail operations with
 * overhead that outweighs synchronization overhead. For example, here
 * is a class using a TreeMap that is expected to be large and
 * concurrently accessed.
 *
 *  <pre> {@code
 * class RWDictionary {
 *   private final Map<String, Data> m = new TreeMap<String, Data>();
 *   private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *   private final Lock r = rwl.readLock();
 *   private final Lock w = rwl.writeLock();
 *
 *   public Data get(String key) {
 *     r.lock();
 *     try { return m.get(key); }
 *     finally { r.unlock(); }
 *   }
 *   public String[] allKeys() {
 *     r.lock();
 *     try { return m.keySet().toArray(); }
 *     finally { r.unlock(); }
 *   }
 *   public Data put(String key, Data value) {
 *     w.lock();
 *     try { return m.put(key, value); }
 *     finally { w.unlock(); }
 *   }
 *   public void clear() {
 *     w.lock();
 *     try { m.clear(); }
 *     finally { w.unlock(); }
 *   }
 * }}</pre>
 *
 * <h3>Implementation Notes</h3>
 *
 * <p>This lock supports a maximum of 65535 recursive write locks
 * and 65535 read locks. Attempts to exceed these limits result in
 * {@link Error} throws from locking methods.
 *
 * 这个锁支持最大数65535的递归写锁和65535个读锁。读图超过这个限制将
 * 抛出异常。
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantReadWriteLock
        implements ReadWriteLock, java.io.Serializable {
    private static final long serialVersionUID = -6992448646407690164L;
    /** Inner class providing readlock */
    //提供读锁的内部类
    private final ReentrantReadWriteLock.ReadLock readerLock;
    /** Inner class providing writelock */
    //提供写锁的内部类
    private final ReentrantReadWriteLock.WriteLock writerLock;
    /** Performs all synchronization mechanics */
    //执行所有的同步机制
    final Sync sync;

    /**
     * Creates a new {@code ReentrantReadWriteLock} with
     * default (nonfair) ordering properties.
     * 创建一个非公平的新的ReentrantReadWriteLock
     */
    public ReentrantReadWriteLock() {
        this(false);
    }

    /**
     * Creates a new {@code ReentrantReadWriteLock} with
     * the given fairness policy.
     * 用一个给定的公平策略创建一个ReentrantReadWriteLock
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    public ReentrantReadWriteLock.WriteLock writeLock() { return writerLock; }
    public ReentrantReadWriteLock.ReadLock  readLock()  { return readerLock; }

    /**
     * Synchronization implementation for ReentrantReadWriteLock.
     * Subclassed into fair and nonfair versions.
     *
     * 为ReentrantReadWriteLock提供的同步实现类
     * 子类细分为公平和非公平的版本
     *
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 6317671515068378041L;

        /*
         * Read vs write count extraction constants and functions.
         * Lock state is logically divided into two unsigned shorts:
         * The lower one representing the exclusive (writer) lock hold count,
         * and the upper the shared (reader) hold count.
         * 读写数据提取的常量和函数
         * 锁状态逻辑上分为两个无符号的short
         * 小的表示独占锁的持有数量，并且大的表示共享锁持有的数量
         */

        static final int SHARED_SHIFT   = 16;
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

        /** Returns the number of shared holds represented in count  */
        //返回代表持有共享锁的数量
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
        /** Returns the number of exclusive holds represented in count  */
        //返回代表持有独占锁的数量
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }

        /**
         * A counter for per-thread read hold counts.
         * Maintained as a ThreadLocal; cached in cachedHoldCounter
         * 一个记录每一个线程读锁持有的数量的计数器。以ThreadLocal的方式维护
         * 缓存在cachedHoldCounter.
         */
        static final class HoldCounter {
            int count = 0;
            // Use id, not reference, to avoid garbage retention
            // 使用id 而不是引用，来避免垃圾滞留
            final long tid = getThreadId(Thread.currentThread());
        }

        /**
         * ThreadLocal subclass. Easiest to explicitly define for sake
         * of deserialization mechanics.
         * ThreadLocal的子类，为了反序列化机制最高简单的显示定义
         */
        static final class ThreadLocalHoldCounter
            extends ThreadLocal<HoldCounter> {
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        /**
         * The number of reentrant read locks held by current thread.
         * Initialized only in constructor and readObject.
         * Removed whenever a thread's read hold count drops to 0.
         * 被当前线程持有的可重入的读锁的数量。只在构建器中和反序列化在被初始化。
         * 在线程持有读锁数量为0的时候被移除。
         */
        private transient ThreadLocalHoldCounter readHolds;

        /**
         * The hold count of the last thread to successfully acquire
         * readLock. This saves ThreadLocal lookup in the common case
         * where the next thread to release is the last one to
         * acquire. This is non-volatile since it is just used
         * as a heuristic, and would be great for threads to cache.
         *
         * <p>Can outlive the Thread for which it is caching the read
         * hold count, but avoids garbage retention by not retaining a
         * reference to the Thread.
         *
         * <p>Accessed via a benign data race; relies on the memory
         * model's final field and out-of-thin-air guarantees.
         *
         * 最近的线程成功获取到的读锁的数量。这节省了ThreadLocal在下一个线程要释放的锁是
         * 最后一个线程要获取的情况下的查找。这是non-volatile因为它公用来作为启发，
         * 并且可以很好的被缓存。
         *
         * 可以比线程存活时间更长，因为它缓存了读锁数量，但是通过没有保留线程的引用来
         * 避免垃圾滞留，、
         *
         * 通过良性的数据竞争来访问，依赖于内在模型的final字段和out-of-thin-air保证。
         *
         */
        private transient HoldCounter cachedHoldCounter;

        /**
         * firstReader is the first thread to have acquired the read lock.
         * firstReaderHoldCount is firstReader's hold count.
         *
         * <p>More precisely, firstReader is the unique thread that last
         * changed the shared count from 0 to 1, and has not released the
         * read lock since then; null if there is no such thread.
         *
         * <p>Cannot cause garbage retention unless the thread terminated
         * without relinquishing its read locks, since tryReleaseShared
         * sets it to null.
         *
         * <p>Accessed via a benign data race; relies on the memory
         * model's out-of-thin-air guarantees for references.
         *
         * <p>This allows tracking of read holds for uncontended read
         * locks to be very cheap.
         *
         * firstReader是第一个获取读锁的线程。firstReaderHoldCount是firstReader的持有数量。
         *
         * 更确切的说，firstReader是最后改变共享数量从0到1到的唯一线程，并且从那时候起还没有释放读锁，
         * 如果没有这样的线程，这个字段值就是null.
         *
         * 通过良性的数据竞争来访问，依赖于内在模型的final字段和out-of-thin-air保证。
         *
         * 这使得追踪非竞争的读锁持有数量非常容易。
         */
        private transient Thread firstReader = null;
        private transient int firstReaderHoldCount;

        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // ensures visibility of readHolds
        }

        /*
         * Acquires and releases use the same code for fair and
         * nonfair locks, but differ in whether/how they allow barging
         * when queues are non-empty.
         * 公平和非公平获取和释放使用相同的代码，但是不同是当队列不为空的时候，他们怎么
         * 进入竞争。
         */

        /**
         * Returns true if the current thread, when trying to acquire
         * the read lock, and otherwise eligible to do so, should block
         * because of policy for overtaking other waiting threads.
         * 当当前线程试图获取读锁的时候，其它线程有资格获取。这时候返回true.
         * 应该阻塞是因为超越其它等待线程的策略。
         */
        abstract boolean readerShouldBlock();

        /**
         * Returns true if the current thread, when trying to acquire
         * the write lock, and otherwise eligible to do so, should block
         * because of policy for overtaking other waiting threads.
         */
        abstract boolean writerShouldBlock();

        /*
         * Note that tryRelease and tryAcquire can be called by
         * Conditions. So it is possible that their arguments contain
         * both read and write holds that are all released during a
         * condition wait and re-established in tryAcquire.
         * 注意tryRelease和tryAcquire可以被Conditions调用。所以可能他们包含的
         * 读和写的参数都被释放了在等待条件的时候和在tryAcquire重新建立的时候。
         */

        protected final boolean tryRelease(int releases) {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int nextc = getState() - releases;
            boolean free = exclusiveCount(nextc) == 0;
            if (free)
                setExclusiveOwnerThread(null);
            setState(nextc);
            return free;
        }

        protected final boolean tryAcquire(int acquires) {
            /*
             * Walkthrough:
             * 1. If read count nonzero or write count nonzero
             *    and owner is a different thread, fail.
             * 2. If count would saturate, fail. (This can only
             *    happen if count is already nonzero.)
             * 3. Otherwise, this thread is eligible for lock if
             *    it is either a reentrant acquire or
             *    queue policy allows it. If so, update state
             *    and set owner.
             *
             * 1. 如果读数量不是0中写数量不为0并且锁的持有者不是当前线程，失败。
             * 2. 如果数量超过最大值，失败，(这只能在数量不是0的时候发生)
             * 3. 否则，这个线程有资格获取锁，如果这是一个可重入的获取或排除策略允许。
             *    如果是这样，更新状态值并且设置锁的持有者
             */
            Thread current = Thread.currentThread();
            int c = getState();
            int w = exclusiveCount(c);
            if (c != 0) {
                // (Note: if c != 0 and w == 0 then shared count != 0)
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w + exclusiveCount(acquires) > MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                // Reentrant acquire
                setState(c + acquires);
                return true;
            }
            if (writerShouldBlock() ||
                !compareAndSetState(c, c + acquires))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();
            if (firstReader == current) {
                // assert firstReaderHoldCount > 0;
                if (firstReaderHoldCount == 1)
                    firstReader = null;
                else
                    firstReaderHoldCount--;
            } else {
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    rh = readHolds.get();
                int count = rh.count;
                if (count <= 1) {
                    readHolds.remove();
                    if (count <= 0)
                        throw unmatchedUnlockException();
                }
                --rh.count;
            }
            for (;;) {
                int c = getState();
                int nextc = c - SHARED_UNIT;
                if (compareAndSetState(c, nextc))
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    return nextc == 0;
            }
        }

        private IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException(
                "attempt to unlock read lock, not locked by current thread");
        }

        protected final int tryAcquireShared(int unused) {
            /*
             * Walkthrough:
             * 1. If write lock held by another thread, fail.
             * 2. Otherwise, this thread is eligible for
             *    lock wrt state, so ask if it should block
             *    because of queue policy. If not, try
             *    to grant by CASing state and updating count.
             *    Note that step does not check for reentrant
             *    acquires, which is postponed to full version
             *    to avoid having to check hold count in
             *    the more typical non-reentrant case.
             * 3. If step 2 fails either because thread
             *    apparently not eligible or CAS fails or count
             *    saturated, chain to version with full retry loop.
             * 1. 如果写锁被其它线程持有，失败。
             * 2. 否则，这个线程有资格获取锁，所以根据排除策略判断是否应试被
             *    阻塞。如果不阻塞，试着获取锁并且更新持有数量。注意这一步没有
             *    检查可重入获取，这一步推迟到了全部版本，为了避免不得不检查
             *    在更典型的非重入的情况下的持有数量
             * 3. 如果第二步失败，因为线程没有资格或CAS失败，或数量越界，
             *    就进入到full retryloop.
             */
            Thread current = Thread.currentThread();
            int c = getState();
            if (exclusiveCount(c) != 0 &&
                getExclusiveOwnerThread() != current)
                return -1;
            int r = sharedCount(c);
            if (!readerShouldBlock() &&
                r < MAX_COUNT &&
                compareAndSetState(c, c + SHARED_UNIT)) {
                if (r == 0) {
                    firstReader = current;
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {
                    firstReaderHoldCount++;
                } else {
                    HoldCounter rh = cachedHoldCounter;
                    if (rh == null || rh.tid != getThreadId(current))
                        cachedHoldCounter = rh = readHolds.get();
                    else if (rh.count == 0)
                        readHolds.set(rh);
                    rh.count++;
                }
                return 1;
            }
            return fullTryAcquireShared(current);
        }

        /**
         * Full version of acquire for reads, that handles CAS misses
         * and reentrant reads not dealt with in tryAcquireShared.
         *
         * 获取读锁的完全版本，这个版本里处理了在tryAcquireShared版本里没有CAS失败和重入
         */
        final int fullTryAcquireShared(Thread current) {
            /*
             * This code is in part redundant with that in
             * tryAcquireShared but is simpler overall by not
             * complicating tryAcquireShared with interactions between
             * retries and lazily reading hold counts.
             */
            HoldCounter rh = null;
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0) {
                    if (getExclusiveOwnerThread() != current)
                        return -1;
                    // else we hold the exclusive lock; blocking here
                    // would cause deadlock.
                } else if (readerShouldBlock()) {
                    // Make sure we're not acquiring read lock reentrantly
                    if (firstReader == current) {
                        // assert firstReaderHoldCount > 0;
                    } else {
                        if (rh == null) {
                            rh = cachedHoldCounter;
                            if (rh == null || rh.tid != getThreadId(current)) {
                                rh = readHolds.get();
                                if (rh.count == 0)
                                    readHolds.remove();
                            }
                        }
                        if (rh.count == 0)
                            return -1;
                    }
                }
                if (sharedCount(c) == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (sharedCount(c) == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        if (rh == null)
                            rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                        cachedHoldCounter = rh; // cache for release
                    }
                    return 1;
                }
            }
        }

        /**
         * Performs tryLock for write, enabling barging in both modes.
         * This is identical in effect to tryAcquire except for lack
         * of calls to writerShouldBlock.
         * 为获取写锁操作tryLock操作，支持强入在两种模式下(公平和朝非公平)。
         * 除了没有调用writerShouldBlock方法，这个和tryAcquire是一样的。1
         */
        final boolean tryWriteLock() {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c != 0) {
                int w = exclusiveCount(c);
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
            }
            if (!compareAndSetState(c, c + 1))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        /**
         * Performs tryLock for read, enabling barging in both modes.
         * This is identical in effect to tryAcquireShared except for
         * lack of calls to readerShouldBlock.
         * 为获取读锁操作tryLock操作，支持强入在两种模式下(公平和朝非公平)。
         * 除了没有调用readerShouldBlock方法，这个和tryAcquireShared是一样的。
         */
        final boolean tryReadLock() {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0 &&
                    getExclusiveOwnerThread() != current)
                    return false;
                int r = sharedCount(c);
                if (r == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        HoldCounter rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            cachedHoldCounter = rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                    }
                    return true;
                }
            }
        }

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // Methods relayed to outer class

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        final Thread getOwner() {
            // Must read state before owner to ensure memory consistency
            return ((exclusiveCount(getState()) == 0) ?
                    null :
                    getExclusiveOwnerThread());
        }

        final int getReadLockCount() {
            return sharedCount(getState());
        }

        final boolean isWriteLocked() {
            return exclusiveCount(getState()) != 0;
        }

        final int getWriteHoldCount() {
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
        }

        final int getReadHoldCount() {
            if (getReadLockCount() == 0)
                return 0;

            Thread current = Thread.currentThread();
            if (firstReader == current)
                return firstReaderHoldCount;

            HoldCounter rh = cachedHoldCounter;
            if (rh != null && rh.tid == getThreadId(current))
                return rh.count;

            int count = readHolds.get().count;
            if (count == 0) readHolds.remove();
            return count;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            readHolds = new ThreadLocalHoldCounter();
            setState(0); // reset to unlocked state
        }

        final int getCount() { return getState(); }
    }

    /**
     * Nonfair version of Sync
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -8159625535654395037L;
        final boolean writerShouldBlock() {
            return false; // writers can always barge
        }
        final boolean readerShouldBlock() {
            /* As a heuristic to avoid indefinite writer starvation,
             * block if the thread that momentarily appears to be head
             * of queue, if one exists, is a waiting writer.  This is
             * only a probabilistic effect since a new reader will not
             * block if there is a waiting writer behind other enabled
             * readers that have not yet drained from the queue.
             *
             * 作为一个避免无限写锁饥饿的启发式，如果线程瞬间出现在队列的头，如果这个线程是
             * 一个等待的写线程。这只是一个概率效应，因为如果有一个等待的写线程在其它在还队列中的
             * 读线程后面，读线程就不会阻塞。
             *
             */
            return apparentlyFirstQueuedIsExclusive();
        }
    }

    /**
     * Fair version of Sync
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -2274990926593161451L;
        final boolean writerShouldBlock() {
            return hasQueuedPredecessors();
        }
        final boolean readerShouldBlock() {
            return hasQueuedPredecessors();
        }
    }

    /**
     * The lock returned by method {@link ReentrantReadWriteLock#readLock}.
     * ReentrantReadWriteLock.readLock的方法返回的锁。
     */
    public static class ReadLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -5992448646407690164L;
        private final Sync sync;

        /**
         * Constructor for use by subclasses
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * Acquires the read lock.
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately.
         *
         * <p>If the write lock is held by another thread then
         * the current thread becomes disabled for thread scheduling
         * purposes and lies dormant until the read lock has been acquired.
         *
         * 获取读锁。
         * 获取读锁如果写锁没有被其它线程持有，并且立刻返回。
         * 如果写锁被其它线程持有，那么当前线程对于线程调度变得不可用，直接读锁被获取到。
         */
        public void lock() {
            sync.acquireShared(1);
        }

        /**
         * Acquires the read lock unless the current thread is
         * {@linkplain Thread#interrupt interrupted}.
         * 获取读锁除非当前线程被中断了。
         *
         * <p>Acquires the read lock if the write lock is not held
         * by another thread and returns immediately.
         *
         * 获取读锁如果写锁没有被其它线程持有，并且立刻返回。
         *
         * <p>If the write lock is held by another thread then the
         * current thread becomes disabled for thread scheduling
         * purposes and lies dormant until one of two things happens:
         *
         * 如果写锁被其它线程持有，那么当前线程对于线程调度变得不可用，直接下面两种情况中
         * 的一个发生。
         * 1. 读锁被当前线程获取;或者其它线程中断了当前线程。
         * <ul>
         *
         * <li>The read lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread.
         *
         * </ul>
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method; or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the read lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * 如果当前线程：
         * 设置了中断状态当进入这个方法的时候。或者被中断了当获取读锁的时候。
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock.
         *
         * 在这个实现中，因为这个方法是一个明确的中断点，优先响应中断的线程，而不是正常或重入的线程
         *
         * @throws InterruptedException if the current thread is interrupted
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        /**
         * Acquires the read lock only if the write lock is not held by
         * another thread at the time of invocation.
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately with the value
         * {@code true}. Even when this lock has been set to use a
         * fair ordering policy, a call to {@code tryLock()}
         * <em>will</em> immediately acquire the read lock if it is
         * available, whether or not other threads are currently
         * waiting for the read lock.  This &quot;barging&quot; behavior
         * can be useful in certain circumstances, even though it
         * breaks fairness. If you want to honor the fairness setting
         * for this lock, then use {@link #tryLock(long, TimeUnit)
         * tryLock(0, TimeUnit.SECONDS) } which is almost equivalent
         * (it also detects interruption).
         *
         * <p>If the write lock is held by another thread then
         * this method will return immediately with the value
         * {@code false}.
         *
         * 获取读锁如果写锁没有被其它线程持有。
         *
         * 获取读锁如果写锁没有被其它线程持有并且立刻返回true.即是公平锁，tryLock将立刻
         * 获取锁，如果锁可用。不管其它线程是否正在等待这个写锁。这个强入行为可能是有用的
         * 在特定的环境中。即使它破坏公平性。如果你想公平性，你可以使用tryLock（long,TimeUnit）
         * tryLock(0, TimeUnit.SECONDS)(它也检测中断)
         *
         * 如果写锁被其它线程持有。这个方法立刻返回false.
         * @return {@code true} if the read lock was acquired
         */
        public boolean tryLock() {
            return sync.tryReadLock();
        }

        /**
         * Acquires the read lock if the write lock is not held by
         * another thread within the given waiting time and the
         * current thread has not been {@linkplain Thread#interrupt
         * interrupted}.
         *
         * 用给定的等待时间参数获取读锁，如果写锁没有被其它线程持有，并且当前线程没有
         * 被中断。
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately with the value
         * {@code true}. If this lock has been set to use a fair
         * ordering policy then an available lock <em>will not</em> be
         * acquired if any other threads are waiting for the
         * lock. This is in contrast to the {@link #tryLock()}
         * method. If you want a timed {@code tryLock} that does
         * permit barging on a fair lock then combine the timed and
         * un-timed forms together:
         *
         *  <pre> {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}</pre>
         *
         * 获取读锁如果写锁没有被其它线程获取并且立刻返回true.如果这个锁是公平锁，那么
         * 可用的锁将不会被获取如果其它线程正在等待这个锁。这一点和tryLock()不同。
         * 如果你想要一个有时间参数的tryLock允许强行获取公平锁，那么可以像下面一样，把
         * 带时间参数和不还时间的结合到一块。
         *
         * <p>If the write lock is held by another thread then the
         * current thread becomes disabled for thread scheduling
         * purposes and lies dormant until one of three things happens:
         *
         * 如果写锁正在被其它线程持有，那么当前线程就变得不要用，对线程调试来说，
         * 并且休眠只到以下三个中的其中一个发生。
         *
         * <ul>
         *
         * <li>The read lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread; or
         *
         * <li>The specified waiting time elapses.
         *
         * </ul>
         *
         * 1. 读锁被当前线程获取到。
         * 2. 其它线程中断了当前线程。
         * 3. 时间超时了。
         *
         * <p>If the read lock is acquired then the value {@code true} is
         * returned.
         *
         * 如果获取到了读锁，那么返回true.
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method; or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the read lock,
         *
         * </ul> then {@link InterruptedException} is thrown and the
         * current thread's interrupted status is cleared.
         *
         * <p>If the specified waiting time elapses then the value
         * {@code false} is returned.  If the time is less than or
         * equal to zero, the method will not wait at all.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock, and over reporting the elapse of the waiting time.
         *
         * 如果当前线程：
         *  进入这个方法时被中断，或者被中断了当获取读锁的时候。
         *  那么InterruptedException异常被抛出，并且当前中断状态被清除。
         *
         *  如果指定的等待时间消耗完了，那么返回false.如果时间小于等于0,方法就不再等待。
         *
         *  在这个实现中，因为这个方法是一个明显的中断点，优先响应中断，比正常或重入的获取操作，
         *
         * @param timeout the time to wait for the read lock
         * @param unit the time unit of the timeout argument
         * @return {@code true} if the read lock was acquired
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        /**
         * Attempts to release this lock.
         *
         * <p>If the number of readers is now zero then the lock
         * is made available for write lock attempts.
         *
         * 试图释放锁。
         * 如果读锁的数量为0,那么写锁可用。
         */
        public void unlock() {
            sync.releaseShared(1);
        }

        /**
         * Throws {@code UnsupportedOperationException} because
         * {@code ReadLocks} do not support conditions.
         *
         * 因为读锁不支持条件。
         *
         * @throws UnsupportedOperationException always
         */
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns a string identifying this lock, as well as its lock state.
         * The state, in brackets, includes the String {@code "Read locks ="}
         * followed by the number of held read locks.
         *
         * @return a string identifying this lock, as well as its lock state
         */
        public String toString() {
            int r = sync.getReadLockCount();
            return super.toString() +
                "[Read locks = " + r + "]";
        }
    }

    /**
     * The lock returned by method {@link ReentrantReadWriteLock#writeLock}.
     *
     * ReentrantReadWriteLock.writeLock返回的锁。
     */
    public static class WriteLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -4992448646407690164L;
        private final Sync sync;

        /**
         * Constructor for use by subclasses
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * Acquires the write lock.
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately, setting the write lock hold count to
         * one.
         *
         * <p>If the current thread already holds the write lock then the
         * hold count is incremented by one and the method returns
         * immediately.
         *
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and
         * lies dormant until the write lock has been acquired, at which
         * time the write lock hold count is set to one.
         *
         * 获取写锁。
         * 获取写锁，如果没有读或写锁被其它线程持有。并且立刻返回，设置写锁持有数量为1.
         *
         * 如果当前线程已经持有写锁了，那么锁的持有数量将会增加1并且立即返回。
         *
         * 如果锁被其它线程持有，那么当前线程对线程调试来说变成不可用，并且休眠只到获取到
         * 写锁，这时候写锁的持有数量被设置成1.
         */
        public void lock() {
            sync.acquire(1);
        }

        /**
         * Acquires the write lock unless the current thread is
         * {@linkplain Thread#interrupt interrupted}.
         *
         * 获取锁，除非当前线程被中断。
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately, setting the write lock hold count to
         * one.
         *
         * 获取写锁如果没有读或写锁被其它线程持有，设置写锁的持有数量为1并且返回。
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * immediately.
         *
         * 如果当前线程已经持有这个锁，那么锁的持有数量会加1并且方法会立即返回。
         *
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and
         * lies dormant until one of two things happens:
         *
         * 如果锁被其它线程持有，那么当前线程变为不可用对线程调度来说，并且休眠只到
         * 下面两个中的其中一个情况发生。
         * <ul>
         *
         * <li>The write lock is acquired by the current thread; or
         *
         * 1. 写锁被当前线程获取到，
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread.
         * 2. 或者其它线程中断了当前线程。
         * </ul>
         *
         * <p>If the write lock is acquired by the current thread then the
         * lock hold count is set to one.
         *
         * 如果写锁被当前线程获取，那么持有数量被设置为1
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method;
         * or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the write lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         * 如果当前线程：
         * 当进入这个方法里带着中断状态，或者当获取写锁的时候被中断，
         * 那么抛出InterruptedException并且当前线程的中断状态被清除。
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock.
         *
         * 在这个实现当中，因为这个方法是明显的中断点，优先回应中断，而不是
         * 普通或重入的获取。
         *
         * @throws InterruptedException if the current thread is interrupted
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        /**
         * Acquires the write lock only if it is not held by another thread
         * at the time of invocation.
         *
         * 获取写锁只在锁没有被其它线程持有的时候。
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately with the value {@code true},
         * setting the write lock hold count to one. Even when this lock has
         * been set to use a fair ordering policy, a call to
         * {@code tryLock()} <em>will</em> immediately acquire the
         * lock if it is available, whether or not other threads are
         * currently waiting for the write lock.  This &quot;barging&quot;
         * behavior can be useful in certain circumstances, even
         * though it breaks fairness. If you want to honor the
         * fairness setting for this lock, then use {@link
         * #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS) }
         * which is almost equivalent (it also detects interruption).
         *
         * 获取写锁如果没有写锁或读锁被其它线程持有，并且立刻返回true,同步设置
         * 锁的持有数量为1。即使这是一个公平锁，调用tryLock()将立即获取锁如果锁可用，
         * 不管其它线程是否正在等待写锁。这种行为可能在一些情况下有用，尽管它打破了
         * 公平性。如果你坚持锁的公平性，那么使用tryLock(long,TimeUnit),
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * {@code true}.
         * 如果当前线程已经持有这个锁，那么持有数量被加1并且返回true.
         *
         * <p>If the lock is held by another thread then this method
         * will return immediately with the value {@code false}.
         * 如果锁被其它线程持有那么 这个方法立刻返回false.
         *
         * @return {@code true} if the lock was free and was acquired
         * by the current thread, or the write lock was already held
         * by the current thread; and {@code false} otherwise.
         * 如果锁是自由的并且被当前线程获取那么返回true,或者写锁已经被
         * 当前线程持有否则就返回false.
         */
        public boolean tryLock( ) {
            return sync.tryWriteLock();
        }

        /**
         * Acquires the write lock if it is not held by another thread
         * within the given waiting time and the current thread has
         * not been {@linkplain Thread#interrupt interrupted}.
         * 以给定的等待时间获取写锁如果这个个锁没有被其它线程持有，并且
         * 当前线程没有被中断，
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately with the value {@code true},
         * setting the write lock hold count to one. If this lock has been
         * set to use a fair ordering policy then an available lock
         * <em>will not</em> be acquired if any other threads are
         * waiting for the write lock. This is in contrast to the {@link
         * #tryLock()} method. If you want a timed {@code tryLock}
         * that does permit barging on a fair lock then combine the
         * timed and un-timed forms together:
         *
         *  <pre> {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}</pre>
         * 获取写锁如果没有读锁或写锁被其它线程持有，并且立刻返回true,同步设置
         * 持有数量为1。如果这个是公平锁，那么可用的锁将不用被获取如果有其它线程
         * 正在等待写锁。这一点和tryLock()相反。如果你想一个定时的trylock允许强行
         * 获取公平锁，那么可能像下面把定时和不定时的样式放一块。
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * {@code true}.
         *
         * 如果当前线程已经持有这个锁，那么持有锁数量被加1，并且返回true.
         *
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and
         * lies dormant until one of three things happens:
         *
         * 如果锁被其它线程持有，那么当前线程对线程调试变得不可用，并且休眠只到
         * 下面的三中情况中的一种发生:
         *
         * <ul>
         *
         * <li>The write lock is acquired by the current thread; or
         * 写锁被当前线程获取，或者其它线程中断了当前线程，或者超时
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread; or
         *
         * <li>The specified waiting time elapses
         *
         * </ul>
         *
         * <p>If the write lock is acquired then the value {@code true} is
         * returned and the write lock hold count is set to one.
         *
         *  如果写锁被获取，那么返回true，并且写锁的持有数量被设置为1
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method;
         * or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the write lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         * 如果 当前线程：
         * 进入这个方法的时候设置了中断状态或者获取锁的时候被中断了，
         * 那么InterruptedException被抛出并且当前线程的中断状态被清除。
         *
         * <p>If the specified waiting time elapses then the value
         * {@code false} is returned.  If the time is less than or
         * equal to zero, the method will not wait at all.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock, and over reporting the elapse of the waiting time.
         * 如果指定的等待时间消耗完，那么返回false.如果时间小于或等于0，
         * 方法将不再等待。
         *
         * 在这个实现中，因为这个方法是一个显然的中断点，优先响应中断，
         * 而不是普通的或重入获取，并且报告等待时间。
         * @param timeout the time to wait for the write lock
         * @param unit the time unit of the timeout argument
         *
         * @return {@code true} if the lock was free and was acquired
         * by the current thread, or the write lock was already held by the
         * current thread; and {@code false} if the waiting time
         * elapsed before the lock could be acquired.
         *
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }

        /**
         * Attempts to release this lock.
         *
         * <p>If the current thread is the holder of this lock then
         * the hold count is decremented. If the hold count is now
         * zero then the lock is released.  If the current thread is
         * not the holder of this lock then {@link
         * IllegalMonitorStateException} is thrown.
         *
         * 试图释放锁。
         * 如果当前线程持有锁，那么持有数量减1.如果持有现在是0，那么锁被
         * 释放。如果当前不是锁的持有者。那么抛出IllegalMonitorStateException异常。
         * @throws IllegalMonitorStateException if the current thread does not
         * hold this lock
         */
        public void unlock() {
            sync.release(1);
        }

        /**
         * Returns a {@link Condition} instance for use with this
         * {@link Lock} instance.
         * <p>The returned {@link Condition} instance supports the same
         * usages as do the {@link Object} monitor methods ({@link
         * Object#wait() wait}, {@link Object#notify notify}, and {@link
         * Object#notifyAll notifyAll}) when used with the built-in
         * monitor lock.
         * 返回这个锁的一个条件实例。
         * 返回的条件实例支持像Object.wait()，Object.notify(),Object.notifyAll,
         * 一样的用法，
         * <ul>
         *
         * <li>If this write lock is not held when any {@link
         * Condition} method is called then an {@link
         * IllegalMonitorStateException} is thrown.  (Read locks are
         * held independently of write locks, so are not checked or
         * affected. However it is essentially always an error to
         * invoke a condition waiting method when the current thread
         * has also acquired read locks, since other threads that
         * could unblock it will not be able to acquire the write
         * lock.)
         *
         * 如果不持有写锁，那么调用Condition方法就会抛出IllegalMonitorStateException异常。
         * (持有读锁独立于写锁，所以不受影响。但是当当前线程持有读锁的时候。
         * 调用Contidion方法总是引起错误，因为其它线程可以解锁它，它将获取不到写锁。)
         *
         * <li>When the condition {@linkplain Condition#await() waiting}
         * methods are called the write lock is released and, before
         * they return, the write lock is reacquired and the lock hold
         * count restored to what it was when the method was called.
         *
         * 当Condition.await()调用时，写锁被释放，并且在写锁被重新获取之前，
         * 写锁被重新获取，并且写锁持有数量被恢复到原来到调用等待方法的的值
         *
         * <li>If a thread is {@linkplain Thread#interrupt interrupted} while
         * waiting then the wait will terminate, an {@link
         * InterruptedException} will be thrown, and the thread's
         * interrupted status will be cleared.
         *
         * 如果一个线程正在等待被中断，那么等待就会结束，一个InterruptedException
         * 就会被抛出。并且中断状态将会被清除。
         *
         * <li> Waiting threads are signalled in FIFO order.
         *
         * 等待的线程以先进先出的顺序被唤醒。
         *
         * <li>The ordering of lock reacquisition for threads returning
         * from waiting methods is the same as for threads initially
         * acquiring the lock, which is in the default case not specified,
         * but for <em>fair</em> locks favors those threads that have been
         * waiting the longest.
         *
         * 锁从等待方法返回时重新获取的顺序，跟锁被开始获取的顺序一样。
         * 也就是以默认的方式，但是对于公平的锁，更倾向于等待时间最长的时间的锁。
         *
         * </ul>
         *
         * @return the Condition object
         */
        public Condition newCondition() {
            return sync.newCondition();
        }

        /**
         * Returns a string identifying this lock, as well as its lock
         * state.  The state, in brackets includes either the String
         * {@code "Unlocked"} or the String {@code "Locked by"}
         * followed by the {@linkplain Thread#getName name} of the owning thread.
         *
         * @return a string identifying this lock, as well as its lock state
         */
        public String toString() {
            Thread o = sync.getOwner();
            return super.toString() + ((o == null) ?
                                       "[Unlocked]" :
                                       "[Locked by thread " + o.getName() + "]");
        }

        /**
         * Queries if this write lock is held by the current thread.
         * Identical in effect to {@link
         * ReentrantReadWriteLock#isWriteLockedByCurrentThread}.
         * 查询这个写锁是否被当前线程持有。和ReentrantReadWriteLock.isWriteLockedByCurrentThread
         * 一样。
         * @return {@code true} if the current thread holds this lock and
         *         {@code false} otherwise
         * @since 1.6
         */
        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }

        /**
         * Queries the number of holds on this write lock by the current
         * thread.  A thread has a hold on a lock for each lock action
         * that is not matched by an unlock action.  Identical in effect
         * to {@link ReentrantReadWriteLock#getWriteHoldCount}.
         *
         * 查询当前线程持有的写锁数量。A thread has a hold on a lock for each lock action
         * that is not matched by an unlock action。和ReentrantReadWriteLock.
         * getWriteHoldCount方法一样。
         * @return the number of holds on this lock by the current thread,
         *         or zero if this lock is not held by the current thread
         * @since 1.6
         */
        public int getHoldCount() {
            return sync.getWriteHoldCount();
        }
    }

    // Instrumentation and status

    /**
     * Returns {@code true} if this lock has fairness set true.
     *
     * @return {@code true} if this lock has fairness set true
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * Returns the thread that currently owns the write lock, or
     * {@code null} if not owned. When this method is called by a
     * thread that is not the owner, the return value reflects a
     * best-effort approximation of current lock status. For example,
     * the owner may be momentarily {@code null} even if there are
     * threads trying to acquire the lock but have not yet done so.
     * This method is designed to facilitate construction of
     * subclasses that provide more extensive lock monitoring
     * facilities.
     * 返回当前持有锁的线程，或者null如果没有线程持有。当时这个方法
     * 被不是持有锁的线程调用时，反返回值大概反应了这个锁的状态。
     * 例如，锁的持有者可能暂时null,即使有线程正在试图获取锁，但是还没有成功。
     * 这个类被设计用来帮助子类构建更扩展的锁监控.
     *
     * @return the owner, or {@code null} if not owned
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * Queries the number of read locks held for this lock. This
     * method is designed for use in monitoring system state, not for
     * synchronization control.
     *
     * 查询持有这个锁的读锁数量.这个方法被设计用来监控系统状态,也不是
     * 同步控制.
     *
     * @return the number of read locks held
     */
    public int getReadLockCount() {
        return sync.getReadLockCount();
    }

    /**
     * Queries if the write lock is held by any thread. This method is
     * designed for use in monitoring system state, not for
     * synchronization control.
     * 查询是否有线程持有这个写锁,这个方法被设计用来监控系统状态,而不是为了
     * 同步控制.
     * @return {@code true} if any thread holds the write lock and
     *         {@code false} otherwise
     */
    public boolean isWriteLocked() {
        return sync.isWriteLocked();
    }

    /**
     * Queries if the write lock is held by the current thread.
     * 查询是否写锁被当前线程持有.
     * @return {@code true} if the current thread holds the write lock and
     *         {@code false} otherwise
     */
    public boolean isWriteLockedByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * Queries the number of reentrant write holds on this lock by the
     * current thread.  A writer thread has a hold on a lock for
     * each lock action that is not matched by an unlock action.
     *
     * 查询当前线程持有写锁的数量 .A writer thread has a hold on a lock for
     * each lock action that is not matched by an unlock action
     *
     * @return the number of holds on the write lock by the current thread,
     *         or zero if the write lock is not held by the current thread
     */
    public int getWriteHoldCount() {
        return sync.getWriteHoldCount();
    }

    /**
     * Queries the number of reentrant read holds on this lock by the
     * current thread.  A reader thread has a hold on a lock for
     * each lock action that is not matched by an unlock action.
     *
     * 查询当前线程持有读锁的数量,A reader thread has a hold on a lock for
     * each lock action that is not matched by an unlock action.
     *
     * @return the number of holds on the read lock by the current thread,
     *         or zero if the read lock is not held by the current thread
     * @since 1.6
     */
    public int getReadHoldCount() {
        return sync.getReadHoldCount();
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire the write lock.  Because the actual set of threads may
     * change dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive lock monitoring facilities.
     * 返回一个可能正在等待这个写锁的集合,因为真实线程集可能动态改变当
     * 构建这个结果的时候,返回的集合只是一个大概.返回的集合元素没有特定
     * 的顺序.这个方法被用来方便子类构建更具有扩展性的锁监控.
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedWriterThreads() {
        return sync.getExclusiveQueuedThreads();
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire the read lock.  Because the actual set of threads may
     * change dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive lock monitoring facilities.
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedReaderThreads() {
        return sync.getSharedQueuedThreads();
    }

    /**
     * Queries whether any threads are waiting to acquire the read or
     * write lock. Note that because cancellations may occur at any
     * time, a {@code true} return does not guarantee that any other
     * thread will ever acquire a lock.  This method is designed
     * primarily for use in monitoring of the system state.
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * Queries whether the given thread is waiting to acquire either
     * the read or write lock. Note that because cancellations may
     * occur at any time, a {@code true} return does not guarantee
     * that this thread will ever acquire a lock.  This method is
     * designed primarily for use in monitoring of the system state.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is queued waiting for this lock
     * @throws NullPointerException if the thread is null
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * Returns an estimate of the number of threads waiting to acquire
     * either the read or write lock.  The value is only an estimate
     * because the number of threads may change dynamically while this
     * method traverses internal data structures.  This method is
     * designed for use in monitoring of the system state, not for
     * synchronization control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire either the read or write lock.  Because the actual set
     * of threads may change dynamically while constructing this
     * result, the returned collection is only a best-effort estimate.
     * The elements of the returned collection are in no particular
     * order.  This method is designed to facilitate construction of
     * subclasses that provide more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with the write lock. Note that because timeouts and
     * interrupts may occur at any time, a {@code true} return does
     * not guarantee that a future {@code signal} will awaken any
     * threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with the write lock. Note that because
     * timeouts and interrupts may occur at any time, the estimate
     * serves only as an upper bound on the actual number of waiters.
     * This method is designed for use in monitoring of the system
     * state, not for synchronization control.
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with the write lock.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a
     * best-effort estimate. The elements of the returned collection
     * are in no particular order.  This method is designed to
     * facilitate construction of subclasses that provide more
     * extensive condition monitoring facilities.
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a string identifying this lock, as well as its lock state.
     * The state, in brackets, includes the String {@code "Write locks ="}
     * followed by the number of reentrantly held write locks, and the
     * String {@code "Read locks ="} followed by the number of held
     * read locks.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        int c = sync.getCount();
        int w = Sync.exclusiveCount(c);
        int r = Sync.sharedCount(c);

        return super.toString() +
            "[Write locks = " + w + ", Read locks = " + r + "]";
    }

    /**
     * Returns the thread id for the given thread.  We must access
     * this directly rather than via method Thread.getId() because
     * getId() is not final, and has been known to be overridden in
     * ways that do not preserve unique mappings.
     * 返回指定线程的线程id.我们必须直接访问这个方法而不是通过Thread.getId(),因为
     * getId不是final,并且已经被知道可以被overridden，以不保存唯一映射的方式。
     */
    static final long getThreadId(Thread thread) {
        return UNSAFE.getLongVolatile(thread, TID_OFFSET);
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long TID_OFFSET;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            TID_OFFSET = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("tid"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
