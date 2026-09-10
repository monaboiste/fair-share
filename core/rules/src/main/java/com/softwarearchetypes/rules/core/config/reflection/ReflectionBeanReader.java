package com.softwarearchetypes.rules.core.config.reflection;

import com.softwarearchetypes.rules.core.predicates.AndPredicate;
import com.softwarearchetypes.rules.core.predicates.LogicalPredicate;
import com.softwarearchetypes.rules.core.predicates.NotPredicate;
import com.softwarearchetypes.rules.core.predicates.OrPredicate;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Rebuilds a rule from stored parameters. Reflection instantiates a type from a stable key, casts it to a known
 * interface, and delegates value objects to registered {@link ValueCodec} implementations. It never invokes methods
 * named by user input.
 */
@SuppressWarnings("squid:S1192")
public class ReflectionBeanReader {

    private final Map<String, String> props;
    private final List<ValueCodec> codecs;

    public ReflectionBeanReader(Map<String, String> props, List<ValueCodec> codecs) {
        this.props = Objects.requireNonNull(props);
        this.codecs = List.copyOf(codecs);
    }

    public <T> T readBean(String prefix, Class<T> expectedType) {
        String classKey = prefix + ".class";
        String className = props.get(classKey);

        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("No entry '" + classKey + "' for type " + expectedType.getName());
        }

        Class<?> rawClass;
        try {
            rawClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot load class: " + className, e);
        }

        if (!expectedType.isAssignableFrom(rawClass)) {
            throw new IllegalArgumentException(
                    "Class " + className + " is not compatible with " + expectedType.getName());
        }

        @SuppressWarnings("unchecked")
        Class<? extends T> clazz = (Class<? extends T>) rawClass;

        ValueCodec codec = codecFor(clazz);
        if (codec != null) {
            @SuppressWarnings("unchecked")
            T value = (T) codec.read(prefix, props);
            return Objects.requireNonNull(value, "Codec returned null for " + prefix);
        }

        if (LogicalPredicate.class.isAssignableFrom(clazz)) {
            @SuppressWarnings("unchecked")
            T predicate = (T) readLogicalPredicate(prefix);
            return Objects.requireNonNull(predicate, "Logical predicate root is required for " + prefix);
        }

        if (clazz.isRecord()) {
            return instantiateRecord(prefix, clazz);
        } else {
            return instantiatePojo(prefix, clazz);
        }
    }

    @SuppressWarnings("squid:S3011")
    private <T> T instantiateRecord(String prefix, Class<T> clazz) {
        var components = clazz.getRecordComponents();
        Class<?>[] paramTypes = new Class<?>[components.length];
        @Nullable Object[] args = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            paramTypes[i] = component.getType();
            String argKey = prefix + ".arg" + i;
            args[i] = readValue(argKey, component.getType());
        }

        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create record " + clazz.getName(), e);
        }
    }

    private <T> T instantiatePojo(String prefix, Class<T> clazz) {
        Constructor<?> constructor = chooseConstructor(clazz);
        Parameter[] parameters = constructor.getParameters();
        @Nullable Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            arguments[i] = readParameter(prefix, parameters[i]);
        }

        try {
            @SuppressWarnings("unchecked")
            T instance = (T) constructor.newInstance(arguments);
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Could not create " + clazz.getName() + " from prefix '" + prefix + "'", e);
        }
    }

    private @Nullable Object readParameter(String prefix, Parameter parameter) {
        return readValue(prefix + "." + parameter.getName(), parameter.getType());
    }

    @SuppressWarnings("squid:S3011")
    private Constructor<?> chooseConstructor(Class<?> clazz) {
        Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        if (ctors.length == 0) {
            throw new IllegalStateException("Class " + clazz.getName() + " has no public constructor");
        }
        if (ctors.length == 1) {
            ctors[0].setAccessible(true);
            return ctors[0];
        }

        throw new IllegalStateException(
                "Class " + clazz.getName() + " has many constructors - specify it in ReflectionBeanReader");
    }

    /**
     * Rebuilds a logical predicate tree stored below the given prefix.
     *
     * <pre>{@code
     * prefix.root = n1
     * prefix.n1.type = AND | OR | NOT | LEAF
     * prefix.n1.left = n2
     * prefix.n1.right = n3
     * }</pre>
     *
     * @param basePrefix property prefix of the stored tree
     * @return the rebuilt predicate, or {@code null} when the root is absent
     */
    @SuppressWarnings("squid:S1452")
    public @Nullable LogicalPredicate<?> readLogicalPredicate(String basePrefix) {
        String rootId = props.get(basePrefix + ".root");
        if (rootId == null || rootId.isBlank()) {
            return null;
        }
        return readLogicalNode(basePrefix, rootId);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private LogicalPredicate<?> readLogicalNode(String basePrefix, String nodeId) {
        String nodePrefix = basePrefix + "." + nodeId;
        String type = props.get(nodePrefix + ".type");
        if (type == null) {
            throw new IllegalArgumentException("No '" + nodePrefix + ".type' for node " + nodeId);
        }

        return switch (type) {
            case "AND" -> {
                String leftId = props.get(nodePrefix + ".left");
                String rightId = props.get(nodePrefix + ".right");
                if (leftId == null || rightId == null) {
                    throw new IllegalArgumentException("No left/right for node AND: " + nodePrefix);
                }
                LogicalPredicate left = readLogicalNode(basePrefix, leftId);
                LogicalPredicate right = readLogicalNode(basePrefix, rightId);
                yield new AndPredicate<>(left, right);
            }
            case "OR" -> {
                String leftId = props.get(nodePrefix + ".left");
                String rightId = props.get(nodePrefix + ".right");
                if (leftId == null || rightId == null) {
                    throw new IllegalArgumentException("No left/right for node OR: " + nodePrefix);
                }
                LogicalPredicate left = readLogicalNode(basePrefix, leftId);
                LogicalPredicate right = readLogicalNode(basePrefix, rightId);
                yield new OrPredicate<>(left, right);
            }
            case "NOT" -> {
                String childId = props.get(nodePrefix + ".child");
                if (childId == null) {
                    throw new IllegalArgumentException("No child for node NOT: " + nodePrefix);
                }
                LogicalPredicate<?> child = readLogicalNode(basePrefix, childId);
                yield new NotPredicate<>(child);
            }
            case "LEAF" -> {
                String className = props.get(nodePrefix + ".class");
                if (className == null) {
                    throw new IllegalArgumentException("No " + nodePrefix + ".class for predicate leaf");
                }
                try {
                    Class<?> leafClass = Class.forName(className);
                    Object bean = leafClass.isRecord()
                            ? instantiateRecord(nodePrefix, leafClass)
                            : instantiatePojo(nodePrefix, leafClass);
                    if (!(bean instanceof LogicalPredicate<?> lp)) {
                        throw new IllegalArgumentException("Leaf " + nodePrefix + " of class " + className
                                + " does not implement LogicalPredicate");
                    }
                    yield lp;
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Could not load leaf class: " + className, e);
                }
            }
            default ->
                throw new IllegalArgumentException(
                        "Unknown type for logical node '" + type + "' for a prefix " + nodePrefix);
        };
    }

    private @Nullable ValueCodec codecFor(Class<?> type) {
        for (ValueCodec codec : codecs) {
            if (codec.supports(type)) {
                return codec;
            }
        }
        return null;
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type)
                || Enum.class.isAssignableFrom(type)
                || type == Boolean.class
                || type == Character.class
                || type == LocalDate.class
                || type == LocalDateTime.class
                || type == UUID.class;
    }

    private @Nullable Object readValue(String key, Class<?> targetType) {

        if (isSimpleType(targetType)) {
            String raw = props.get(key);
            if (raw == null) {
                if (targetType.isPrimitive()) {
                    throw new IllegalArgumentException("No value for primitive: " + key);
                }
                return null;
            }
            return convertSimple(raw, targetType);
        }

        ValueCodec codec = codecFor(targetType);
        if (codec != null) {
            return codec.read(key, props);
        }

        if (LogicalPredicate.class.isAssignableFrom(targetType)) {
            return readLogicalPredicate(key);
        }

        if (props.containsKey(key + ".class")) {
            return readBean(key, targetType);
        }

        if (targetType.isPrimitive()) {
            throw new IllegalArgumentException("No config for primitive: " + key);
        }
        return null;
    }

    private static final Map<Class<?>, Function<String, ?>> SIMPLE_CONVERTERS = Map.ofEntries(
            Map.entry(String.class, Function.identity()),
            Map.entry(int.class, Integer::parseInt),
            Map.entry(Integer.class, Integer::parseInt),
            Map.entry(long.class, Long::parseLong),
            Map.entry(Long.class, Long::parseLong),
            Map.entry(double.class, Double::parseDouble),
            Map.entry(Double.class, Double::parseDouble),
            Map.entry(boolean.class, Boolean::parseBoolean),
            Map.entry(Boolean.class, Boolean::parseBoolean),
            Map.entry(BigDecimal.class, BigDecimal::new),
            Map.entry(BigInteger.class, BigInteger::new),
            Map.entry(UUID.class, UUID::fromString),
            Map.entry(LocalDate.class, raw -> parseDate(raw, LocalDate::parse)),
            Map.entry(LocalDateTime.class, raw -> parseDate(raw, LocalDateTime::parse)));

    private Object convertSimple(String raw, Class<?> targetType) {
        Function<String, ?> converter = SIMPLE_CONVERTERS.get(targetType);
        if (converter != null) {
            return converter.apply(raw);
        }

        if (targetType.isEnum()) {
            return parseEnum(raw, targetType);
        }

        throw new IllegalArgumentException("Unsupported type: " + targetType.getName());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Enum<?> parseEnum(String raw, Class<?> targetType) {
        return Enum.valueOf((Class<? extends Enum>) targetType, raw);
    }

    private static <T> T parseDate(String raw, Function<String, T> parser) {
        try {
            return parser.apply(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Illegal date: " + raw, e);
        }
    }
}
