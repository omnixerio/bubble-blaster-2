package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypes;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class BooleanArrayType implements ArrayType<boolean[], Boolean> {
    private boolean[] obj;

    public BooleanArrayType(boolean[] obj) {
        this.obj = obj;
    }

    public BooleanArrayType(int len) {
        this.obj = new boolean[len];
    }

    public BooleanArrayType(boolean[] obj, int len) {
        this.obj = Arrays.copyOf(obj, len);
    }

    public BooleanArrayType(Boolean[] array) {
        this.obj = new boolean[array.length];
        for (var i = 0; i < array.length; i++) {
            this.obj[i] = array[i];
        }
    }

    @Override
    public boolean[] getValue() {
        return this.obj;
    }

    @Override
    public void setValue(boolean[] obj) {
        if (obj == null) throw new IllegalArgumentException("Value can't be set to null");
        this.obj = obj;
    }

    @Override
    public int id() {
        return DataTypes.BOOLEAN_ARRAY;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.obj.length);
        for (var i = 0; i < this.obj.length; i += 4) {
            var b = this.obj[i];
            var b1 = i + 1 < this.obj.length && this.obj[i + 1];
            var b2 = i + 2 < this.obj.length && this.obj[i + 2];
            var b3 = i + 3 < this.obj.length && this.obj[i + 3];
            var b4 = i + 4 < this.obj.length && this.obj[i + 4];
            var b5 = i + 5 < this.obj.length && this.obj[i + 5];
            var b6 = i + 6 < this.obj.length && this.obj[i + 6];
            var b7 = i + 7 < this.obj.length && this.obj[i + 7];
            output.writeByte(
                    (b ? 1 : 0) | (b1 ? 2 : 0) | (b2 ? 4 : 0) | (b3 ? 8 : 0) |
                            (b4 ? 16 : 0) | (b5 ? 32 : 0) | (b6 ? 64 : 0) | (b7 ? 128 : 0)
            );
        }
    }

    public static BooleanArrayType read(DataInput input) throws IOException {
        var len = input.readInt();
        var arr = new boolean[len];
        for (var i = 0; i < len; i += 4) {
            var b = input.readByte();
            arr[i] = (b & 1) == 1;
            if (i + 1 < len) arr[i + 1] = (b & 2) == 2;
            if (i + 2 < len) arr[i + 2] = (b & 4) == 4;
            if (i + 3 < len) arr[i + 3] = (b & 8) == 8;
            if (i + 4 < len) arr[i + 4] = (b & 16) == 16;
            if (i + 5 < len) arr[i + 5] = (b & 32) == 32;
            if (i + 6 < len) arr[i + 6] = (b & 64) == 64;
            if (i + 7 < len) arr[i + 7] = (b & 128) == 128;
        }
        return new BooleanArrayType(arr);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BooleanArrayType)) return false;
        var that = (BooleanArrayType) other;
        return Arrays.equals(this.obj, that.obj);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.obj);
    }

    @Override
    public BooleanArrayType copy() {
        return new BooleanArrayType(this.obj.clone());
    }

    @Override
    public String writeUso() {
        var builder = new StringBuilder("(z;");
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
    public @NotNull Boolean get(int index) {
        return this.obj[index];
    }

    @Override
    public void set(int index, Boolean value) {
        this.obj[index] = value;
    }

    public boolean getBoolean(int index) {
        return this.obj[index];
    }

    public void set(int index, boolean value) {
        this.obj[index] = value;
    }

    @Override
    public String toString() {
        return this.writeUso();
    }

    @Override
    public @NotNull BooleanIterator iterator() {
        return new BooleanIterator(this.obj);
    }

    public static class BooleanIterator implements Iterator<@NotNull Boolean> {
        private final boolean[] obj;
        private int index;

        public BooleanIterator(boolean[] obj) {
            this.obj = obj;
        }

        @Override
        public boolean hasNext() {
            return this.index < this.obj.length;
        }

        @Override
        @Deprecated
        public Boolean next() {
            return this.obj[this.index++];
        }

        public boolean nextBoolean() {
            return this.obj[this.index++];
        }
    }
}
