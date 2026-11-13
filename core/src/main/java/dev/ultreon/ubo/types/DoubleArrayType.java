package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypes;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class DoubleArrayType implements ArrayType<double[], Double> {
    private double[] obj;

    public DoubleArrayType(double[] obj) {
        this.obj = obj;
    }

    public DoubleArrayType(int size) {
        this.obj = new double[size];
    }

    @Override
    public double[] getValue() {
        return this.obj;
    }

    @Override
    public void setValue(double[] obj) {
        if (obj == null) throw new IllegalArgumentException("Value can't be set to null");
        this.obj = obj;
    }

    @Override
    public int id() {
        return DataTypes.DOUBLE_ARRAY;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.obj.length);
        for (var i : this.obj) {
            output.writeDouble(i);
        }
    }

    public static DoubleArrayType read(DataInput input) throws IOException {
        var len = input.readInt();
        var arr = new double[len];
        for (var i = 0; i < len; i++) {
            arr[i] = input.readDouble();
        }
        return new DoubleArrayType(arr);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DoubleArrayType)) return false;
        var that = (DoubleArrayType) other;
        return Arrays.equals(this.obj, that.obj);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.obj);
    }

    @Override
    public DoubleArrayType copy() {
        return new DoubleArrayType(this.obj.clone());
    }

    @Override
    public int size() {
        return this.obj.length;
    }

    @Override
    public Double get(int index) {
        return this.obj[index];
    }

    @Override
    public void set(int index, Double value) {
        this.obj[index] = value;
    }

    public double getDouble(int index) {
        return this.obj[index];
    }

    public void set(int index, double value) {
        this.obj[index] = value;
    }

    @Override
    public String writeUso() {
        var builder = new StringBuilder("(d;");
        for (var v : this.obj) {
            builder.append(v).append(",");
        }

        if (this.obj.length > 0) {
            return builder.substring(0, builder.length() - 1) + ")";
        }

        return builder.append(")").toString();
    }

    @Override
    public String toString() {
        return this.writeUso();
    }

    @Override
    public @NotNull Iterator<Double> iterator() {
        return new DoubleIterator(this.obj);
    }

    public static class DoubleIterator implements Iterator<Double> {
        private final double[] obj;
        private int index;

        public DoubleIterator(double[] obj) {
            this.obj = obj;
        }

        @Override
        public boolean hasNext() {
            return this.index < this.obj.length;
        }

        @Override
        public Double next() {
            return this.obj[this.index++];
        }

        public double nextDouble() {
            return this.obj[this.index++];
        }
    }
}
