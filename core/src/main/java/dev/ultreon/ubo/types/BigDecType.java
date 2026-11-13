package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class BigDecType implements DataType<BigDecimal> {
    private BigDecimal obj;

    public BigDecType(BigDecimal obj) {
        this.obj = obj;
    }

    public BigDecType(String number) {
        this.obj = new BigDecimal(number);
    }

    @Override
    public BigDecimal getValue() {
        return this.obj;
    }

    @Override
    public void setValue(BigDecimal obj) {
        if (obj == null) throw new IllegalArgumentException("Value can't be set to null");
        this.obj = obj;
    }

    @Override
    public int id() {
        return DataTypes.BIG_DEC;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        var bytes = this.obj.unscaledValue().toByteArray();
        output.writeInt(bytes.length);
        output.writeInt(this.obj.scale());
        for (var aByte : bytes) {
            output.writeByte(aByte);
        }
    }

    public static BigDecType read(DataInput input) throws IOException {
        var len = input.readInt();
        var scale = input.readInt();
        var bytes = new byte[len];
        for (var i = 0; i < len; i++) {
            bytes[i] = input.readByte();
        }

        return new BigDecType(new BigDecimal(new BigInteger(bytes), scale));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BigDecType)) return false;
        var that = (BigDecType) other;
        return this.obj.equals(that.obj);
    }

    @Override
    public int hashCode() {
        return this.obj.hashCode();
    }

    @Override
    public BigDecType copy() {
        return new BigDecType(this.obj);
    }

    @Override
    public String writeUso() {
        return this.obj.toString() + "D";
    }

    @Override
    public String toString() {
        return this.writeUso();
    }
}
