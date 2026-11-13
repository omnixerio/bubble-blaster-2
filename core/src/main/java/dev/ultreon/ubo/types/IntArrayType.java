package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypes;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class IntArrayType implements ArrayType<int[], Integer> {
    private int[] obj;

    public IntArrayType(int[] obj) {
        this.obj = obj;
    }

    public IntArrayType(int size) {
        this.obj = new int[size];
    }

    @Override
    public int[] getValue() {
        return this.obj;
    }

    @Override
    public void setValue(int[] obj) {
        if (obj == null) throw new IllegalArgumentException("Value can't be set to null");
        this.obj = obj;
    }

    @Override
    public int id() {
        return DataTypes.INT_ARRAY;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.obj.length);
        for (var i : this.obj) {
            output.writeInt(i);
        }
    }

    public static IntArrayType read(DataInput input) throws IOException {
        var len = input.readInt();
        var arr = new int[len];
        for (var i = 0; i < len; i++) {
            arr[i] = input.readInt();
        }
        return new IntArrayType(arr);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof IntArrayType)) return false;
        var that = (IntArrayType) other;
        return Arrays.equals(this.obj, that.obj);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.obj);
    }

    @Override
    public IntArrayType copy() {
        return new IntArrayType(this.obj.clone());
    }

    @Override
    public int size() {
        return this.obj.length;
    }

    @Override
    public Integer get(int index) {
        return this.obj[index];
    }

    @Override
    public void set(int index, Integer value) {
        this.obj[index] = value;
    }

    public int getInt(int index) {
        return this.obj[index];
    }

    public void set(int index, int value) {
        this.obj[index] = value;
    }

    @Override
    public String writeUso() {
        var builder = new StringBuilder("(i;");
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
    public @NotNull IntIterator iterator() {
        return new IntIterator(this.obj);
    }

    public static class IntIterator implements Iterator<Integer> {
        private final int[] obj;
        private int index;

        public IntIterator(int[] obj) {
            this.obj = obj;
        }

        @Override
        public boolean hasNext() {
            return this.index < this.obj.length;
        }

        @Override
        public Integer next() {
            return this.obj[this.index++];
        }

        public int nextInt() {
            return this.obj[this.index++];
        }
    }
}
