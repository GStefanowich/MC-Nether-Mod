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

package me.TheElm.NetherMod.goals;

import com.google.common.collect.ImmutableMap;
import me.TheElm.NetherMod.interfaces.EmotionalPiglins;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Created on Mar 11 2021 at 12:14 AM.
 * By greg in PiglinMod
 */
public final class PiglinTradingGoal extends Task<PiglinEntity> {
    
    public PiglinTradingGoal() {
        this(60);
    }
    
    public PiglinTradingGoal( int runTime ) {
        this(runTime, runTime);
    }
    
    public PiglinTradingGoal( int minRunTime, int maxRunTime ) {
        super(ImmutableMap.of(MemoryModuleType.NEARBY_ADULT_PIGLINS, MemoryModuleState.VALUE_PRESENT, MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_PRESENT), minRunTime, maxRunTime);
    }
    
    @Override
    protected boolean shouldRun( ServerWorld world, PiglinEntity piglin ) {
        if ( !((EmotionalPiglins)piglin).hasFullArmorSet() )
            return false;
        SimpleInventory inventory = ((EmotionalPiglins)piglin).getInventory();
        if ( inventory.count(PiglinBrain.BARTERING_ITEM) <= 0 )
            return false;
        LivingEntity entity;
        Brain<PiglinEntity> brain = piglin.getBrain();
        Optional<LivingEntity> optional = brain.getOptionalMemory(MemoryModuleType.INTERACTION_TARGET);
        return optional.isPresent() && ((entity = optional.get()) instanceof PiglinEntity) && !((EmotionalPiglins)entity).hasFullArmorSet();
    }
    
    @Override
    protected void run( ServerWorld world, PiglinEntity piglin, long time ) {
        Brain<PiglinEntity> brain = piglin.getBrain();
        brain.getOptionalMemory(MemoryModuleType.INTERACTION_TARGET).ifPresent((target) -> {
            SimpleInventory inventory = ((EmotionalPiglins)piglin).getInventory();
            if (!( target instanceof PiglinEntity ))
                return;
            boolean doCheck = false;
            
            for ( int i = 0; i < inventory.size(); i++ ) {
                ItemStack stack = inventory.getStack(i);
                if (stack.isEmpty() || !PiglinBrain.BARTERING_ITEM.equals(stack.getItem()) )
                    continue;
                
                // Directly give the piglin gold telepathically
                ItemStack remainder = ((EmotionalPiglins)target)
                    .incrementGold(stack);
                inventory.setStack(i, remainder);
                
                doCheck = true;
            }
            
            if ( doCheck )
                ((EmotionalPiglins) target).craftingCheck();
        });
    }
}
