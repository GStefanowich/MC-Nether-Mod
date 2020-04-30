/*
 * This software is licensed under the MIT License
 * https://github.com/GStefanowich/MC-Server-Protection
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

package me.TheElm.NetherMod.mixins;

import me.TheElm.NetherMod.interfaces.EmotionalPiglins;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PiglinBrain.class)
public class PiglinSigns {
    
    /**
     * A Mixin for when the piglin becomes angry, called by the Logic-Side
     * @param piglin The piglin that is angry
     * @param target The target that it is angry at
     * @param callback The Mixin callback
     */
    @Inject(at = @At("HEAD"), method = "angerAt")
    private static void onAngerAt(PiglinEntity piglin, LivingEntity target, CallbackInfo callback) {
        // Check that the Target is a Player (And NOT Null), and not already the same target
        if (target instanceof PlayerEntity && piglin.getTarget() != target)
            // Show the same particle effect that angry villagers do
            ((EmotionalPiglins)piglin).produceParticles(ParticleTypes.ANGRY_VILLAGER);
    }
    
    /**
     * A Mixin for when the piglin completes a trade
     * @param piglin The piglin that completed a trade
     * @param stack The itemstack that the piglin picked up
     * @param callback The Mixin callback
     */
    @Inject(at = @At("HEAD"), method = "doBarter")
    private static void onBarter(PiglinEntity piglin, ItemStack stack, CallbackInfo callback) {
        ((EmotionalPiglins)piglin).incrementGold();
    }
    
}
