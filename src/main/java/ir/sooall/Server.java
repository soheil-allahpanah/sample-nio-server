package ir.sooall;

import ir.sooall.processor.SocketAcceptor;
import ir.sooall.processor.SocketProcessor;
import ir.sooall.thread.CustomThreadFactory;

public class Server {

    private final int tcpPort;
    private final CustomThreadFactory customThreadFactory = new CustomThreadFactory();

    public Server(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public void start() {
        new SocketAcceptor(tcpPort, customThreadFactory).start();
        new SocketProcessor(customThreadFactory).start();
    }
}
