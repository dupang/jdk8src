package jls.threadAndLock;

/**
 * Created by dupang on 2016/5/22.
 */
public class threadAndLock {
    /**
     * 当一个对象被创建，它的等待集合是空的。增加线程到和删除线程从等待集合中的基本的行为是自动的。
     * 等待集合被操控仅仅通过方法Object.wait,Object.notify,和Object.notifyAll.
     *
     * 等待集合的操控也可以受线程的中断状态的影响，和线程类的处理中断的方法。另外，Thread类的方法
     * sleeping和join()也来自等待和通过的操作。
     *
     * 等待行为发生在调用wait()方法的时候，或者定时的形式的wait(long millisecs)和wait(long millisecs,int nanosecs)
     *    调用wait(long millisecs)传入一个0的参数，或者调用wait(long millsecs,int nanosecs)传入两个0的参数，效果和
     *    调用wait()一样。
     *
     *    一个线程正常地返回从wait方法中，如果它没有抛出异常。
     *
     *    让线程t执行对象m的wait方法，并且n是线程t在对象m上锁操作的数量，并且还没有解锁的操作。
     *    下面的其中一件事发生：
     *    1.如果n是0，(也就是线程t还没有对m执行锁操作)，那么将抛出IllegalMonitorStateExceptio异常。
     *    2.如果这是一个定时的等待并且nanosecs参数范围不在0-999999，或者millisecs参数是负数，那么抛出
     *    IllegalArgumentException异常。
     *    3.如果 线程t被中断，那么抛出InterruptedException异常，并且t的中断状态被设置成false.
     *
     *
     *    否则，下面的顺序发生
     *
     *    1.线程t被加入到对象m的等待集合，并且执行n次解锁操作在m上。
     *    2.线程t不会执行任何更多的指令直到它被从m的等待队列上移除。线程可能从等待集合上移除由于下面的任何一个操作
     *      并且之后的某一时刻恢复。
     *      1》.在对象m上执行notify操作，并且t被选择从m的等待队列中移除。
     *      2》.在对象m上执行notifyAll操作，
     *      3》.在线程t上执行中断操作。
     *      4》.如果这是一个定时的wait,从m的等待中移除t至少经过millsecs毫秒加上nanosecs纳秒。
     *      5》.实现的内部操作，实现被允许，尽管不提倡，执行"虚假唤醒"，也就是说从等待集合中移除线程，并且使它恢复而
     *          没有明确的指令去执行这个。
     *
     *          注意这个规定需要Java编写wait只有在循环中，这个循环只在一些这个线程正在等待的条件满足的时候才终止。
     *
     *      每一个线程必须确定它被从等待集合移除的顺序，这个顺序不必和其它顺序一致，但是这个线程必须表现得像这个顺序
     *      一样。
     *
     *      例如，如果一个线程t正在m的等待集合上，然后线程t的中断和m的唤醒发生。在这些事件上必须有一个顺序。如果线程t
     *      的中断先发生，那么线程t从wait中返回，并抛出一个InterruptedException,并且其它线程在m的等待集合中必须接受到了
     *      唤醒通知。如果通知被认为先发生。那么t最终从wait方法中返回并且中断即将发生。
     *
     *      3.线程t执行n个锁操作在m对象上
     *      4.如果由于中断线程t从m的中断集合中移除。那么t的中断状态被设置成false,并且wait方法抛出InterruptedException.
     *
     *
     *      17.2.2 Notification
     *
     *      通知动作发生在notify和notifyAll方法的时候。
     *
     *      假如线程t是正在对象t上执行这些方法的的线程，并且n是t在m上的锁动作的数量并且没有还没有解锁。下面动作中的其中一个
     *      发生。
     *
     *      1. 如果n是0，那么抛出IllegalMonitorStateException异常。这就是t还没有持有对象m的锁的例子。
     *      2. 如果n是一个大于0的数，并且这是一个notify动作。那么如果m的当前等待集合不会空，线程u是m的等待集合中被选择的
     *         一员，并且从等待集合中移除。
     *
     *         没有保证那一个线程会从等待集合中被选择。从等待集合中移除使u从wait中恢复，然而请注意，在恢复时的u的锁动作不
     *         会成功直到t完全释放了m的监视器。
     *
     *      3. 如果n大于0并且这是一个notifyALl动作，然后所有线程被移除从m的等待集合中。
     *
     *          然而请注意一个时刻只有他们中的其中一个将锁住监视器在从wait中恢复的时候。
     *
     *     17.2.3  中断
     *
     *
     *     中断动作发生在在调用Thread.interrupt的时候，反过来方法被定义的类调用也可以，例如ThreadGroup.interrupt.
     *
     *     假如线程t正在调用u.interrupt,对于一些线程u,t和u可能是一样的。这个动作使u的中断判断被设置成true.
     *
     *     另外，如果存在对象m，它的等待集合中包含u，那么 u被移除从m的等待集合中。这使u从等待动作中恢复过来，
     *     这种情况这个等待将在重新锁m的监视器之后抛出interruption异常。
     *
     *     调用Thread.isInterrupted的方法可以确定一个线程的中断状态。静态方法Thread.interrupted可以被线程调用
     *     来查看它的中断状态并且清除它的中断状态。
     *
     *
     *
     *
     *
     */
}
