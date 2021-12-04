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

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created on Mar 16 2021 at 8:01 PM.
 * By greg in PiglinMod
 */
public final class ItemUtils {
    private ItemUtils() {}
    
    public static @NotNull List<ItemStack> getDroppableItems(@NotNull Collection<ItemStack> inventory) {
        return inventory.stream()
            .filter(stack -> !EnchantmentHelper.hasVanishingCurse(stack))
            .collect(Collectors.toList());
    }
    public static @Nullable Item getPiglinGiftItem(@NotNull AbstractPiglinEntity piglin) {
        if (piglin instanceof PiglinEntity)
            return EntityUtils.hasFullArmorSet(piglin) ? null : Items.GOLD_INGOT;
        EquipmentSlot slot = EntityUtils.getNeededArmorSlot(piglin);
        if ( slot == null )
            return null;
        return ItemUtils.getPiglinEquipmentSlotItem(slot);
    }
    public static @NotNull Optional<Item> getOptionalPiglinEquipmentSlotItem(@NotNull EquipmentSlot slot) {
        return Optional.ofNullable(ItemUtils.getPiglinEquipmentSlotItem(slot));
    }
    public static @Nullable Item getPiglinEquipmentSlotItem(@NotNull EquipmentSlot slot) {
        switch (slot) {
            case HEAD: return Items.GOLDEN_HELMET;
            case CHEST: return Items.GOLDEN_CHESTPLATE;
            case LEGS: return Items.GOLDEN_LEGGINGS;
            case FEET: return Items.GOLDEN_BOOTS;
            case MAINHAND: return Items.GOLDEN_SWORD;
            case OFFHAND: return Items.CROSSBOW;
        }
        return null;
    }
    
}
