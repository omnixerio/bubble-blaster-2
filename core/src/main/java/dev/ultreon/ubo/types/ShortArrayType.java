package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypes;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class ShortArrayType implements ArrayType<short[], Short> {
    private short[] obj;

    public ShortArrayType(short[] obj) {
        this.obj = obj;
    }

    public ShortArrayType(int size) {
        this.obj = new short[size];
    }

    @Override
    public short[] getValue() {
        return this.obj;
    }

    @Override
    public void setValue(short[] obj) {
        if (obj == null) throw new IllegalArgumentException("Value can't be set to null");
        this.obj = obj;
    }

    @Override
    public int id() {
        return DataTypes.SHORT_ARRAY;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.obj.length);
        for (int i : this.obj) {
            output.writeShort(i);
        }
    }

    public static ShortArrayType read(DataInput input) throws IOException {
        var len = input.readInt();
        var arr = new short[len];
        for (var i = 0; i < len; i++) {
            arr[i] = input.readShort();
        }
        return new ShortArrayType(arr);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ShortArrayType)) return false;
        var that = (ShortArrayType) other;
        return Arrays.equals(this.obj, that.obj);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.obj);
    }

    @Override
    public ShortArrayType copy() {
        return new ShortArrayType(this.obj.clone());
    }

    @Override
    public int size() {
        return this.obj.length;
    }

    @Override
    public Short get(int index) {
        return this.obj[index];
    }

    @Override
    public void set(int index, Short value) {
        this.obj[index] = value;
    }

    public short getShort(int index) {
        return this.obj[index];
    }

    public void set(int index, short value) {
        this.obj[index] = value;
    }

    @Override
    public String writeUso() {
        var builder = new StringBuilder("(s;");
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
    public @NotNull Iterator<Short> iterator() {
        return new ShortIterator(this.obj);
    }

    public static class ShortIterator implements Iterator<Short> {
        private final short[] obj;
        private int index;

        public ShortIterator(short[] obj) {
            this.obj = obj;
        }

        @Override
        public boolean hasNext() {
            return this.index < this.obj.length;
        }

        @Override
        public Short next() {
            return this.obj[this.index++];
        }

        public short nextShort() {
            return this.obj[this.index++];
        }
    }
}
