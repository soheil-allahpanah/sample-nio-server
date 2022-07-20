package ir.sooall.processor;

import ir.sooall.SocketContainer;
import ir.sooall.SocketWrapper;
import ir.sooall.message.Message;
import ir.sooall.message.ds.MessageBuffer;
import ir.sooall.message.MessageProcessor;
import ir.sooall.thread.ThreadId;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReaderSocketProcessor {

    private final MessageBuffer readMessageBuffer = new MessageBuffer();
    private final ByteBuffer readByteBuffer = ByteBuffer.allocate(1024 * 1024);
    private final Map<Long, SocketWrapper> socketMap = new ConcurrentHashMap<>();
    private final Queue<Message> outboundMessageQueue = new ConcurrentLinkedQueue<>();
    private final SocketContainer socketContainer;
    private final MessageProcessor messageProcessor;

    public ReaderSocketProcessor(SocketContainer socketContainer, MessageProcessor messageProcessor) {
        this.socketContainer = socketContainer;
        this.messageProcessor = messageProcessor;
    }

    public void takeNewSockets(final Selector readSelector) {
        ThreadId threadId = new ThreadId(Thread.currentThread().getName());
        SocketWrapper newSocket = this.socketContainer.take(threadId);
        if (newSocket != null) {
            try {
                newSocket.init(readSelector, readMessageBuffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.socketMap.put(newSocket.getSocketId(), newSocket);
        }
    }

    public void readFromSockets(final Selector readSelector) {
        int readReady = 0;
        try {
            readReady = readSelector.selectNow();
            if (readReady > 0) {
                Set<SelectionKey> selectedKeys = readSelector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    readFromSocket(key);
                    keyIterator.remove();
                }
                selectedKeys.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readFromSocket(final SelectionKey key) throws IOException {
        SocketWrapper socket = (SocketWrapper) key.attachment();
        socket.getMessageReader().read(socket, this.readByteBuffer);

        List<Message> fullMessages = socket.getMessageReader().getMessages();
        if (fullMessages.size() > 0) {
            for (Message message : fullMessages) {
                message.socketId = socket.getSocketId();
                outboundMessageQueue.offer(this.messageProcessor.process(message));
            }
            fullMessages.clear();
        }
        if (socket.isEndOfStreamReached()) {
            this.socketMap.remove(socket.getSocketId());
            key.attach(null);
            key.cancel();
            key.channel().close();
        }
    }

    public Map<Long, SocketWrapper> getSocketMap() {
        return socketMap;
    }

    public Queue<Message> getOutboundMessageQueue() {
        return outboundMessageQueue;
    }
}
