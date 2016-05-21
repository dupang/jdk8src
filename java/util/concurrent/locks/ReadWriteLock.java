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

/**
 * A {@code ReadWriteLock} maintains a pair of associated {@link
 * Lock locks}, one for read-only operations and one for writing.
 * The {@link #readLock read lock} may be held simultaneously by
 * multiple reader threads, so long as there are no writers.  The
 * {@link #writeLock write lock} is exclusive.
 *
 * ReadWriteLock维护着一对相关的锁，一个为了只读操作，一个为了写操作。
 * readLock 可以同时被多个读线程持有，只要没有写线程。writeLock的写
 * 操作是排它的。
 *
 * <p>All {@code ReadWriteLock} implementations must guarantee that
 * the memory synchronization effects of {@code writeLock} operations
 * (as specified in the {@link Lock} interface) also hold with respect
 * to the associated {@code readLock}. That is, a thread successfully
 * acquiring the read lock will see all updates made upon previous
 * release of the write lock.
 *
 * 所有的ReadWriteLock实现必须保证writeLock的内存同步效果写操作也同时
 * 持有相应的readLock。也就是说，成功获取读锁后将看到所有先前释放的写锁的
 * 更新，
 *
 * <p>A read-write lock allows for a greater level of concurrency in
 * accessing shared data than that permitted by a mutual exclusion lock.
 * It exploits the fact that while only a single thread at a time (a
 * <em>writer</em> thread) can modify the shared data, in many cases any
 * number of threads can concurrently read the data (hence <em>reader</em>
 * threads).
 *
 * 一个读-写锁允许更高层的同步访问共享数据被一个互相排斥的锁。
 * 它利用只有一个线程在一个时间点可以修改共享数据，在很多情况下几个线程
 * 可以同时读数据。
 *
 *
 * In theory, the increase in concurrency permitted by the use of a read-write
 * lock will lead to performance improvements over the use of a mutual
 * exclusion lock. In practice this increase in concurrency will only be fully
 * realized on a multi-processor, and then only if the access patterns for
 * the shared data are suitable.
 *
 * 理论上，使用read-write锁增加并发将使性能的提升比使用互斥锁。实践中并发的
 * 增加将在一个多处理器中意识到，并且共享数据的访问模型是合适的。
 *
 * <p>Whether or not a read-write lock will improve performance over the use
 * of a mutual exclusion lock depends on the frequency that the data is
 * read compared to being modified, the duration of the read and write
 * operations, and the contention for the data - that is, the number of
 * threads that will try to read or write the data at the same time.
 * For example, a collection that is initially populated with data and
 * thereafter infrequently modified, while being frequently searched
 * (such as a directory of some kind) is an ideal candidate for the use of
 * a read-write lock. However, if updates become frequent then the data
 * spends most of its time being exclusively locked and there is little, if any
 * increase in concurrency. Further, if the read operations are too short
 * the overhead of the read-write lock implementation (which is inherently
 * more complex than a mutual exclusion lock) can dominate the execution
 * cost, particularly as many read-write lock implementations still serialize
 * all threads through a small section of code. Ultimately, only profiling
 * and measurement will establish whether the use of a read-write lock is
 * suitable for your application.
 *
 *
 * 读写锁是否比排他锁提升性能取决数据读和修改的频率，读操作和写操作的时长，
 * 对数据的竞争-也就是说，同时试图读或写数据的数量。例如，一个初始化好数据
 * 的集合很少被修改，而经常被查询，这种情况很适合用读写锁。但是如果更新很
 * 频繁，那么将会花费大量的时间在独占锁上而很少在并发的增加。更进一步，如果
 * 读操作时间很短，读写涣的实现可以支配执行花费，特别地，很多读写乐实现仍然
 * 序列化所有线程通过一小段代码。最终，只有分析和测量将建立读写锁是否对你的
 * 应用程序合适。
 *
 * <p>Although the basic operation of a read-write lock is straight-forward,
 * there are many policy decisions that an implementation must make, which
 * may affect the effectiveness of the read-write lock in a given application.
 * Examples of these policies include:
 *
 * 尽量读写锁的基本操作是直接的，有很多实现必须做到的策略决定，这可能影响
 * 读写锁的效率在给定的程序中。
 * 这些策略的例子包括：
 * <ul>
 * <li>Determining whether to grant the read lock or the write lock, when
 * both readers and writers are waiting, at the time that a writer releases
 * the write lock. Writer preference is common, as writes are expected to be
 * short and infrequent. Reader preference is less common as it can lead to
 * lengthy delays for a write if the readers are frequent and long-lived as
 * expected. Fair, or &quot;in-order&quot; implementations are also possible.
 *
 * 确定是授予读锁还是写锁，当读和写都在等待的时候，在写释放写锁的时候。
 * 通常偏爱写锁，因为写操作预计短和不经常。读操作偏爱少，因为它导致写延迟，
 * 如果读操作经常并且生命周期长。公平或排序实现也可能。
 *
 * <li>Determining whether readers that request the read lock while a
 * reader is active and a writer is waiting, are granted the read lock.
 * Preference to the reader can delay the writer indefinitely, while
 * preference to the writer can reduce the potential for concurrency.
 *
 * 确定是否授予读锁，当有一个活跃的读线程并且有一个写线程正在等待的时候。
 * 读线程优先可能无限延迟写线程，而写线程优先可能降低并发。
 *
 * <li>Determining whether the locks are reentrant: can a thread with the
 * write lock reacquire it? Can it acquire a read lock while holding the
 * write lock? Is the read lock itself reentrant?
 *
 * 确定锁是否可以重入：持有写锁的线程是否可以再次请求这个写锁？持有写锁的线程
 * 是否可以请求读锁，持有读写的线程是否可以再次请求读锁？
 *
 * <li>Can the write lock be downgraded to a read lock without allowing
 * an intervening writer? Can a read lock be upgraded to a write lock,
 * in preference to other waiting readers or writers?
 *
 * 写锁可不可以降级为读锁，而不允许干扰写锁，读锁是否可以升级为写锁，
 * 优先响应其它等待读线程或者写线程
 *
 * </ul>
 * You should consider all of these things when evaluating the suitability
 * of a given implementation for your application.
 *
 * 你应该考虑所有这些事情，当评估给定的实现是否适合你的实现的时候。
 *
 * @see ReentrantReadWriteLock
 * @see Lock
 * @see ReentrantLock
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ReadWriteLock {
    /**
     * Returns the lock used for reading.
     *
     * 返回读锁
     *
     * @return the lock used for reading
     */
    Lock readLock();

    /**
     * Returns the lock used for writing.
     *
     * 返回写锁
     *
     * @return the lock used for writing
     */
    Lock writeLock();
}
