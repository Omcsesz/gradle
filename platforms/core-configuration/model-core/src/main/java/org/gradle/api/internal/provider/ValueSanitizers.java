/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.provider;

import com.google.common.collect.ImmutableCollection;
import groovy.lang.GString;
import org.gradle.internal.Cast;
import org.gradle.internal.deprecation.DeprecationLogger;
import org.gradle.util.internal.GUtil;

import javax.annotation.Nullable;

public class ValueSanitizers {
    private static final ValueSanitizer<Object> STRING_VALUE_SANITIZER = new ValueSanitizer<Object>() {
        @Override
        @Nullable
        public Object sanitize(Class<?> valueType, @Nullable Object value) {
            if (value instanceof GString) {
                return value.toString();
            } else {
                return value;
            }
        }
    };

    private static final ValueSanitizer<Object> ENUM_VALUE_SANITIZER = new ValueSanitizer<Object>() {
        @Override
        @Nullable
        public Object sanitize(Class<?> valueType, @Nullable Object value) {
            if (value instanceof CharSequence) {
                DeprecationLogger.deprecateBehaviour("Assigning String value to Enum rich property.")
                    .willBecomeAnErrorInGradle10()
                    .withUpgradeGuideSection(8, "enum_rich_properties_with_string_values")
                    .nagUser();
            }
            return GUtil.toEnum(Cast.uncheckedCast(valueType), value);
        }
    };

    private static final ValueSanitizer<Object> LONG_VALUE_SANITIZER = new ValueSanitizer<Object>() {
        @Override
        @Nullable
        public Object sanitize(Class<?> valueType, @Nullable Object value) {
            if (value instanceof Integer) {
                return ((Integer) value).longValue();
            } else {
                return value;
            }
        }
    };

    private static final ValueSanitizer<Object> IDENTITY_SANITIZER = new ValueSanitizer<Object>() {
        @Override
        @Nullable
        public Object sanitize(Class<?> valueType, @Nullable Object value) {
            return value;
        }
    };

    private static final ValueCollector<Object> IDENTITY_VALUE_COLLECTOR = valueCollectorWithValueSanitizer(Object.class, IDENTITY_SANITIZER);
    private static final ValueCollector<Object> STRING_VALUE_COLLECTOR = valueCollectorWithValueSanitizer(String.class, STRING_VALUE_SANITIZER);
    private static final ValueCollector<Object> LONG_VALUE_COLLECTOR = valueCollectorWithValueSanitizer(Long.class, LONG_VALUE_SANITIZER);

    public static <T> ValueSanitizer<T> forType(@Nullable Class<? extends T> targetType) {
        if (String.class.equals(targetType)) {
            return Cast.uncheckedCast(STRING_VALUE_SANITIZER);
        } else if (Long.class.equals(targetType)) {
            return Cast.uncheckedCast(LONG_VALUE_SANITIZER);
        } else if (targetType != null && targetType.isEnum()) {
            return Cast.uncheckedCast(ENUM_VALUE_SANITIZER);
        } else {
            return Cast.uncheckedCast(IDENTITY_SANITIZER);
        }
    }

    public static <T> ValueCollector<T> collectorFor(@Nullable Class<? extends T> elementType) {
        if (String.class.equals(elementType)) {
            return Cast.uncheckedCast(STRING_VALUE_COLLECTOR);
        } else if (Long.class.equals(elementType)) {
            return Cast.uncheckedCast(LONG_VALUE_COLLECTOR);
        } else if (elementType != null && elementType.isEnum()) {
            return Cast.uncheckedCast(valueCollectorWithValueSanitizer(elementType, ENUM_VALUE_SANITIZER));
        } else {
            return Cast.uncheckedCast(IDENTITY_VALUE_COLLECTOR);
        }
    }

    private static ValueCollector<Object> valueCollectorWithValueSanitizer(Class<?> elementType, ValueSanitizer<Object> sanitizer) {
        return new ValueCollector<Object>() {
            @Override
            public void add(@Nullable Object value, ImmutableCollection.Builder<Object> dest) {
                dest.add(sanitizer.sanitize(Cast.uncheckedCast(elementType), value));
            }

            @Override
            public void addAll(Iterable<?> values, ImmutableCollection.Builder<Object> dest) {
                for (Object value : values) {
                    add(value, dest);
                }
            }
        };
    }
}
