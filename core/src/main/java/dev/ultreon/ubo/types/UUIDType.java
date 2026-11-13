package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class UUIDType implements DataType<UUID> {
    private UUID obj;

    public UUIDType(UUID obj) {
        this.obj = obj;
    }

    @Override
    public UUID getValue() {
        return this.obj;
    }

    @Override
    public void setValue(UUID obj) {
        if (obj == null) throw new IllegalArgumentException("Value can't be set to null");
        this.obj = obj;
    }

    @Override
    public int id() {
        return DataTypes.UUID;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeLong(this.obj.getMostSignificantBits());
        output.writeLong(this.obj.getLeastSignificantBits());
    }

    public static UUIDType read(DataInput input) throws IOException {
        var msb = input.readLong();
        var lsb = input.readLong();
        return new UUIDType(new UUID(msb, lsb));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UUIDType)) return false;
        var uuidType = (UUIDType) other;
        return Objects.equals(this.obj, uuidType.obj);
    }

    @Override
    public int hashCode() {
        return this.obj.hashCode();
    }

    @Override
    public UUIDType copy() {
        return new UUIDType(this.obj);
    }

    @Override
    public String writeUso() {
        return '<' + this.obj.toString() + '>';
    }

    @Override
    public String toString() {
        return this.writeUso();
    }
}
