package ir.sooall.message.ds;


import ir.sooall.message.Message;
import ir.sooall.message.MessageData;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BiFunction;

public class MessageBuffer {

    public static int KB = 1024;
    private static final int CAPACITY_SMALL = 4 * KB;
    private static final int CAPACITY_MEDIUM = 128 * KB;
    private static final int CAPACITY_LARGE = 1024 * KB;
    private final TreeSet<MessageBufferChunk> chunks;

    public MessageBuffer() {
        chunks = new TreeSet<>(Comparator.comparingInt(o -> o.metaData().unitSize()));
        chunks.add(createMessageBufferChunk(new MessageBufferChunkMetaData(CAPACITY_SMALL, 1024)));
        chunks.add(createMessageBufferChunk(new MessageBufferChunkMetaData(CAPACITY_MEDIUM, 128)));
        chunks.add(createMessageBufferChunk(new MessageBufferChunkMetaData(CAPACITY_LARGE, 16)));
    }

    public MessageBuffer(List<MessageBufferChunkMetaData> chunkMetaDataList) {
        chunks = new TreeSet<>(Comparator.comparingInt(o -> o.metaData().unitSize()));
        for (MessageBufferChunkMetaData metaData : chunkMetaDataList) {
            chunks.add(createMessageBufferChunk(metaData));
        }
    }

    private static MessageBufferChunk createMessageBufferChunk(MessageBufferChunkMetaData metaData) {
        var messageBufferChunkIndices = new RingBuffer(metaData.count());
        var data = new byte[metaData.unitSize() * metaData.count()];
        for (int i = 0; i < data.length; i += metaData.unitSize()) {
            messageBufferChunkIndices.put(i);
        }
        return new MessageBufferChunk(metaData, messageBufferChunkIndices, data);
    }

    public Message getMessage() {
        int nextFreeSmallBlock = this.chunks.first().indices().take();
        if (nextFreeSmallBlock == -1) {
            return null;
        }
        Message message = new Message(this);
        message.data = new MessageData(this.chunks.first().data(), nextFreeSmallBlock, 0, this.chunks.first().metaData().unitSize());
        return message;
    }

    public boolean expandMessage(Message message) {
        return checkChunksBasedOnCapacity(message.data.capacity(), (current, next) -> {
            if (next != null) {
                return moveMessage(message, current, next);
            }
            return false;
        }, false);
    }

    public void freeCurrentMessage(Message message) {
        checkChunksBasedOnCapacity(message.data.capacity(), (current, next) -> {
            current.indices().put(message.data.offset());
            return null;
        }, null);
    }

    private <T> T checkChunksBasedOnCapacity(int capacity, BiFunction<MessageBufferChunk, MessageBufferChunk, T> callBack, T defaultValue) {
        var it = chunks.iterator();
        var current = it.next();
        while (it.hasNext()) {
            var next = it.next();
            if (capacity == current.metaData().unitSize()) {
                return callBack.apply(current, next);
            }
            current = next;
        }
        return defaultValue;
    }

    private boolean moveMessage(Message message, MessageBufferChunk current, MessageBufferChunk next) {
        int nextFreeBlock = next.indices().take();
        if (nextFreeBlock == -1) {
            return false;
        }
        System.arraycopy(message.data.array(), message.data.offset(), next.data(), nextFreeBlock, message.data.length());
        current.indices().put(message.data.offset());
        message.data.array(next.data());
        message.data.offset(nextFreeBlock);
        message.data.capacity(next.metaData().unitSize());
        return true;
    }
}
