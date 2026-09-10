package com.softwarearchetypes.rules.core.config.reflection;

import java.util.Map;

public interface ValueCodec {

    boolean supports(Class<?> type);

    Object read(String prefix, Map<String, String> props);

    void write(String prefix, Object value, Map<String, String> out);
}
