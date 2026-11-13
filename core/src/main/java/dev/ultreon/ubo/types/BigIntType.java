package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;

public class BigIntType implements DataType<BigInteger> {
    private BigInteger obj;

    public BigIntType(BigInteger obj) {
        this.obj = obj;
    }

    public BigIntType(String number) {
        this.obj = new BigInteger(number);
    }

    @Override
    public BigInteger getValue() {
        return this.obj;
    }

    @Override
    public void setValue(BigInteger obj) {
        if (obj == null) throw new IllegalArgumentException("Value can't be set to null");
        this.obj = obj;
    }

    @Override
    public int id() {
        return DataTypes.BIG_INT;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        var bytes = this.obj.toByteArray();
        output.writeInt(bytes.length);
        for (var aByte : bytes) {
            output.writeByte(aByte);
        }
    }

    public static BigIntType read(DataInput input) throws IOException {
        var len = input.readInt();
        var bytes = new byte[len];
        for (var i = 0; i < len; i++) {
            bytes[i] = input.readByte();
        }

        return new BigIntType(new BigInteger(bytes));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BigIntType)) return false;
        var that = (BigIntType) other;
        return this.obj.equals(that.obj);
    }

    @Override
    public int hashCode() {
        return this.obj.hashCode();
    }

    @Override
    public BigIntType copy() {
        return new BigIntType(this.obj);
    }

    @Override
    public String writeUso() {
        return this.obj.toString() + "I";
    }

    @Override
    public String toString() {
        return this.writeUso();
    }
}
