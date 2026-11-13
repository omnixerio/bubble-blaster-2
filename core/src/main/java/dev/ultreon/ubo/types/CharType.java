package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class CharType implements DataType<Character> {
    private char obj;

    public CharType(char obj) {
        this.obj = obj;
    }

    @Override
    public Character getValue() {
        return this.obj;
    }

    public char getCharValue() {
        return this.obj;
    }

    @Override
    public void setValue(Character obj) {
        if (obj == null) throw new IllegalArgumentException("Value can't be set to null");
        this.obj = obj;
    }

    public void setValue(char val) {
        this.obj = val;
    }

    @Override
    public int id() {
        return DataTypes.CHAR;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeChar(this.obj);
    }

    public static CharType read(DataInput input) throws IOException {
        return new CharType(input.readChar());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CharType)) return false;
        var charType = (CharType) other;
        return this.obj == charType.obj;
    }

    @Override
    public int hashCode() {
        return this.obj;
    }

    @Override
    public CharType copy() {
        return new CharType(this.obj);
    }

    @Override
    public String writeUso() {
        return "'" + (this.obj == '\'' ? "\\'" : String.valueOf(this.obj)) + "'";
    }

    @Override
    public String toString() {
        return this.writeUso();
    }
}
