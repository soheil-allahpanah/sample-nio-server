package ir.sooall.message;

public class MessageData {
    private byte[] array;
    private int offset;
    private int length;
    private int capacity;

    public MessageData(byte[] array, int offset, int length, int capacity) {
        this.array = array;
        this.offset = offset;
        this.length = length;
        this.capacity = capacity;
    }

    public byte[] array() {
        return array;
    }

    public void array(byte[] data) {
        this.array = data;
    }

    public int offset() {
        return offset;
    }

    public void offset(int offset) {
        this.offset = offset;
    }

    public int length() {
        return length;
    }

    public void length(int length) {
        this.length = length;
    }

    public int capacity() {
        return capacity;
    }

    public void capacity(int capacity) {
        this.capacity = capacity;
    }
}
