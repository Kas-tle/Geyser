/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.display;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector4f;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.geysermc.geyser.impl.IdentifierImpl;
import org.geysermc.geyser.entity.properties.type.FloatProperty;
import org.geysermc.geyser.entity.properties.type.IntProperty;
import org.geysermc.geyser.entity.properties.type.PropertyType;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SlotDisplayEntity extends Entity {
    private static final Map<String, FloatProperty> TRANSFORM_PROPERTIES = createTransformProperties();
    public static final IntProperty REVISION = integerProperty("revision", 1000000);
    public static final IntProperty DELAY = new IntProperty(IdentifierImpl.parse("geyser:delay"), 1000000, -1000000, 0);
    public static final FloatProperty DURATION = new FloatProperty(IdentifierImpl.parse("geyser:duration"), 50000, 0, 0f);
    public static final IntProperty RENDER_PROFILE = integerProperty("render_profile", 1000000);
    public static final IntProperty DISPLAY_CONTEXT = integerProperty("display_context", 8);

    protected ItemData hand = ItemData.AIR;
    private int revision;
    private boolean transformDirty;

    public SlotDisplayEntity(EntitySpawnContext context) {
        super(context);
    }

    private static IntProperty integerProperty(String name, int max) {
        return new IntProperty(IdentifierImpl.parse("geyser:" + name), max, 0, 0);
    }

    private static Map<String, FloatProperty> createTransformProperties() {
        Map<String, FloatProperty> properties = new LinkedHashMap<>();
        for (String name : List.of("tx", "ty", "tz", "sx", "sy", "sz", "lx", "ly", "lz", "lw", "rx", "ry", "rz", "rw")) {
            properties.put(name, new FloatProperty(IdentifierImpl.parse("geyser:" + name), 1000000, -1000000, name.startsWith("s") || name.endsWith("w") ? 1f : 0f));
        }
        return properties;
    }

    public static List<PropertyType<?, ?>> properties() {
        var properties = new java.util.ArrayList<PropertyType<?, ?>>(TRANSFORM_PROPERTIES.values());
        properties.addAll(List.of(REVISION, DELAY, DURATION, RENDER_PROFILE, DISPLAY_CONTEXT));
        return List.copyOf(properties);
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        setFlag(EntityFlag.HAS_GRAVITY, false);
        setFlag(EntityFlag.HAS_COLLISION, false);
    }

    @Override
    public void spawnEntity() {
        flushRevision();
        super.spawnEntity();
        updateMainHand();
    }

    protected void updateMainHand() {
        if (!valid) {
            return;
        }
        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setRuntimeEntityId(geyserId);
        packet.setItem(hand);
        packet.setHotbarSlot(0);
        packet.setInventorySlot(0);
        packet.setContainerId(ContainerId.INVENTORY);
        session.sendUpstreamPacket(packet);
    }

    private void flushRevision() {
        if (transformDirty) {
            revision = revision == 1000000 ? 0 : revision + 1;
            REVISION.apply(propertyManager, revision);
            transformDirty = false;
        }
    }

    @Override
    public void updateBedrockMetadata() {
        flushRevision();
        super.updateBedrockMetadata();
    }

    @Override
    public void updateBedrockEntityProperties() {
        flushRevision();
        super.updateBedrockEntityProperties();
    }

    private void setVector(String prefix, Vector3f vector) {
        if (vector == null || !Float.isFinite(vector.getX()) || !Float.isFinite(vector.getY()) || !Float.isFinite(vector.getZ())) {
            return;
        }
        String[] axes = {"x", "y", "z"};
        float[] values = {vector.getX(), vector.getY(), vector.getZ()};
        for (int i = 0; i < axes.length; i++) {
            TRANSFORM_PROPERTIES.get(prefix + axes[i]).apply(propertyManager, Math.clamp(values[i], -1000000f, 1000000f));
        }
        transformDirty = true;
    }

    public void setTranslation(EntityMetadata<Vector3f, ?> metadata) {
        Vector3f value = metadata.getValue();
        if (value != null) {
            // Model X points in the opposite direction to Java; animation Y points down.
            setVector("t", Vector3f.from(-value.getX(), -value.getY(), value.getZ()));
        }
    }

    public void setScale(EntityMetadata<Vector3f, ?> metadata) {
        setVector("s", metadata.getValue());
    }

    private void setQuaternion(String prefix, Quaternionf quaternion) {
        Vector4f value = DisplayRotation.toBedrockQuaternion(quaternion);
        String[] axes = {"x", "y", "z", "w"};
        float[] values = {value.getX(), value.getY(), value.getZ(), value.getW()};
        for (int i = 0; i < axes.length; i++) {
            TRANSFORM_PROPERTIES.get(prefix + axes[i]).apply(propertyManager, values[i]);
        }
        transformDirty = true;
    }

    public void setLeftRotation(EntityMetadata<Quaternionf, ?> metadata) {
        if (metadata.getValue() != null) {
            setQuaternion("l", metadata.getValue());
        }
    }

    public void setRightRotation(EntityMetadata<Quaternionf, ?> metadata) {
        if (metadata.getValue() != null) {
            setQuaternion("r", metadata.getValue());
        }
    }

    public void setInterpolationDelay(IntEntityMetadata metadata) {
        DELAY.apply(propertyManager, Math.clamp(metadata.getPrimitiveValue(), -1000000, 1000000));
        transformDirty = true;
    }

    public void setInterpolationDuration(IntEntityMetadata metadata) {
        DURATION.apply(propertyManager, Math.clamp(metadata.getPrimitiveValue() / 20f, 0, 50000));
        transformDirty = true;
    }

    /** Selects a correction generated from a custom pack's renderer factors; zero enables automatic selection. */
    public void setRenderProfile(int profile) {
        if (profile < 0 || profile > 1000000) {
            throw new IllegalArgumentException("Display profile must be between 0 and 1000000");
        }
        RENDER_PROFILE.apply(propertyManager, profile);
        updateBedrockEntityProperties();
    }
}
