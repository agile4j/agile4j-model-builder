package com.agile4j.model.builder;


import static com.fasterxml.jackson.core.JsonFactory.Feature.INTERN_FIELD_NAMES;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.sun.istack.internal.Nullable;

/**
 * @author w.vela
 */
public final class ObjectMapperUtils {

    private static final String EMPTY_JSON = "{}";
    private static final String EMPTY_ARRAY_JSON = "[]";
    /**
     * disable INTERN_FIELD_NAMES, 解决GC压力大、内存泄露的问题
     * @see <a href="https://jira.corp.kuaishou.com/browse/INFRAJAVA-552">JIRA</a>
     */
    private static final ObjectMapper MAPPER = new ObjectMapper(new JsonFactory().disable(INTERN_FIELD_NAMES))
            .registerModule(new GuavaModule());

    static {
        MAPPER.disable(FAIL_ON_UNKNOWN_PROPERTIES);
        MAPPER.enable(ALLOW_UNQUOTED_CONTROL_CHARS);
        MAPPER.enable(ALLOW_COMMENTS);
        MAPPER.registerModule(new ParameterNamesModule());
        MAPPER.registerModule(new KotlinModule());
        //MAPPER.registerModule(new ProtobufModule());
        /*
          Benchmark                                                Mode  Cnt    Score   Error   Units
          JasksonAfterBurnerBenchmark.photoFeedViewFast           thrpt    2   34.302          ops/ms
          JasksonAfterBurnerBenchmark.photoFeedViewNormal         thrpt    2   28.017          ops/ms
          JasksonAfterBurnerBenchmark.requestPropertyFast         thrpt    2  663.840          ops/ms
          JasksonAfterBurnerBenchmark.requestPropertyNormal       thrpt    2  565.343          ops/ms
          JasksonAfterBurnerBenchmark.requestPropertyParseFast    thrpt    2  720.691          ops/ms
          JasksonAfterBurnerBenchmark.requestPropertyParseNormal  thrpt    2  412.315          ops/ms

          benchmark by lijie02
         */
        //        MAPPER.registerModule(new AfterburnerModule().setUseValueClassLoader(false));
    }

    //private static final ForwardingObjectMapper FORWARDING_OBJECT_MAPPER = new ForwardingObjectMapper(MAPPER);

    public static String toJSON(@Nullable Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
            //throw new UncheckedJsonProcessingException(e);
        }
    }

    /**
     * 输出格式化好的json
     * 请不要在输出log时使用
     *
     * 一般只用于写结构化数据到ZooKeeper时使用（为了更好的可读性）
     */
    public static String toPrettyJson(@Nullable Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            //throw new UncheckedJsonProcessingException(e);
            throw new UncheckedIOException(e);
        }
    }

    public static void toJSON(@Nullable Object obj, OutputStream writer) {
        if (obj == null) {
            return;
        }
        try {
            MAPPER.writeValue(writer, obj);
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public static <T> T fromJSON(@Nullable byte[] bytes, Class<T> valueType) {
        if (bytes == null) {
            return null;
        }
        try {
            return MAPPER.readValue(bytes, valueType);
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    private static RuntimeException wrapException(IOException e) {
        if (e instanceof JsonProcessingException) {
            return new UncheckedIOException(e);
            //return new UncheckedJsonProcessingException((JsonProcessingException) e);
        } else {
            return new UncheckedIOException(e);
        }
    }

    public static <T> T fromJSON(@Nullable String json, Class<T> valueType) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, valueType);
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public static <T> T fromJSON(Object value, Class<T> valueType) {
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return fromJSON((String) value, valueType);
        } else if (value instanceof byte[]) {
            return fromJSON((byte[]) value, valueType);
        } else {
            return null;
        }
    }

    public static <T> T value(Object rawValue, Class<T> type) {
        return MAPPER.convertValue(rawValue, type);
    }

    public static <T> T update(T rawValue, String newProperty) {
        try {
            return MAPPER.readerForUpdating(rawValue).readValue(newProperty);
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public static <T> T value(Object rawValue, TypeReference<T> type) {
        return MAPPER.convertValue(rawValue, type);
    }

    public static <T> T value(Object rawValue, JavaType type) {
        return MAPPER.convertValue(rawValue, type);
    }

    public static <T> T unwrapJsonP(String raw, Class<T> type) {
        return fromJSON(unwrapJsonP(raw), type);
    }

    private static String unwrapJsonP(String raw) {
        raw = StringUtils.trim(raw);
        raw = StringUtils.removeEnd(raw, ";");
        raw = raw.substring(raw.indexOf('(') + 1);
        raw = raw.substring(0, raw.lastIndexOf(')'));
        raw = StringUtils.trim(raw);
        return raw;
    }

    public static <E, T extends Collection<E>> T fromJSON(String json,
            Class<? extends Collection> collectionType, Class<E> valueType) {
        if (StringUtils.isEmpty(json)) {
            json = EMPTY_ARRAY_JSON;
        }
        try {
            return MAPPER.readValue(json,
                    defaultInstance().constructCollectionType(collectionType, valueType));
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    /**
     * use {@link #fromJson(String)} instead
     */
    public static <K, V, T extends Map<K, V>> T fromJSON(String json, Class<? extends Map> mapType,
            Class<K> keyType, Class<V> valueType) {
        if (StringUtils.isEmpty(json)) {
            json = EMPTY_JSON;
        }
        try {
            return MAPPER.readValue(json,
                    defaultInstance().constructMapType(mapType, keyType, valueType));
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public static <T> T fromJSON(InputStream inputStream, Class<T> type) {
        try {
            return MAPPER.readValue(inputStream, type);
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public static <E, T extends Collection<E>> T fromJSON(byte[] bytes,
            Class<? extends Collection> collectionType, Class<E> valueType) {
        try {
            return MAPPER.readValue(bytes,
                    defaultInstance().constructCollectionType(collectionType, valueType));
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public static <E, T extends Collection<E>> T fromJSON(InputStream inputStream,
            Class<? extends Collection> collectionType, Class<E> valueType) {
        try {
            return MAPPER.readValue(inputStream,
                    defaultInstance().constructCollectionType(collectionType, valueType));
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public static Map<String, Object> fromJson(InputStream is) {
        return fromJSON(is, Map.class, String.class, Object.class);
    }

    public static Map<String, Object> fromJson(String string) {
        return fromJSON(string, Map.class, String.class, Object.class);
    }

    public static Map<String, Object> fromJson(byte[] bytes) {
        return fromJSON(bytes, Map.class, String.class, Object.class);
    }

    /**
     * use {@link #fromJson(byte[])} instead
     */
    public static <K, V, T extends Map<K, V>> T fromJSON(byte[] bytes, Class<? extends Map> mapType,
            Class<K> keyType, Class<V> valueType) {
        try {
            return MAPPER.readValue(bytes,
                    defaultInstance().constructMapType(mapType, keyType, valueType));
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    /**
     * use {@link #fromJson(InputStream)} instead
     */
    public static <K, V, T extends Map<K, V>> T fromJSON(InputStream inputStream,
            Class<? extends Map> mapType, Class<K> keyType, Class<V> valueType) {
        try {
            return MAPPER.readValue(inputStream,
                    defaultInstance().constructMapType(mapType, keyType, valueType));
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    /**
     * 注意，一般情况不推荐使用本方法，因为开销还是略大，可以选择的几个方式是：
     *
     *  1. 直接 {@link #fromJSON} 解析，适用于本身也是要解析JSON并生成DTO，并且是主要分支的场景
     *  2. 自己编写一个方法，只检查第一个字符，适合内部特定数据结构兼容的快速检查，并不需要完整的 JSON 检查
     */
    public static boolean isJSON(String jsonStr) {
        if (StringUtils.isBlank(jsonStr)) {
            return false;
        }
        try (JsonParser parser = new ObjectMapper().getFactory().createParser(jsonStr)) {
            while (parser.nextToken() != null) {
                // do nothing.
            }
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

    public static boolean isBadJSON(String jsonStr) {
        return !isJSON(jsonStr);
    }

    /**
     * 我们 delegate 了 ObjectMapper，此方法暴露的 mapper 为只读不可修改
     * 若此 mapper 不能满足你的需求，请在工程内单独维护 ObjectMapper 实例或联系董浩亮、王至前、w.vela
     *
     * @return forwardingObjectMapper
     */
    /*public static ObjectMapper mapper() {
        return FORWARDING_OBJECT_MAPPER;
    }*/

}