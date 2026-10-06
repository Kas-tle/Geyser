# Display entities: draft implementation

This branch refreshes Kastle's old display-entities branch against current Geyser and ports item and block displays to the current entity registry. Use the generated display-only artifact from the matching [pack branch](https://github.com/AlexProgrammerDE/GeyserOptionalPack/tree/feature/display-entities). Native correction profiles target Bedrock **1.26.51.1**.

Codex generated this implementation and its tests. It has automated validation but **has not been tested in-game by a human**. Keep the PR in draft until those tests are complete.

## Implemented

- Current entity constructors, metadata layout, item translation and typed property registration.
- Block-state equipment that retains the mapped Bedrock block definition. Air and blocks without an inventory item clear equipment.
- Initial equipment after spawn and subsequent equipment updates.
- Independent left and right quaternions around non-uniform scale. Zero and negative display scales remain valid.
- Quaternion magnitude preservation, a zero-quaternion collapse, an identity fallback for invalid or out-of-range quaternions, rejection of non-finite vectors and bounded numeric properties.
- One revision for each transform update batch. The pack handles delayed, zero-duration and interrupted interpolation.
- Item-display context forwarding and an explicit correction-profile property for custom calibration.

The upstream text-display implementation remains in place.

Build the companion pack with `python3 tools/display/generate.py --pack GeyserDisplayEntities.mcpack` in its checkout. Put that file in Geyser's `packs/` directory. Its separate UUID preserves modern Geyser's integrated resources. The legacy OptionalPack artifact disables those resources.

## Build and automated validation

Use Java 25 and initialize the upstream submodules:

```sh
git submodule update --init
./gradlew :core:test :standalone:shadowJar
```

The standalone artifact is `bootstrap/standalone/build/libs/Geyser-Standalone.jar`.

Run the optional pack's generator and script tests as described in its `tools/display/README.md`. Its fixture tests cover native correction matrices, shear, reflections, singular source scales and zero target scale. The script tests execute the generated Molang in MoJava. These checks do not establish in-game rendering parity.

## Human test procedure

Use an isolated Java test server with this Geyser build and the matching pack. Connect with Bedrock 1.26.51.1. Enable the Bedrock content log and inspect it after each test.

1. Summon an item display with an ordinary sprite, then use a full block and each special profile. Check orientation against the pack's documented reference frame.
2. Check an item supplied before spawn, a replacement after spawn, air, an invalid block state, and a block without an inventory item. Replacements must not leave stale equipment or correction factors.
3. Apply independent rotations with non-uniform scale. Check zero scale, negative scale, non-unit and zero quaternions, and rotations near gimbal lock. A zero quaternion must collapse the display; a non-unit quaternion must retain its squared-length scale.
4. Start a 20-tick interpolation, interrupt it halfway through, and check the next transition. Also test a positive delay and zero duration.
5. Cross the positive/negative 180-degree boundary. The display must take the shortest rotation path. Antipodal quaternion representations must keep the same pose.
6. Use a custom attachable. Automatic correction must skip it. Supply authored renderer factors and an explicit profile to test custom correction.
7. Compare multiple resource-pack combinations. Record the profile ID, active packs, client version and content-log errors.

Example commands for a current Java server:

```mcfunction
summon minecraft:item_display ~ ~1 ~ {item:{id:"minecraft:apple",count:1},item_display:"none"}
summon minecraft:block_display ~2 ~1 ~ {block_state:{Name:"minecraft:stone"}}
data merge entity @e[type=minecraft:item_display,sort=nearest,limit=1] {start_interpolation:0,interpolation_duration:20,transformation:{translation:[1f,0f,0f],scale:[2f,0.5f,1f],left_rotation:[0.70710677f,0f,0f,0.70710677f],right_rotation:[0f,0.34202015f,0f,0.9396926f]}}
```

The Geyser implementation sends Java translation as `(-x, -y, z)` animation coordinates. The pack converts animation positions into the native matrix frame and uses separate quaternion rotations. Check this coordinate mapping in-game before treating the draft as complete.

## Remaining work

This is a renderer-calibration draft, not complete Java display parity. The following metadata remains unsupported: position/rotation interpolation, billboard modes, brightness override, view range, shadows, display bounds and glow color. Item-display context values reach the pack, but complete vanilla Java context transforms require versioned Java model data.

The native profile set does not cover every item. Hand-equipped items, maps, dedicated renderers, arbitrary custom attachables and unlisted items need further classification or authored transforms. The pack records those gaps and retains native rendering when it has no correction.

Both drafts must remain open for that work and human validation. Do not label them ready to merge based only on automated tests.
