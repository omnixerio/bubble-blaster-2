package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypes;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

public class ByteArrayType implements ArrayType<byte[], Byte> {
    private byte[] obj;

    public ByteArrayType(byte[] obj) {
        this.obj = obj;
    }

    public ByteArrayType(ByteBuffer buffer) {
        this.obj = new byte[buffer.remaining()];
        buffer.get(this.obj);
    }

    public ByteArrayType(String str) {
        this.obj = str.getBytes(StandardCharsets.UTF_8);
    }

    public ByteArrayType(String str, Charset charset) {
        this.obj = str.getBytes(charset);
    }

    public ByteArrayType(int len) {
        this.obj = new byte[len];
    }

    public ByteArrayType(byte[] obj, int len) {
        this.obj = Arrays.copyOf(obj, len);
    }

    public ByteArrayType(Byte[] array) {
        this.obj = new byte[array.length];
        for (var i = 0; i < array.length; i++) {
            this.obj[i] = array[i];
        }
    }

    @Override
    public byte[] getValue() {
        return this.obj;
    }

    @Override
    public void setValue(byte[] obj) {
        if (obj == null) throw new IllegalArgumentException("Value can't be set to null");
        this.obj = obj;
    }

    @Override
    public int id() {
        return DataTypes.BYTE_ARRAY;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.obj.length);
        for (var b : this.obj) {
            output.writeByte(b);
        }
    }

    public static ByteArrayType read(DataInput input) throws IOException {
        var len = input.readInt();
        var arr = new byte[len];
        for (var i = 0; i < len; i++) {
            arr[i] = input.readByte();
        }
        return new ByteArrayType(arr);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ByteArrayType)) return false;
        var that = (ByteArrayType) other;
        return Arrays.equals(this.obj, that.obj);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.obj);
    }

    @Override
    public ByteArrayType copy() {
        return new ByteArrayType(this.obj.clone());
    }

    @Override
    public String writeUso() {
        var builder = new StringBuilder("(b;");
        for (var v : this.obj) {
            builder.append(v).append(",");
        }

        return builder.substring(0, builder.length() - 1) + ")";
    }

    @Override
    public int size() {
        return this.obj.length;
    }

    @Override
    public @NotNull Byte get(int index) {
        return this.obj[index];
    }

    @Override
    public void set(int index, Byte value) {
        this.obj[index] = value;
    }

    public byte getByte(int index) {
        return this.obj[index];
    }

    public void set(int index, byte value) {
        this.obj[index] = value;
    }

    @Override
    public String toString() {
        return this.writeUso();
    }

    @Override
    public @NotNull ByteIterator iterator() {
        return new ByteIterator(this.obj);
    }

    public static class ByteIterator implements Iterator<@NotNull Byte> {
        private final byte[] obj;
        private int index;

        public ByteIterator(byte[] obj) {
            this.obj = obj;
        }

        @Override
        public boolean hasNext() {
            return this.index < this.obj.length;
        }

        @Override
        @Deprecated
        public Byte next() {
            return this.obj[this.index++];
        }

        public byte nextByte() {
            return this.obj[this.index++];
        }
    }
}
