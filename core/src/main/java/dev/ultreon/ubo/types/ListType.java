package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypeRegistry;
import dev.ultreon.ubo.DataTypes;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListType<T extends DataType<?>> implements DataType<List<T>>, List<T> {
    private final int id;
    Class<T> componentType;
    private List<T> obj;

    public ListType(int id) {
        this(new ArrayList<>(), id);
    }

    @SafeVarargs
    public ListType(T... type) {
        this((Class<T>) type.getClass().getComponentType());

        this.setValue(Arrays.asList(type));
    }

    @SafeVarargs
    public ListType(List<T> obj, T... type) {
        this(obj, (Class<T>) type.getClass().getComponentType());
    }

    public ListType(Class<? extends DataType<?>> type) {
        this(new ArrayList<>(), DataTypeRegistry.getIdOrThrow(type));
    }

    public ListType(List<T> obj, Class<T> type) {
        this.obj = obj;
        this.id = DataTypeRegistry.getIdOrThrow(type);
        this.componentType = type;
    }

    private ListType(List<T> list, int id) {
        this.setValue(list);
        this.id = id;
        this.componentType = (Class<T>) DataTypeRegistry.getType(id);
    }

    @Override
    public List<T> getValue() {
        return this.obj;
    }

    @Override
    public void setValue(List<T> obj) {
        var id = -1;
        List<T> list = new ArrayList<>(obj.size());
        for (int i = 0, objSize = obj.size(); i < objSize; i++) {
            var iType = obj.get(i);
            if (id == -1) {
                id = iType.id();
            }
            if (id != iType.id()) {
                throw new IllegalArgumentException("Type at index " + i + " has invalid id: " + iType.id() + " (expected " + id + ")");
            }

            list.add(iType);
        }

        this.obj = list;
    }

    @Override
    public int id() {
        return DataTypes.LIST;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeByte(this.id);
        output.writeInt(this.obj.size());
        for (DataType<?> l : this.obj) {
            l.write(output);
        }
    }

    public static ListType<?> read(DataInput input) throws IOException {
        var id = input.readUnsignedByte();
        var len = input.readInt();
        List<DataType<?>> list = new ArrayList<>(len);
        for (var i = 0; i < len; i++) {
            list.add(DataTypeRegistry.read(id, input));
        }

        return new ListType<>(list, id);
    }

    @Override
    public boolean add(T type) {
        if (type.id() != this.id)
            throw new IllegalArgumentException("Type has invalid id: " + type.id() + " (expected " + this.id + ")");
        return this.obj.add(type);
    }

    @Override
    public boolean remove(Object o) {
        return this.obj.remove(o);
    }

    @Override
    @SuppressWarnings("SlowListContainsAll")
    public boolean containsAll(@NotNull Collection<?> c) {
        return this.obj.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        return this.obj.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends T> c) {
        return this.obj.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return this.obj.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return this.obj.retainAll(c);
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return this.obj.iterator();
    }

    @Override
    public Object @NotNull [] toArray() {
        return this.obj.toArray();
    }

    @Override
    public <T1> T1 @NotNull [] toArray(T1 @NotNull [] a) {
        return this.obj.toArray(a);
    }

    public T @NotNull [] toTypeArray() {
        var a = (T[]) Array.newInstance(this.componentType, this.obj.size());
        for (var i = 0; i < this.obj.size(); i++) {
            a[i] = this.obj.get(i);
        }
        return a;
    }

    @Override
    public @NotNull ListIterator<T> listIterator() {
        return this.obj.listIterator();
    }

    @Override
    public @NotNull ListIterator<T> listIterator(int index) {
        return this.obj.listIterator(index);
    }

    @Override
    public @NotNull List<T> subList(int fromIndex, int toIndex) {
        return this.obj.subList(fromIndex, toIndex);
    }

    public int type() {
        return this.id;
    }

    @SafeVarargs
    public final <C extends DataType<?>> ListType<C> cast(C... type) {
        return this.cast((Class<C>) type.getClass().getComponentType());
    }

    public final <C extends DataType<?>> ListType<C> cast(Class<C> type) {
        var cs = new ListType<C>(type);
        cs.setValue((List<C>) this.obj);
        return cs;
    }

    @Override
    public T get(int index) {
        return this.obj.get(index);
    }

    @Override
    public T remove(int index) {
        if (index >= this.obj.size())
            throw new IndexOutOfBoundsException("Index out of bounds: " + index);
        if (index < 0)
            index = this.obj.size() + index;
        if (index < 0)
            throw new IndexOutOfBoundsException("Index out of bounds: " + index);
        this.obj.remove(index);
        return null;
    }

    @Override
    @Deprecated
    public int indexOf(Object o) {
        if (!(o instanceof DataType<?>))
            return -1;
        var type = (DataType<?>) o;
        if (type.id() != this.id)
            throw new IllegalArgumentException("Type has invalid id: " + type.id() + " (expected " + this.id + ")");
        return this.obj.indexOf(o);
    }

    @Override
    @Deprecated
    public int lastIndexOf(Object o) {
        if (!(o instanceof DataType<?>))
            return -1;
        var type = (DataType<?>) o;
        if (type.id() != this.id)
            throw new IllegalArgumentException("Type has invalid id: " + type.id() + " (expected " + this.id + ")");
        return this.obj.lastIndexOf(o);
    }

    public int indexOf(T type) {
        return this.obj.indexOf(type);
    }

    public int lastIndexOf(T type) {
        return this.obj.lastIndexOf(type);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ListType)) return false;
        var listType = (ListType<?>) other;
        return this.id == listType.id && Objects.equals(this.obj, listType.obj);
    }

    @Override
    public int hashCode() {
        var result = this.id;
        result = 31 * result + this.obj.hashCode();
        return result;
    }

    @Override
    public ListType<T> copy() {
        return new ListType<>(this.obj.stream().map(t -> (T) t.copy()).collect(Collectors.toList()));
    }

    @Override
    public String writeUso() {
        var builder = new StringBuilder("[");
        for (var t : this)
            builder.append(t.writeUso()).append(", ");

        if (!this.obj.isEmpty())
            return builder.substring(0, builder.length() - 2) + "]";

        return builder + "]";
    }

    @Override
    public int size() {
        return this.obj.size();
    }

    @Override
    public void clear() {
        this.obj.clear();
    }

    @Override
    public T set(int index, T type) {
        if (type.id() != this.id)
            throw new IllegalArgumentException("Type at index " + index + " has invalid id: " + type.id() + " (expected " + this.id + ")");
        return this.obj.set(index, type);
    }

    @Override
    public void add(int index, T element) {
        if (element.id() != this.id)
            throw new IllegalArgumentException("Type at index " + index + " has invalid id: " + element.id() + " (expected " + this.id + ")");
        this.obj.add(index, element);
    }

    public T remove(T type) {
        this.obj.remove(type);
        return type;
    }

    public boolean contains(T type) {
        return this.obj.contains(type);
    }

    public <R> List<R> mapTo(Function<T, R> mapper) {
        return this.mapTo(mapper, new ArrayList<>());
    }

    public <R> List<R> mapTo(Function<T, R> mapper, List<R> list) {
        for (var t : this.obj) {
            var r = mapper.apply(t);
            list.add(r);
        }
        return list;
    }

    @Override
    public @NotNull Stream<T> stream() {
        return this.obj.stream();
    }

    @Override
    public @NotNull Stream<T> parallelStream() {
        return this.obj.parallelStream();
    }

    @Override
    public boolean isEmpty() {
        return this.obj.isEmpty();
    }

    @Override
    @Deprecated
    public boolean contains(Object o) {
        return this.obj.contains(o);
    }

    @Override
    public String toString() {
        return this.writeUso();
    }
}
