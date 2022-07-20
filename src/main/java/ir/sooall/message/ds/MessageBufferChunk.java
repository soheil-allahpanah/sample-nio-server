package ir.sooall.message.ds;

public record MessageBufferChunk(MessageBufferChunkMetaData metaData, RingBuffer indices, byte[] data) {
}
