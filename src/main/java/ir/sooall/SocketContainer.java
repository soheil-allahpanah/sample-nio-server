package ir.sooall;

import ir.sooall.thread.ThreadId;

import java.util.Map;
import java.util.concurrent.*;

public final class SocketContainer {

    private SocketContainer() {
    }

    private Map<ThreadId, BlockingQueue<SocketWrapper>> acceptedSocket;

    private void initQueue(ThreadId threadId) {
        if (acceptedSocket == null) {
            acceptedSocket = new ConcurrentHashMap<>();
        }
        if (acceptedSocket.get(threadId) == null) {
            var queue = new ArrayBlockingQueue<SocketWrapper>(1024);
            acceptedSocket.put(threadId, queue);
        }
    }

    public void add(ThreadId threadId, SocketWrapper socket) {
        initQueue(threadId);
        this.acceptedSocket.get(threadId).offer(socket);
    }

    public SocketWrapper take(ThreadId threadId) {
        initQueue(threadId);
        try {
            return this.acceptedSocket.get(threadId).poll(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public static final class SocketContainerHolder {
        private static final SocketContainer socketContainer = new SocketContainer();

        public static SocketContainer getSocketContainer() {
            return socketContainer;
        }
    }
}
