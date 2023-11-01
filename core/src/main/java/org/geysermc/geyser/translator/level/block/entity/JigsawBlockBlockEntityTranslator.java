/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.level.block.entity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.session.GeyserSession;

@BlockEntity(type = "JigsawBlock")
public class JigsawBlockBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {
    @Override
    public void translateTag(GeyserSession session, NbtMapBuilder builder, CompoundTag tag, int blockState) {
        if (tag == null) {
            return;
        }
        Tag jointTag = tag.get("joint");
        if (jointTag instanceof StringTag) {
            builder.put("joint", ((StringTag) jointTag).getValue());
        } else {
            // Tag is not present in at least 1.14.4 Paper
            // Minecraft 1.18.1 deliberately has a fallback here, but not for any other value
            builder.put("joint", BlockStateValues.getHorizontalFacingJigsaws().contains(blockState) ? "aligned" : "rollable");
        }
        builder.put("name", getOrDefault(tag.get("name"), ""));
        builder.put("target_pool", getOrDefault(tag.get("pool"), ""));
        builder.put("final_state", ((StringTag) tag.get("final_state")).getValue());
        builder.put("target", getOrDefault(tag.get("target"), ""));
    }
}
