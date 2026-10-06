package org.geysermc.geyser.entity.type.display;

import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisplayRotationTest {
    @Test
    void reflectsTheCoordinateFrameWithoutDiscardingQuaternionMagnitude() {
        var result = DisplayRotation.toBedrockQuaternion(Quaternionf.from(2, 3, 4, 5));
        assertEquals(Vector4f.from(2, -3, -4, 5), result);
        assertEquals(54, result.lengthSquared(), 1e-6);
    }

    @Test
    void zeroQuaternionsPreserveCollapseAndInvalidValuesHaveAnIdentityFallback() {
        var identity = Vector4f.from(0, 0, 0, 1);
        assertEquals(0, DisplayRotation.toBedrockQuaternion(Quaternionf.from(0, 0, 0, 0)).lengthSquared(), 1e-6);
        assertEquals(identity, DisplayRotation.toBedrockQuaternion(Quaternionf.from(Float.NaN, 0, 0, 1)));
        assertEquals(identity, DisplayRotation.toBedrockQuaternion(Quaternionf.from(0, Float.POSITIVE_INFINITY, 0, 1)));
        assertEquals(identity, DisplayRotation.toBedrockQuaternion(Quaternionf.from(1000001, 0, 0, 1)));
    }

    @Test
    void antipodalInputsKeepEquivalentRotations() {
        var positive = DisplayRotation.toBedrockQuaternion(Quaternionf.from(1, -2, 3, -4));
        var negative = DisplayRotation.toBedrockQuaternion(Quaternionf.from(-1, 2, -3, 4));
        assertEquals(positive.negate(), negative);
    }
}
