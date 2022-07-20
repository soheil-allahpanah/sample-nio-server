package ir.sooall.message;

import ir.sooall.message.ds.MessageBuffer;

import java.nio.charset.StandardCharsets;

public class MessageProcessorImpl implements MessageProcessor {

    private final MessageBuffer writeMessageBuffer = new MessageBuffer();

    private final long sleepTime;

    public MessageProcessorImpl(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    String httpResponse = """
        HTTP/1.1 200 OK\r
        Content-Length: 38\r
        Content-Type: text/html\r
        \r
        <html><body>Hello World!</body></html>""";


    @Override
    public Message process(Message req) {
        Message res = writeMessageBuffer.getMessage();
        byte[] httpResponseBytes = httpResponse.getBytes(StandardCharsets.UTF_8);
        res.socketId = req.socketId;
        res.writeToMessage(httpResponseBytes);
        writeMessageBuffer.freeCurrentMessage(res);
        if (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return res;
    }
}
