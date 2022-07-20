package ir.sooall.message;

import ir.sooall.message.ds.MessageBuffer;

import java.nio.ByteBuffer;

public class Message {

    private final MessageBuffer messageBuffer;
    public long socketId = 0;
    public MessageData data;
    public Object metaData = null;

    public Message(MessageBuffer messageBuffer) {
        this.messageBuffer = messageBuffer;
    }

    public int writeToMessage(ByteBuffer byteBuffer) {
        int remaining = byteBuffer.remaining();
        while (data.length() + remaining > data.capacity()) {
            if (!this.messageBuffer.expandMessage(this)) {
                return -1;
            }
        }
        int bytesToCopy = Math.min(remaining, data.capacity() - data.length());
        byteBuffer.get(data.array(), data.offset() + data.length(), bytesToCopy);
        data.length(data.length() + bytesToCopy);

        return bytesToCopy;
    }

    public int writeToMessage(byte[] byteArray) {
        return writeToMessage(byteArray, 0, byteArray.length);
    }


    public int writeToMessage(byte[] byteArray, int offset, int length) {
        while (data.length() + length > data.capacity()) {
            if (!this.messageBuffer.expandMessage(this)) {
                return -1;
            }
        }
        int bytesToCopy = Math.min(length, data.capacity() - data.length());
        System.arraycopy(byteArray, offset, data.array(), data.offset() + data.length(), bytesToCopy);
        data.length(data.length() + bytesToCopy);
        return bytesToCopy;
    }

    public void writePartialMessageToMessage(Message message, int endIndex) {
        int lengthOfPartialMessage = (message.data.offset() + message.data.length()) - endIndex;
        if (lengthOfPartialMessage > 0) {
            System.arraycopy(message.data.array(), endIndex, data.array(), data.offset(), lengthOfPartialMessage);
        }
    }

    public int writeToByteBuffer(ByteBuffer byteBuffer) {
        return 0;
    }
}
