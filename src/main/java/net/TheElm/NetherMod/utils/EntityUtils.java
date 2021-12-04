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

package net.TheElm.NetherMod.utils;

import net.TheElm.NetherMod.interfaces.EmotionalPiglins;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Created on Mar 16 2021 at 7:58 PM.
 * By greg in PiglinMod
 */
public final class EntityUtils {
    
    public static boolean hasFullArmorSet(@NotNull LivingEntity entity) {
        return EntityUtils.getNeededArmorSlot(entity) == null;
    }
    public static boolean missingArmorSet(@NotNull LivingEntity entity) {
        return !EntityUtils.hasFullArmorSet(entity);
    }
    public static @Nullable EquipmentSlot getNeededArmorSlot(@NotNull LivingEntity entity) {
        for ( EquipmentSlot slot : EquipmentSlot.values() )
            if ( slot.getType() == EquipmentSlot.Type.ARMOR && entity.getEquippedStack(slot).isEmpty() )
                return slot;
        return null;
    }
    public static void transferDesiredItems(@NotNull Collection<AbstractPiglinEntity> piglins, @NotNull PiglinEntity main) {
        SimpleInventory inventory = ((EmotionalPiglins)main).getInventory();
        for ( AbstractPiglinEntity piglin : piglins ) {
            while ( true ) {
                ItemStack transfer = null;
                Item wanted = ItemUtils.getPiglinGiftItem(piglin);
                if ( wanted != null ) {
                    if ( inventory.count(wanted) > 0 )
                        transfer = inventory.removeItem(wanted, wanted.getMaxCount());
                    else if (((EmotionalPiglins)main).canCraftItem(wanted))
                        transfer = ((EmotionalPiglins)main).tryCraftItem(wanted);
                }
                
                if ( transfer == null )
                    break;
                
                // Try to transfer the item to the other entity
                ItemStack remainder = EntityUtils.transferDesiredItem(piglin, transfer);
                if ( !remainder.isEmpty() ) // Try to take the item bback
                    remainder = inventory.addStack(remainder);
                if ( !remainder.isEmpty() ) // Drop the item if all else fails
                    main.dropStack(remainder);
            }
        }
    }
    public static ItemStack transferDesiredItem(@NotNull AbstractPiglinEntity piglin, @NotNull ItemStack stack) {
        Item item = stack.getItem();
        if ( item instanceof ArmorItem && EntityUtils.equipCraftedStack(piglin, stack) )
            return ItemStack.EMPTY;
        if (piglin instanceof PiglinEntity)
            return ((EmotionalPiglins) piglin).incrementGold(stack);
        return ItemStack.EMPTY;
    }
    
    public static boolean equipCraftedStack(@NotNull MobEntity mob, @NotNull ItemStack equipment) {
        EquipmentSlot equipmentSlot = mob.getPreferredEquipmentSlot(equipment);
        ItemStack itemStack = mob.getEquippedStack(equipmentSlot);
        if (!itemStack.isEmpty() || !mob.canPickupItem(equipment))
            return false;
        mob.equipStack(equipmentSlot, equipment);
        return true;
    }
    
}
