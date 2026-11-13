package dev.ultreon.ubo.types.custom;

import dev.ultreon.ubo.types.DataType;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public interface Vector4Type<T, V> extends DataType<T>, VectorType<V> {
    V getX();
    V getY();
    V getZ();
    V getW();

    void setX(V x);
    void setY(V y);
    void setZ(V z);
    void setW(V w);

    @Override
    default int size() {
        return 4;
    }

    default void set(V x, V y, V z, V w) {
        this.setX(x);
        this.setY(y);
        this.setZ(z);
        this.setW(w);
    }

    @Override
    default V get(int index) {
        if (index == 0) return this.getX();
        if (index == 1) return this.getY();
        if (index == 2) return this.getZ();
        if (index == 3) return this.getW();
        throw new IndexOutOfBoundsException("Index out of bounds: " + index);
    }

    @Override
    default void set(int index, V value) {
        if (index == 0) this.setX(value);
        if (index == 1) this.setY(value);
        if (index == 2) this.setZ(value);
        if (index == 3) this.setW(value);
        throw new IndexOutOfBoundsException("Index out of bounds: " + index);
    }

    default void fill(V value) {
        this.set(value, value, value, value);
    }

    @Override
    @NotNull
    default Iterator<V> iterator() {
        return new Vector4Iterator<>(this);
    }
}

class Vector4Iterator<T, V> implements Iterator<V> {
    private final Vector4Type<T, V> vector;
    private int index;
    public Vector4Iterator(Vector4Type<T, V> vector) {
        this.vector = vector;
        this.index = 0;
    }
    @Override
    public boolean hasNext() {
        return this.index < 4;
    }
    @Override
    public V next() {
        if (this.index >= 4) throw new NoSuchElementException("No more elements");
        if (this.index++ == 0) return this.vector.getX();
        if (this.index++ == 1) return this.vector.getY();
        if (this.index++ == 2) return this.vector.getZ();
        if (this.index++ == 3) return this.vector.getW();

        throw new NoSuchElementException("No more elements");
    }
}
