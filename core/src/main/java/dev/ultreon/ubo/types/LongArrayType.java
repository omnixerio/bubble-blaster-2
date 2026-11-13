package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypes;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class LongArrayType implements ArrayType<long[], Long> {
    private long[] obj;

    public LongArrayType(long[] obj) {
        this.obj = obj;
    }

    public LongArrayType(int size) {
        this.obj = new long[size];
    }

    @Override
    public long[] getValue() {
        return this.obj;
    }

    @Override
    public void setValue(long[] obj) {
        if (obj == null) throw new IllegalArgumentException("Value can't be set to null");
        this.obj = obj;
    }

    @Override
    public int id() {
        return DataTypes.LONG_ARRAY;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.obj.length);
        for (var l : this.obj) {
            output.writeLong(l);
        }
    }

    public static LongArrayType read(DataInput input) throws IOException {
        var len = input.readInt();
        var arr = new long[len];
        for (var i = 0; i < len; i++) {
            arr[i] = input.readLong();
        }
        return new LongArrayType(arr);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LongArrayType)) return false;
        var that = (LongArrayType) other;
        return Arrays.equals(this.obj, that.obj);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.obj);
    }

    @Override
    public LongArrayType copy() {
        return new LongArrayType(this.obj.clone());
    }

    @Override
    public int size() {
        return this.obj.length;
    }

    @Override
    public Long get(int index) {
        return this.obj[index];
    }

    @Override
    public void set(int index, Long value) {
        this.obj[index] = value;
    }

    public long getLong(int index) {
        return this.obj[index];
    }

    public void set(int index, long value) {
        this.obj[index] = value;
    }

    @Override
    public String writeUso() {
        var builder = new StringBuilder("(l;");
        for (var v : this.obj) {
            builder.append(v).append(",");
        }

        return builder.substring(0, builder.length() - 1) + ")";
    }

    @Override
    public String toString() {
        return this.writeUso();
    }

    @Override
    public @NotNull Iterator<Long> iterator() {
        return new LongIterator(this.obj);
    }

    public static class LongIterator implements Iterator<Long> {
        private final long[] obj;
        private int index;

        public LongIterator(long[] obj) {
            this.obj = obj;
        }

        @Override
        public boolean hasNext() {
            return this.index < this.obj.length;
        }

        @Override
        public Long next() {
            return this.obj[this.index++];
        }

        public long nextLong() {
            return this.obj[this.index++];
        }
    }
}
