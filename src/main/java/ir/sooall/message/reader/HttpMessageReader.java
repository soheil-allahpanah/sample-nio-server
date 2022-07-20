package ir.sooall.message.reader;

import ir.sooall.SocketWrapper;
import ir.sooall.message.Message;
import ir.sooall.message.ds.MessageBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class HttpMessageReader implements MessageReader {

    private MessageBuffer messageBuffer = null;
    private final List<Message> completeMessages = new ArrayList<>();
    private Message nextMessage = null;

    @Override
    public void init(MessageBuffer readMessageBuffer) {
        this.messageBuffer = readMessageBuffer;
        this.nextMessage = messageBuffer.getMessage();
        this.nextMessage.metaData = new HttpHeaders();
    }

    @Override
    public void read(SocketWrapper socket, ByteBuffer byteBuffer) throws IOException {
        int bytesRead = socket.read(byteBuffer);
        byteBuffer.flip();
        if (byteBuffer.remaining() == 0) {
            byteBuffer.clear();
            return;
        }
        this.nextMessage.writeToMessage(byteBuffer);
        int endIndex = HttpUtil.parseHttpRequest(this.nextMessage.data.array()
            , this.nextMessage.data.offset()
            , this.nextMessage.data.offset() + this.nextMessage.data.length()
            , (HttpHeaders) this.nextMessage.metaData);
        if (endIndex != -1) {
            messageBuffer.freeCurrentMessage(nextMessage);
            completeMessages.add(nextMessage);

            if (endIndex < nextMessage.data.offset() + nextMessage.data.length()) {

                Message message = this.messageBuffer.getMessage();
                message.metaData = new HttpHeaders();
                message.writePartialMessageToMessage(nextMessage, endIndex);

                nextMessage = message;
            }
        }
        byteBuffer.clear();
    }


    @Override
    public List<Message> getMessages() {
        return this.completeMessages;
    }

}
