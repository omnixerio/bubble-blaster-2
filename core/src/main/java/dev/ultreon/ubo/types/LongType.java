package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class LongType implements DataType<Long> {
    private long obj;

    public LongType(long obj) {
        this.obj = obj;
    }

    @Override
    public Long getValue() {
        return this.obj;
    }

    public long getLongValue() {
        return this.obj;
    }

    @Override
    public void setValue(Long obj) {
        if (obj == null) throw new IllegalArgumentException("Value can't be set to null");
        this.obj = obj;
    }

    public void setValue(long val) {
        this.obj = val;
    }

    @Override
    public int id() {
        return DataTypes.LONG;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeLong(this.obj);
    }

    public static LongType read(DataInput input) throws IOException {
        return new LongType(input.readLong());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LongType)) return false;
        var longType = (LongType) other;
        return this.obj == longType.obj;
    }

    @Override
    public int hashCode() {
        return (int) (this.obj ^ this.obj >>> 32);
    }

    @Override
    public LongType copy() {
        return new LongType(this.obj);
    }

    @Override
    public String writeUso() {
        return this.obj + "l";
    }

    @Override
    public String toString() {
        return this.writeUso();
    }
}
