/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.entity.properties;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.entity.properties.type.BooleanProperty;
import org.geysermc.geyser.entity.properties.type.EnumProperty;
import org.geysermc.geyser.entity.properties.type.FloatProperty;
import org.geysermc.geyser.entity.properties.type.IntProperty;
import org.geysermc.geyser.entity.properties.type.PropertyType;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
@ToString
public class GeyserEntityProperties {
    private final String entityType;
    private final List<PropertyType> properties;
    private final Object2IntMap<String> propertyIndices;

    private GeyserEntityProperties(String entityType, List<PropertyType> properties,
            Object2IntMap<String> propertyIndices) {
        this.entityType = entityType;
        this.properties = properties;
        this.propertyIndices = propertyIndices;
    }

    public NbtMap toNbtMap() {
        NbtMapBuilder mapBuilder = NbtMap.builder();
        for (PropertyType property : properties) {
            mapBuilder = property.addToNbtMap(mapBuilder);
        }
        return mapBuilder.putString("type", entityType).build();
    }

    public @NonNull String getEntityType() {
        return entityType;
    }

    public @NonNull List<PropertyType> getProperties() {
        return properties;
    }

    public int getPropertyIndex(String name) {
        return propertyIndices.getOrDefault(name, -1);
    }

    public static class Builder {
        private String entityType;
        private final List<PropertyType> properties = new ArrayList<>();
        private final Object2IntMap<String> propertyIndices = new Object2IntOpenHashMap<>();

        public Builder entityType(@NonNull String entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder addIntProperty(@NonNull String name, int min, int max) {
            if (propertyIndices.containsKey(name)) {
                throw new IllegalArgumentException(
                        "Property with name " + name + " already exists for entity " + entityType);
            }
            PropertyType property = new IntProperty(name, min, max);
            this.properties.add(property);
            propertyIndices.put(name, properties.size() - 1);
            return this;
        }

        public Builder addIntProperty(@NonNull String name) {
            if (propertyIndices.containsKey(name)) {
                throw new IllegalArgumentException(
                        "Property with name " + name + " already exists for entity " + entityType);
            }
            PropertyType property = new IntProperty(name, Integer.MIN_VALUE, Integer.MAX_VALUE);
            this.properties.add(property);
            propertyIndices.put(name, properties.size() - 1);
            return this;
        }

        public Builder addFloatProperty(@NonNull String name, float min, float max) {
            if (propertyIndices.containsKey(name)) {
                throw new IllegalArgumentException(
                        "Property with name " + name + " already exists for entity " + entityType);
            }
            PropertyType property = new FloatProperty(name, min, max);
            this.properties.add(property);
            propertyIndices.put(name, properties.size() - 1);
            return this;
        }

        public Builder addFloatProperty(@NonNull String name) {
            if (propertyIndices.containsKey(name)) {
                throw new IllegalArgumentException(
                        "Property with name " + name + " already exists for entity " + entityType);
            }
            PropertyType property = new FloatProperty(name, Float.MIN_VALUE, Float.MAX_VALUE);
            this.properties.add(property);
            propertyIndices.put(name, properties.size() - 1);
            return this;
        }

        public Builder addBoolProperty(@NonNull String name) {
            if (propertyIndices.containsKey(name)) {
                throw new IllegalArgumentException(
                        "Property with name " + name + " already exists for entity " + entityType);
            }
            PropertyType property = new BooleanProperty(name);
            this.properties.add(property);
            propertyIndices.put(name, properties.size() - 1);
            return this;
        }

        public Builder addEnumProperty(@NonNull String name, List<String> values) {
            if (propertyIndices.containsKey(name)) {
                throw new IllegalArgumentException(
                        "Property with name " + name + " already exists for entity " + entityType);
            }
            PropertyType property = new EnumProperty(name, values);
            this.properties.add(property);
            propertyIndices.put(name, properties.size() - 1);
            return this;
        }

        public GeyserEntityProperties build() {
            if (entityType == null) {
                throw new IllegalArgumentException("Entity type must be set");
            }
            return new GeyserEntityProperties(entityType, properties, propertyIndices);
        }
    }
}
