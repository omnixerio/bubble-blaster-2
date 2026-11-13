package dev.ultreon.ubo.types;

import dev.ultreon.ubo.DataTypeRegistry;
import dev.ultreon.ubo.DataTypes;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapType implements DataType<Map<String, DataType<?>>>, Map<String, DataType<?>> {
    private Map<String, DataType<?>> obj;

    public MapType() {
        this.obj = new HashMap<>();
    }

    public MapType(Map<String, DataType<?>> map) {
        this.setValue(map);
    }

    public MapType(String key, DataType<?> value) {
        this.obj = new HashMap<>();
        this.obj.put(key, value);
    }

    @Override
    public Map<String, DataType<?>> getValue() {
        return this.obj;
    }

    @Override
    public void setValue(Map<String, DataType<?>> obj) {
        this.obj = obj;
    }

    public Set<String> keys() {
        return this.obj.keySet();
    }

    public Set<Entry<String, DataType<?>>> entries() {
        return this.obj.entrySet();
    }

    public @NotNull Collection<DataType<?>> values() {
        return this.obj.values();
    }

    @Override
    public @NotNull Set<Entry<String, DataType<?>>> entrySet() {
        return this.obj.entrySet();
    }

    @Override
    public int id() {
        return DataTypes.MAP;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.obj.size());
        for (var e : this.obj.entrySet()) {
            output.writeUTF(e.getKey());
            var value = e.getValue();
            output.writeByte(value.id());
            value.write(output);
        }
    }

    public static MapType read(DataInput input) throws IOException {
        var len = input.readInt();
        Map<String, DataType<?>> map = new HashMap<>(len);
        for (var i = 0; i < len; i++) {
            var key = input.readUTF();
            var id = input.readUnsignedByte();
            map.put(key, DataTypeRegistry.read(id, input));
        }

        return new MapType(map);
    }

    public boolean contains(String key, int type) {
        var data = this.obj.get(key);

        return data != null && data.id() == type;
    }

    @SafeVarargs
    public final <T extends DataType<?>> boolean contains(String key, T... type) {
        var data = this.obj.get(key);

        return data != null && type.getClass().getComponentType().isAssignableFrom(data.getClass());
    }

    public DataType<?> put(String key, DataType<?> dataType) {
        return this.obj.put(key, dataType);
    }

    @Override
    @Deprecated
    public DataType<?> remove(Object key) {
        return this.obj.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends DataType<?>> m) {
        this.obj.putAll(m);
    }

    public void putByte(String key, byte value) {
        this.put(key, new ByteType(value));
    }

    public void putByte(String key, int value) {
        this.put(key, new ByteType((byte) value));
    }

    public void putShort(String key, short value) {
        this.put(key, new ShortType(value));
    }

    public void putShort(String key, int value) {
        this.put(key, new ShortType((short) value));
    }

    public void putInt(String key, int value) {
        this.put(key, new IntType(value));
    }

    public void putLong(String key, long value) {
        this.put(key, new LongType(value));
    }

    public void putBigInt(String key, BigInteger value) {
        this.put(key, new BigIntType(value));
    }

    public void putFloat(String key, float value) {
        this.put(key, new FloatType(value));
    }

    public void putDouble(String key, double value) {
        this.put(key, new DoubleType(value));
    }

    public void putBigDec(String key, BigDecimal value) {
        this.put(key, new BigDecType(value));
    }

    public void putChar(String key, char value) {
        this.put(key, new CharType(value));
    }

    public void putBoolean(String key, boolean value) {
        this.put(key, new BooleanType(value));
    }

    public void putString(String key, String value) {
        this.put(key, new StringType(value));
    }

    public void putByteArray(String key, byte[] value) {
        this.put(key, new ByteArrayType(value));
    }

    public void putShortArray(String key, short[] value) {
        this.put(key, new ShortArrayType(value));
    }

    public void putIntArray(String key, int[] value) {
        this.put(key, new IntArrayType(value));
    }

    public void putLongArray(String key, long[] value) {
        this.put(key, new LongArrayType(value));
    }

    public void putFloatArray(String key, float[] value) {
        this.put(key, new FloatArrayType(value));
    }

    public void putDoubleArray(String key, double[] value) {
        this.put(key, new DoubleArrayType(value));
    }

    public void putCharArray(String key, char[] value) {
        this.put(key, new CharArrayType(value));
    }

    public void putBitSet(String key, byte[] value) {
        this.put(key, new BitSetType(value));
    }

    public void putBitSet(String key, BitSet value) {
        this.put(key, new BitSetType(value));
    }

    public void putUUID(String key, UUID value) {
        this.put(key, new UUIDType(value));
    }

    public byte getByte(String key) {
        return this.getByte(key, (byte) 0);
    }

    public byte getByte(String key, byte def) {
        var dataType = this.get(key);
        if (dataType instanceof ByteType) {
            return ((ByteType) dataType).getByteValue();
        }
        return def;
    }

    public short getShort(String key) {
        return this.getShort(key, (byte) 0);
    }

    public short getShort(String key, short def) {
        var dataType = this.get(key);
        if (dataType instanceof ShortType) {
            return ((ShortType) dataType).getShortValue();
        }
        return def;
    }

    public int getInt(String key) {
        return this.getInt(key, 0);
    }

    public int getInt(String key, int def) {
        var dataType = this.get(key);
        if (dataType instanceof IntType) {
            return ((IntType) dataType).getIntValue();
        }
        return def;
    }

    public long getLong(String key) {
        return this.getLong(key, 0);
    }

    public long getLong(String key, long def) {
        var dataType = this.get(key);
        if (dataType instanceof LongType) {
            return ((LongType) dataType).getLongValue();
        }
        return def;
    }

    public BigInteger getBigInt(String key) {
        return this.getBigInt(key, BigInteger.ZERO);
    }

    public BigInteger getBigInt(String key, BigInteger def) {
        var dataType = this.get(key);
        if (dataType instanceof BigIntType) {
            return ((BigIntType) dataType).getValue();
        }
        return def;
    }

    public float getFloat(String key) {
        return this.getFloat(key, 0);
    }

    public float getFloat(String key, float def) {
        var dataType = this.get(key);
        if (dataType instanceof FloatType) {
            return ((FloatType) dataType).getFloatValue();
        }
        return def;
    }

    public double getDouble(String key) {
        return this.getDouble(key, 0);
    }

    public double getDouble(String key, double def) {
        var dataType = this.get(key);
        if (dataType instanceof DoubleType) {
            return ((DoubleType) dataType).getDoubleValue();
        }
        return def;
    }

    public BigDecimal getBigDec(String key) {
        return this.getBigDec(key, BigDecimal.ZERO);
    }

    public BigDecimal getBigDec(String key, BigDecimal def) {
        var dataType = this.get(key);
        if (dataType instanceof BigDecType) {
            return ((BigDecType) dataType).getValue();
        }
        return def;
    }

    public char getChar(String key) {
        return this.getChar(key, (char) 0);
    }

    public char getChar(String key, char def) {
        var dataType = this.get(key);
        if (dataType instanceof CharType) {
            return ((CharType) dataType).getCharValue();
        }
        return def;
    }

    public boolean getBoolean(String key) {
        return this.getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean def) {
        var dataType = this.get(key);
        if (dataType instanceof BooleanType) {
            return ((BooleanType) dataType).getBooleanValue();
        }
        return def;
    }

    public String getString(String key) {
        return this.getString(key, null);
    }

    public String getString(String key, String def) {
        var dataType = this.get(key);
        if (dataType instanceof StringType) {
            return ((StringType) dataType).getValue();
        }
        return def;
    }

    public byte[] getByteArray(String key) {
        return this.getByteArray(key, null);
    }

    public byte[] getByteArray(String key, byte[] def) {
        var dataType = this.get(key);
        if (dataType instanceof ByteArrayType) {
            return ((ByteArrayType) dataType).getValue();
        }
        return def;
    }

    public short[] getShortArray(String key) {
        return this.getShortArray(key, null);
    }

    public short[] getShortArray(String key, short[] def) {
        var dataType = this.get(key);
        if (dataType instanceof ShortArrayType) {
            return ((ShortArrayType) dataType).getValue();
        }
        return def;
    }

    public int[] getIntArray(String key) {
        return this.getIntArray(key, null);
    }

    public int[] getIntArray(String key, int[] def) {
        var dataType = this.get(key);
        if (dataType instanceof IntArrayType) {
            return ((IntArrayType) dataType).getValue();
        }
        return def;
    }

    public long[] getLongArray(String key) {
        return this.getLongArray(key, null);
    }

    public long[] getLongArray(String key, long[] def) {
        var dataType = this.get(key);
        if (dataType instanceof LongArrayType) {
            return ((LongArrayType) dataType).getValue();
        }
        return def;
    }

    public float[] getFloatArray(String key) {
        return this.getFloatArray(key, null);
    }

    public float[] getFloatArray(String key, float[] def) {
        var dataType = this.get(key);
        if (dataType instanceof FloatArrayType) {
            return ((FloatArrayType) dataType).getValue();
        }
        return def;
    }

    public double[] getDoubleArray(String key) {
        return this.getDoubleArray(key, null);
    }

    public double[] getDoubleArray(String key, double[] def) {
        var dataType = this.get(key);
        if (dataType instanceof DoubleArrayType) {
            return ((DoubleArrayType) dataType).getValue();
        }
        return def;
    }

    public char[] getCharArray(String key) {
        return this.getCharArray(key, null);
    }

    public char[] getCharArray(String key, char[] def) {
        var dataType = this.get(key);
        if (dataType instanceof CharArrayType) {
            return ((CharArrayType) dataType).getValue();
        }
        return def;
    }

    public BitSet getBitSet(String key) {
        return this.getBitSet(key, null);
    }

    public BitSet getBitSet(String key, BitSet def) {
        var dataType = this.get(key);
        if (dataType instanceof BitSetType) {
            return ((BitSetType) dataType).getValue();
        }
        return def;
    }

    public MapType getMap(String key) {
        return this.getMap(key, null);
    }

    public MapType getMap(String key, MapType def) {
        var dataType = this.get(key);
        if (dataType instanceof MapType) {
            return (MapType) dataType;
        }
        return def;
    }

    @SafeVarargs
    public final <T extends DataType<?>> ListType<T> getList(String key, T... type) {
        return this.getList(key, new ListType<>(type));
    }

    public <T extends DataType<?>> ListType<T> getList(String key, ListType<T> def) {
        var dataType = this.get(key);
        if (dataType instanceof ListType<?>) {
            var obj = (ListType<?>) dataType;
            if (obj.type() != def.type()) {
                return def;
            }
            return obj.cast(def.componentType);
        }
        return def;
    }

    public UUID getUUID(String key) {
        return this.getUUID(key, null);
    }

    public UUID getUUID(String key, UUID def) {
        var dataType = this.get(key);
        if (dataType instanceof UUIDType) {
            return ((UUIDType) dataType).getValue();
        }
        return def;
    }

    public DataType<?> get(String key) {
        return this.obj.get(key);
    }

    public <T extends DataType<?>> T get(String key, @NotNull T def) {
        var dataType = this.get(key);
        if (dataType == null) {
            return def;
        }
        return dataType.cast((Class<T>) def.getClass(), def);
    }

    public boolean remove(String key) {
        return this.obj.remove(key, this.get(key));
    }

    public boolean contains(String key) {
        return this.obj.containsKey(key);
    }

    public boolean containsValue(DataType<?> value) {
        return this.obj.containsValue(value);
    }

    public DataType<?> pop(String key) {
        return this.obj.remove(key);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MapType)) return false;
        var mapType = (MapType) other;
        return Objects.equals(this.obj, mapType.obj);
    }

    @Override
    public int hashCode() {
        return this.obj.hashCode();
    }

    @Override
    public MapType copy() {
        return new MapType(this.obj.entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().copy(), (a, b) -> b)));
    }

    public <R> Map<String, R> mapTo(Function<DataType<?>, R> mapper) {
        return this.mapTo(mapper, new HashMap<>());
    }

    public <R> Map<String, R> mapTo(Function<DataType<?>, R> mapper, Map<String, R> map) {
        for (var entry : this.obj.entrySet()) {
            if (map.put(entry.getKey(), mapper.apply(entry.getValue())) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }
        return map;
    }

    public <R> List<R> reduce(BiFunction<String, DataType<?>, R> reducer) {
        return this.reduce(reducer, new ArrayList<>());
    }

    public <R> List<R> reduce(BiFunction<String, DataType<?>, R> reducer, List<R> list) {
        for (var entry : this.obj.entrySet()) {
            list.add(reducer.apply(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    @Override
    public String writeUso() {
        var builder = new StringBuilder("{");
        for (var entry : this.obj.entrySet()) {
            builder.append("\"").append(entry.getKey().replace("\"", "\\\"")).append("\": ").append(entry.getValue().writeUso()).append(", ");
        }

        if (this.obj.size() > 1) {
            return builder.substring(0, builder.length() - 2) + "}";
        }

        return builder + "}";
    }

    public int size() {
        return this.obj.size();
    }

    public void clear() {
        this.obj.clear();
    }

    @Override
    public @NotNull Set<String> keySet() {
        return Collections.emptySet();
    }

    public boolean isEmpty() {
        return this.obj.isEmpty();
    }

    @Override
    @Deprecated
    public boolean containsKey(Object key) {
        return this.obj.containsKey(key);
    }

    @Override
    @Deprecated
    public boolean containsValue(Object value) {
        return this.obj.containsValue(value);
    }

    @Override
    @Deprecated
    public DataType<?> get(Object key) {
        return this.obj.get(key);
    }

    @Override
    public String toString() {
        return this.writeUso();
    }
}
