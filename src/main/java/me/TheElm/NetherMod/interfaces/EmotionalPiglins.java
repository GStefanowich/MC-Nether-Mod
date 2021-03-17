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

package me.TheElm.NetherMod.interfaces;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import org.jetbrains.annotations.NotNull;

public interface EmotionalPiglins {
    
    /**
     * Produce Particles at the Piglin
     * @param particle The particle type to spawn at the Piglin
     */
    void produceParticles(@NotNull ParticleEffect particle);
    
    /**
     * Called when giving an itemstack to a piglin
     * @param stack An ItemStack that was traded to the Piglin
     */
    ItemStack incrementGold(@NotNull ItemStack stack);
    
    /**
     * Called when giving stacks of items to a piglin
     * @param stacks Multiple ItemStacks that were traded to the Piglin
     */
    default void incrementGold(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks)
            this.incrementGold(stack);
    }
    
    /**
     * Get the piglins inventory
     * @return
     */
    SimpleInventory getInventory();
    
    /**
     * If the piglin has a full set of gold armor
     * @return
     */
    boolean hasFullArmorSet();
    void craftingCheck();
    
    boolean canCraftItem(@NotNull Item item);
    @NotNull ItemStack tryCraftItem(@NotNull Item item);
    
}
