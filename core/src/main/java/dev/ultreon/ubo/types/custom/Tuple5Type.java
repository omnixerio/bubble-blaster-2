package dev.ultreon.ubo.types.custom;

import dev.ultreon.ubo.types.DataType;

import java.util.function.Function;

public interface Tuple5Type<T, A, B, C, D, E> extends DataType<T> {
    A getA();
    B getB();
    C getC();
    D getD();
    E getE();

    void setA(A a);
    void setB(B b);
    void setC(C c);
    void setD(D d);
    void setE(E e);

    default <R> R mapA(Function<A, R> mapper) {
        return mapper.apply(this.getA());
    }

    default <R> R mapB(Function<B, R> mapper) {
        return mapper.apply(this.getB());
    }

    default <R> R mapC(Function<C, R> mapper) {
        return mapper.apply(this.getC());
    }

    default <R> R mapD(Function<D, R> mapper) {
        return mapper.apply(this.getD());
    }

    default <R> R mapE(Function<E, R> mapper) {
        return mapper.apply(this.getE());
    }
}
