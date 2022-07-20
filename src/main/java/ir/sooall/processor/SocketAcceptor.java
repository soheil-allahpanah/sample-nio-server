package ir.sooall.processor;


import ir.sooall.SocketContainer;
import ir.sooall.SocketWrapper;
import ir.sooall.thread.CustomThreadFactory;
import ir.sooall.thread.ThreadId;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SocketAcceptor {

    private final SocketContainer socketContainer;
    private final Runnable acceptor;
    private final int port;
    private boolean stopped;
    private final CustomThreadFactory customThreadFactory;

    public SocketAcceptor(int port, CustomThreadFactory customThreadFactory) {
        this.socketContainer = SocketContainer.SocketContainerHolder.getSocketContainer();
        this.port = port;
        this.stopped = false;
        this.customThreadFactory = customThreadFactory;
        this.acceptor = new Acceptor();
    }

    public class Acceptor implements Runnable {

        @Override
        public void run() {
            try {
                ServerSocketChannel serverSocketChannel = ServerSocketChannel
                    .open()
                    .bind(new InetSocketAddress(SocketAcceptor.this.port));

                while (!stopped) {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    ThreadId threadId = customThreadFactory.getNextThreadId();
//          System.out.printf("SocketAcceptor >> add SocketWrapper to socketContainer.socketChannel : %s , threadId: %s  \n\r", socketChannel.getLocalAddress(), threadId);
                    socketContainer.add(threadId, new SocketWrapper(socketChannel));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        stopped = true;
    }

    public void start() {
        Thread thread = new Thread(acceptor);
        thread.start();
    }
}
