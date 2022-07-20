package ir.sooall.message.writer;

import ir.sooall.SocketWrapper;
import ir.sooall.message.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;

public class MessageWriter {

    private List<Message> writeQueue = new ArrayList<>();
    private Message messageInProgress = null;
    private int bytesWritten = 0;

    public MessageWriter() {
    }

    public void enqueue(Message message) {
        if (this.messageInProgress == null) {
            this.messageInProgress = message;
        } else {
            this.writeQueue.add(message);
        }
    }

    public void write(SelectionKey key, SocketWrapper socket, ByteBuffer byteBuffer) throws IOException {
        byteBuffer.put(this.messageInProgress.data.array()
            , this.messageInProgress.data.offset() + this.bytesWritten
            , this.messageInProgress.data.length() - this.bytesWritten);
        byteBuffer.flip();

        this.bytesWritten += socket.write(byteBuffer);
        byteBuffer.clear();

        if (bytesWritten >= this.messageInProgress.data.length()) {
            if (this.writeQueue.size() > 0) {
                this.messageInProgress = this.writeQueue.remove(0);
            } else {
                this.messageInProgress = null;
                this.bytesWritten = 0;

                key.attach(null);
                key.cancel();
                key.channel().close();
            }
        }
    }

    public boolean isEmpty() {
        return this.writeQueue.isEmpty() && this.messageInProgress == null;
    }

}
