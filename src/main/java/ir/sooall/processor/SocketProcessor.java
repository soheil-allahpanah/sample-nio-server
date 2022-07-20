package ir.sooall.processor;

import ir.sooall.SocketContainer;
import ir.sooall.message.MessageProcessor;
import ir.sooall.message.MessageProcessorImpl;
import ir.sooall.thread.CustomThreadFactory;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.concurrent.*;

public class SocketProcessor {

    private boolean stopped;
    private final ExecutorService executor;

    public SocketProcessor(CustomThreadFactory customThreadFactory) {
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), customThreadFactory);
        this.stopped = false;
    }

    class Processor implements Runnable {

        private final MessageProcessor messageProcessor;

        public Processor(MessageProcessor messageProcessor) {
            this.messageProcessor = messageProcessor;
        }

        @Override
        public void run() {
            try {
                var readerSocketProcessor = new ReaderSocketProcessor(SocketContainer.SocketContainerHolder.getSocketContainer(), messageProcessor);
                var writerSocketProcessor = new WriterSocketProcessor(readerSocketProcessor);
                Selector readSelector = Selector.open();
                Selector writeSelector = Selector.open();
                while (!stopped) {
                    readerSocketProcessor.takeNewSockets(readSelector);
                    readerSocketProcessor.readFromSockets(readSelector);
                    writerSocketProcessor.writeToSockets(writeSelector);
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
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            executor.submit(new SocketProcessor.Processor(new MessageProcessorImpl(10)));
        }
    }

}
