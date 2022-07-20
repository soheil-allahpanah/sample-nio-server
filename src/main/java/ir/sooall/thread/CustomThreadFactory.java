package ir.sooall.thread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class CustomThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final AtomicInteger threadIdIndex = new AtomicInteger(0);
    private final List<ThreadId> threadIds = Collections.synchronizedList(new ArrayList<>());
    private final ThreadGroup group;
    private final String namePrefix;


    public CustomThreadFactory() {
        @SuppressWarnings("removal")
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
            Thread.currentThread().getThreadGroup();
        namePrefix = "pool-" +
            poolNumber.getAndIncrement() +
            "-thread-";
    }

    public ThreadId getNextThreadId() {
        var res = threadIds.get(threadIdIndex.get());
        var number = (threadIdIndex.get() + 1) % threadIds.size();
        threadIdIndex.set(number);
        return res;
    }

    public Thread newThread(Runnable r) {
        var name = namePrefix + threadNumber.getAndIncrement();
        Thread t = new Thread(group, r,
            name,
            0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        threadIds.add(new ThreadId(name));
        return t;
    }
}
