package ir.sooall.message.reader;

import ir.sooall.SocketWrapper;
import ir.sooall.message.Message;
import ir.sooall.message.ds.MessageBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public interface MessageReader {

    void init(MessageBuffer readMessageBuffer);

    void read(SocketWrapper socket, ByteBuffer byteBuffer) throws IOException;

    List<Message> getMessages();

}
