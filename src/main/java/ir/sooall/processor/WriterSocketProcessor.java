package ir.sooall.processor;

import ir.sooall.SocketWrapper;
import ir.sooall.message.Message;
import ir.sooall.message.writer.MessageWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

public class WriterSocketProcessor {

    private final ByteBuffer writeByteBuffer = ByteBuffer.allocate(1024 * 1024);
    private final Set<SocketWrapper> nonEmptySockets = new HashSet<>();
    private final Set<SocketWrapper> emptySockets = new HashSet<>();
    private final Map<Long, SocketWrapper> socketMap;
    private final Queue<Message> outboundMessageQueue;


    public WriterSocketProcessor(ReaderSocketProcessor readerSocketProcessor) {
        socketMap = readerSocketProcessor.getSocketMap();
        outboundMessageQueue = readerSocketProcessor.getOutboundMessageQueue();
    }

    public void writeToSockets(Selector writeSelector) {
        try {
            takeNewOutboundMessages();
            cancelEmptySockets(writeSelector);
            registerNonEmptySockets(writeSelector);
            int writeReady = writeSelector.selectNow();
            if (writeReady > 0) {
                Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    SocketWrapper socket = (SocketWrapper) key.attachment();
                    socket.getMessageWriter().write(key, socket, this.writeByteBuffer);
                    if (socket.getMessageWriter().isEmpty()) {
                        this.emptySockets.add(socket);
                    }
                    keyIterator.remove();
                }
                selectionKeys.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void takeNewOutboundMessages() {
        Message outMessage = this.outboundMessageQueue.poll();
        while (outMessage != null) {
            SocketWrapper socket = this.socketMap.get(outMessage.socketId);
            if (socket != null) {
                MessageWriter messageWriter = socket.getMessageWriter();
                if (messageWriter.isEmpty()) {
                    messageWriter.enqueue(outMessage);
                    emptySockets.remove(socket);
                    nonEmptySockets.add(socket);
                } else {
                    messageWriter.enqueue(outMessage);
                }
            }
            outMessage = this.outboundMessageQueue.poll();
        }
    }

    private void cancelEmptySockets(Selector writeSelector) {
        for (SocketWrapper socket : emptySockets) {
            SelectionKey key = socket.getSocketChannel().keyFor(writeSelector);
            key.cancel();
        }
        emptySockets.clear();
    }

    private void registerNonEmptySockets(Selector writeSelector) throws ClosedChannelException {
        for (SocketWrapper socket : nonEmptySockets) {
            socket.getSocketChannel().register(writeSelector, SelectionKey.OP_WRITE, socket);
        }
        nonEmptySockets.clear();
    }
}
