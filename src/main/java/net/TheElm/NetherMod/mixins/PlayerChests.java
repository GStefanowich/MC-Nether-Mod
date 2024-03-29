/*
 * This software is licensed under the MIT License
 * https://github.com/GStefanowich/MC-Nether-Mod
 *
 * Copyright (c) 2019 Gregory Stefanowich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.TheElm.NetherMod.mixins;

import net.TheElm.NetherMod.interfaces.PlayerChest;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlockEntity.class)
public abstract class PlayerChests extends LootableContainerBlockEntity implements PlayerChest {
    
    private boolean belongsToPlayer = false;
    
    protected PlayerChests(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }
    
    @Inject(at = @At("TAIL"), method = "readNbt")
    public void onFromTag(NbtCompound tag, CallbackInfo callback) {
        // If the chest was placed by a player, read that value
        this.belongsToPlayer = tag.contains("playerPlaced", 1) && tag.getBoolean("playerPlaced");
    }
    
    @Inject(at = @At("TAIL"), method = "writeNbt")
    public void onToTag(NbtCompound tag, CallbackInfo callback) {
        // If the chest was placed by a player, save that information
        if (this.belongsToPlayer)
            tag.putBoolean("playerPlaced", this.belongsToPlayer);
    }
    
    @Override
    public void setPlayers(boolean players) {
        this.belongsToPlayer = players;
    }
    
    @Override
    public boolean isPlayers() {
        return this.belongsToPlayer;
    }
}
