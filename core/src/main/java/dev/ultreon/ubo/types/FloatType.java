package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class FloatType implements DataType<Float> {
    private float obj;

    public FloatType(float obj) {
        this.obj = obj;
    }

    @Override
    public Float getValue() {
        return this.obj;
    }

    public float getFloatValue() {
        return this.obj;
    }

    @Override
    public void setValue(Float obj) {
        if (obj == null) throw new IllegalArgumentException("Value can't be set to null");
        this.obj = obj;
    }

    public void setValue(float val) {
        this.obj = val;
    }

    @Override
    public int id() {
        return DataTypes.FLOAT;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeFloat(this.obj);
    }

    public static FloatType read(DataInput input) throws IOException {
        return new FloatType(input.readFloat());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FloatType)) return false;
        var floatType = (FloatType) other;
        return Float.compare(this.obj, floatType.obj) == 0;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(this.obj);
    }

    @Override
    public FloatType copy() {
        return new FloatType(this.obj);
    }

    @Override
    public String writeUso() {
        return this.obj + "f";
    }

    @Override
    public String toString() {
        return this.writeUso();
    }
}
