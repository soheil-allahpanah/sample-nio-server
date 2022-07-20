package ir.sooall;

import ir.sooall.message.ds.MessageBuffer;
import ir.sooall.message.reader.HttpMessageReader;
import ir.sooall.message.reader.MessageReader;
import ir.sooall.message.writer.MessageWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicLong;

public class SocketWrapper {

    public static class Sequence {
        private static final AtomicLong counter = new AtomicLong();

        public static Long nextValue() {
            return counter.getAndIncrement();
        }
    }

    private final SocketChannel socketChannel;
    private Long socketId;
    private boolean endOfStreamReached = false;
    private MessageReader messageReader = null;
    private MessageWriter messageWriter = null;


    public SocketWrapper(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void init(Selector readSelector, MessageBuffer readMessageBuffer) throws IOException {
        socketId = Sequence.nextValue();
        socketChannel.configureBlocking(false);
        messageReader = new HttpMessageReader();
        messageReader.init(readMessageBuffer);
        messageWriter = new MessageWriter();
        SelectionKey key = socketChannel.register(readSelector, SelectionKey.OP_READ);
        key.attach(this);
    }

    public int read(ByteBuffer byteBuffer) throws IOException {
        int bytesRead = this.socketChannel.read(byteBuffer);
        int totalBytesRead = bytesRead;
        while (bytesRead > 0) {
            bytesRead = this.socketChannel.read(byteBuffer);
            totalBytesRead += bytesRead;
        }

        if (bytesRead == -1) {
            this.endOfStreamReached = true;
        }
        return totalBytesRead;
    }

    public int write(ByteBuffer byteBuffer) throws IOException {
        int bytesWritten = this.socketChannel.write(byteBuffer);
        int totalBytesWritten = bytesWritten;

        while (bytesWritten > 0 && byteBuffer.hasRemaining()) {
            bytesWritten = this.socketChannel.write(byteBuffer);
            totalBytesWritten += bytesWritten;
        }
        return totalBytesWritten;
    }

    public Long getSocketId() {
        return socketId;
    }

    public MessageReader getMessageReader() {
        return messageReader;
    }

    public MessageWriter getMessageWriter() {
        return messageWriter;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public boolean isEndOfStreamReached() {
        return endOfStreamReached;
    }
}
