package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.BitSet;
import java.util.Objects;

public class BitSetType implements DataType<BitSet> {
    private BitSet obj;

    public BitSetType(byte[] bits) {
        this.obj = BitSet.valueOf(bits);
    }

    public BitSetType(long[] bits) {
        this.obj = BitSet.valueOf(bits);
    }

    public BitSetType(BitSet obj) {
        this.obj = obj;
    }

    public BitSetType(String bits) {
        this.obj = new BitSet(bits.length());
        for (var i = 0; i < bits.length(); i++) {
            this.obj.set(i, bits.charAt(i) == '1');
        }
    }

    @Override
    public BitSet getValue() {
        return this.obj;
    }

    @Override
    public void setValue(BitSet obj) {
        if (obj == null) throw new IllegalArgumentException("Value can't be set to null");
        this.obj = obj;
    }

    @Override
    public int id() {
        return DataTypes.BIT_SET;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        var arr = this.obj.toByteArray();
        if (arr.length > 65535) throw new IllegalArgumentException("Bitset is too big to be written");
        output.writeShort(arr.length);
        for (var b : arr) {
            output.writeByte(b);
        }
    }

    public static BitSetType read(DataInput input) throws IOException {
        var len = input.readUnsignedShort();
        var arr = new byte[len];
        for (var i = 0; i < len; i++) {
            arr[i] = input.readByte();
        }
        return new BitSetType(arr);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BitSetType)) return false;
        var uuidType = (BitSetType) other;
        return Objects.equals(this.obj, uuidType.obj);
    }

    @Override
    public int hashCode() {
        return this.obj.hashCode();
    }

    @Override
    public BitSetType copy() {
        return new BitSetType((BitSet) this.obj.clone());
    }

    @Override
    public String writeUso() {
        var builder = new StringBuilder("x");
        for (var i = 0; i < this.obj.length(); i++) {
            builder.append(this.obj.get(i) ? "1" : "0");
        }

        return builder + ";";
    }

    public void setBit(int index, boolean value) {
        if (value) this.obj.set(index);
        else this.obj.clear(index);
    }

    public boolean getBit(int index) {
        return this.obj.get(index);
    }

    public int length() {
        return this.obj.length();
    }

    public int cardinality() {
        return this.obj.cardinality();
    }

    public int nextSetBit(int fromIndex) {
        return this.obj.nextSetBit(fromIndex);
    }

    public int nextClearBit(int fromIndex) {
        return this.obj.nextClearBit(fromIndex);
    }

    public int previousSetBit(int fromIndex) {
        return this.obj.previousSetBit(fromIndex);
    }

    public int previousClearBit(int fromIndex) {
        return this.obj.previousClearBit(fromIndex);
    }

    @Override
    public String toString() {
        return this.writeUso();
    }
}
