package org.geysermc.geyser.entity.type.display;

import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.entity.FloatEntityProperty;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.EntityTypeDefinition;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.impl.IdentifierImpl;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.type.GeyserBedrockBlock;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.scoreboard.network.util.GeyserMockContext;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SlotDisplayEntityTest {
    @BeforeAll
    static void initializeMetadataRegistry() {
        GeyserMockContext.mockContext(context -> {
            try {
                Class.forName("org.geysermc.geyser.entity.EntityDataBehaviorRegistry");
            } catch (ClassNotFoundException e) {
                throw new AssertionError(e);
            }
        });
    }

    private final GeyserSession session = mock(GeyserSession.class, RETURNS_DEEP_STUBS);
    private final List<String> names = SlotDisplayEntity.properties().stream().map(p -> p.identifier().toString()).toList();

    private SlotDisplayEntity entity() {
        return entity(SlotDisplayEntity::new);
    }

    private <T extends SlotDisplayEntity> T entity(Function<EntitySpawnContext, T> factory) {
        var properties = mock(GeyserEntityProperties.class);
        when(properties.getProperties()).thenReturn(SlotDisplayEntity.properties());
        when(properties.getPropertyIndex(anyString())).thenAnswer(call -> names.indexOf(call.getArgument(0)));
        var bedrock = mock(BedrockEntityDefinition.class);
        when(bedrock.registeredProperties()).thenReturn(properties);
        when(bedrock.identifier()).thenReturn(IdentifierImpl.parse("geyser:item_display"));
        var context = mock(EntitySpawnContext.class);
        when(context.session()).thenReturn(session);
        when(context.entityTypeDefinition()).thenReturn(mock(EntityTypeDefinition.class));
        when(context.bedrockEntityDefinition()).thenReturn(bedrock);
        when(context.position()).thenReturn(Vector3f.ZERO);
        when(context.motion()).thenReturn(Vector3f.ZERO);
        when(context.geyserId()).thenReturn(42L);
        return factory.apply(context);
    }

    @Test
    void sendsInitialEquipmentAfterSpawnEvenWhenItIsAir() {
        var entity = entity();
        entity.spawnEntity();
        var ordered = inOrder(session);
        ordered.verify(session).sendUpstreamPacket(isA(AddEntityPacket.class));
        ordered.verify(session).sendUpstreamPacket(isA(MobEquipmentPacket.class));
    }

    @Test
    void batchesBothRotationsAndScaleIntoOneRevisionWithoutMergingThem() {
        var entity = entity();
        entity.spawnEntity();
        clearInvocations(session);
        EntityMetadata<Vector3f, ?> scale = mock(EntityMetadata.class);
        when(scale.getValue()).thenReturn(Vector3f.from(-2, 0, 3));
        entity.setScale(scale);
        EntityMetadata<Quaternionf, ?> left = mock(EntityMetadata.class);
        when(left.getValue()).thenReturn(Quaternionf.from(1, 0, 0, 1));
        entity.setLeftRotation(left);
        EntityMetadata<Quaternionf, ?> right = mock(EntityMetadata.class);
        when(right.getValue()).thenReturn(Quaternionf.from(0, 1, 0, 1));
        entity.setRightRotation(right);
        entity.updateBedrockMetadata();
        var packets = ArgumentCaptor.forClass(SetEntityDataPacket.class);
        verify(session).sendUpstreamPacket(packets.capture());
        var values = packets.getValue().getProperties();
        assertEquals(1, values.getIntProperties().stream().filter(p -> p.getIndex() == names.indexOf("geyser:revision")).findFirst().orElseThrow().getValue());
        assertEquals(-2, value(values.getFloatProperties(), "sx"));
        assertEquals(0, value(values.getFloatProperties(), "sy"));
        assertEquals(1, value(values.getFloatProperties(), "lx"));
        assertEquals(-1, value(values.getFloatProperties(), "ry"));
        clearInvocations(session);
        entity.updateBedrockMetadata();
        verify(session, never()).sendUpstreamPacket(any());
    }

    @Test
    void rejectsNonFiniteVectorsAndOutOfRangeProfileIds() {
        var entity = entity();
        entity.spawnEntity();
        clearInvocations(session);
        EntityMetadata<Vector3f, ?> scale = mock(EntityMetadata.class);
        when(scale.getValue()).thenReturn(Vector3f.from(Float.NaN, 2, 3));
        entity.setScale(scale);
        entity.updateBedrockMetadata();
        verify(session, never()).sendUpstreamPacket(any());
        assertThrows(IllegalArgumentException.class, () -> entity.setRenderProfile(-1));
        assertThrows(IllegalArgumentException.class, () -> entity.setRenderProfile(1000001));
    }

    @Test
    void explicitCalibrationChangesReachTheClientWithoutAnotherMetadataUpdate() {
        var entity = entity();
        entity.spawnEntity();
        clearInvocations(session);
        entity.setRenderProfile(7);
        var packet = ArgumentCaptor.forClass(SetEntityDataPacket.class);
        verify(session).sendUpstreamPacket(packet.capture());
        var properties = packet.getValue().getProperties().getIntProperties();
        assertEquals(7, properties.stream().filter(p -> p.getIndex() == names.indexOf("geyser:render_profile")).findFirst().orElseThrow().getValue());
    }

    @Test
    void blockEquipmentRetainsTheExactMappedStateAndClearsBlocksWithoutItems() {
        GeyserMockContext.mockContext(() -> {
            var entity = entity(BlockDisplayEntity::new);
            entity.spawnEntity();
            clearInvocations(session);
            var item = mock(Item.class);
            var block = mock(Block.class);
            when(block.asItem()).thenReturn(item);
            var state = mock(BlockState.class);
            when(state.block()).thenReturn(block);
            var mapping = mock(ItemMapping.class);
            when(mapping.getBedrockDefinition()).thenReturn(new SimpleItemDefinition("minecraft:stone", 1, false));
            var bedrockState = mock(GeyserBedrockBlock.class);
            when(session.getItemMappings().getMapping(item)).thenReturn(mapping);
            when(session.getBlockMappings().getBedrockBlock(state)).thenReturn(bedrockState);
            var metadata = mock(IntEntityMetadata.class);
            when(metadata.getPrimitiveValue()).thenReturn(123);
            try (var states = mockStatic(BlockState.class)) {
                states.when(() -> BlockState.of(123)).thenReturn(state);
                entity.setDisplayedBlockState(metadata);
                var packet = ArgumentCaptor.forClass(MobEquipmentPacket.class);
                verify(session).sendUpstreamPacket(packet.capture());
                assertSame(bedrockState, packet.getValue().getItem().getBlockDefinition());
                assertEquals(1, packet.getValue().getItem().getCount());
                clearInvocations(session);
                when(block.asItem()).thenReturn(Items.AIR);
                entity.setDisplayedBlockState(metadata);
                verify(session).sendUpstreamPacket(packet.capture());
                assertEquals(ItemData.AIR, packet.getValue().getItem());
            }
        });
    }

    private float value(List<FloatEntityProperty> properties, String name) {
        return properties.stream().filter(p -> p.getIndex() == names.indexOf("geyser:" + name)).findFirst().orElseThrow().getValue();
    }
}
