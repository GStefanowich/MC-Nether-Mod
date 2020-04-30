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
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.inventory.BasicInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Mixin(PiglinEntity.class)
public abstract class PiglinEmotions extends HostileEntity implements CrossbowUser, EmotionalPiglins {
    
    @Shadow private BasicInventory inventory;
    
    private boolean shareGold = false;
    
    protected PiglinEmotions(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/inventory/BasicInventory.addStack(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"), method = "addItem")
    protected ItemStack onAddToInventory(BasicInventory inventory, ItemStack stack) {
        if ((!stack.isEmpty())) {
            if (stack.getItem() == Items.GOLD_BLOCK) {
                int blocks = stack.getCount();
                return this.addGoldToInventory(new ItemStack(Items.GOLD_INGOT, blocks * 9), false);
            } else if (stack.getItem() == Items.GOLD_INGOT)
                return this.addGoldToInventory(stack, true);
        }
        
        return inventory.addStack( stack );
    }
    
    private ItemStack addGoldToInventory(ItemStack stack, boolean traded) {
        if (traded) {
            EnchantmentHelper.set(new HashMap<Enchantment, Integer>() {{
                put(Enchantments.VANISHING_CURSE, 1);
            }}, stack);
        }
        return this.inventory.addStack(stack);
    }
    
    @Inject(at = @At("RETURN"), method = "addItem")
    protected void onAddItem(ItemStack stack, CallbackInfoReturnable<ItemStack> callback) {
        this.craftingCheck();
    }
    
    /**
     * Show a status particle effect around piglins
     * @param particle The particle effect to show around the piglin
     */
    @Override
    public void produceParticles(ParticleEffect particle) {
        // Loop 5 times
        for( int i = 0; i < 5; ++i ) {
            double d = this.random.nextGaussian() * 0.02D;
            double e = this.random.nextGaussian() * 0.02D;
            double f = this.random.nextGaussian() * 0.02D;
            
            // Spawn the particles (Either as the Server or Client)
            if (this.world instanceof ServerWorld) ((ServerWorld)this.world).spawnParticles(particle, this.getParticleX(1.0D), this.getRandomBodyY() + 1.0D, this.getParticleZ(1.0D), 1, d, e, f, 0);
            else this.world.addParticle(particle, this.getParticleX(1.0D), this.getRandomBodyY() + 1.0D, this.getParticleZ(1.0D), d, e, f);
        }
    }
    
    @Override
    public void incrementGold() {
        this.addGoldToInventory(new ItemStack(Items.GOLD_INGOT, 1), true);
        this.craftingCheck();
    }
    
    /**
     * Overwrite of the Vanilla DropEquipment to remove cursed items from the inventory
     * @reason Items in a Piglins inventory that have Curse of Vanishing are not destroyed
     * @author TheElm
     */
    @Overwrite
    public void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropEquipment(source, lootingMultiplier, allowDrops);
        this.inventory.clearToList().stream()
            .filter(stack -> !EnchantmentHelper.hasVanishingCurse(stack)) // Filter out cursed items
            .forEach(this::dropStack); // Drop all inventory items
    }
    
    private void craftingCheck() {
        if (!this.inventory.isEmpty()) {
            // Count the number of armors we're wearing
            int armors = 0;
            
            // Check every equipment to attempt crafting more
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack armorStack = this.getEquippedStack(slot);
                
                // Skip to the next armor slot
                if (!armorStack.isEmpty()) {
                    // If the slot is occupied by armor
                    if (slot.getType() == EquipmentSlot.Type.ARMOR) armors++;
                    continue;
                }
                
                ItemStack createStack = this.craftArmorFromInventory(slot);
                if (!createStack.isEmpty())
                    this.equipStack(slot, createStack);
            }
            
            // Attempt to give gold to a different Piglin
            this.shareGold = (armors >= 4);
        }
    }
    private ItemStack craftArmorFromInventory(EquipmentSlot slot) {
        // Find the recipe for the equipment slot
        CraftingRecipe recipe = getArmorRecipe(this.world, getGoldArmorRecipeId(slot));
        if (recipe == null) // Check that a recipe was found
            return ItemStack.EMPTY;
        
        // Check for all of the ingredients
        boolean hasAllIngredients = true;
        
        List<Ingredient> ingredients = recipe.getPreviewInputs();
        List<ItemStack> craftingTable = new ArrayList<>();
        
        // Remove the ingredients
        for (Ingredient ingredient : ingredients) {
            boolean hasIngredient = false;
            
            // Remove
            for ( int iSlot = 0; iSlot < this.inventory.size(); iSlot++ ) {
                ItemStack iStack = this.inventory.getStack( iSlot );
                if ( !ingredient.test( iStack ) ) // If not match, skip slot
                    continue;
                
                // Decrement and stop searching the inventory
                if (hasIngredient = ( ingredient.isEmpty() || ( iStack.getCount() >= 1 && craftingTable.add(iStack.split(1)) )))
                    break;
            }
            
            if (!hasIngredient) {
                hasAllIngredients = false;
                break;
            }
        }
        
        // If all of the ingredients are present
        if (!hasAllIngredients)
            craftingTable.forEach(this.inventory::addStack);
        else {
            // "Craft" the item
            LocalDifficulty difficulty = this.world.getLocalDifficulty(this.getBlockPos());
            float f = difficulty.getClampedLocalDifficulty();
            
            // Clone the recipe output (Or you modify the recipe!)
            ItemStack output = recipe.getOutput().copy();
            
            // Attempt to enchant the item
            if (this.random.nextFloat() < 0.5F * f)
                return EnchantmentHelper.enchant(random, output, (int) (5.0F + f * (float) this.random.nextInt(18)), false);
            return output;
        }
        
        return ItemStack.EMPTY;
    }
    
    private static CraftingRecipe getArmorRecipe(World world, Identifier identifier) {
        if (identifier == null) return null;
        return world.getRecipeManager().values().stream().map(recipe -> {
            if (recipe instanceof CraftingRecipe)
                return (CraftingRecipe) recipe;
            return null;
        }).filter(Objects::nonNull).filter(recipe -> recipe.getId().equals(identifier)).findFirst().orElse(null);
    }
    private static Identifier getGoldArmorRecipeId(EquipmentSlot slot) {
        switch (slot) {
            case FEET:
                return new Identifier("golden_boots");
            case LEGS:
                return new Identifier("golden_leggings");
            case CHEST:
                return new Identifier("golden_chestplate");
            case HEAD:
                return new Identifier("golden_helmet");
            case MAINHAND:
                return new Identifier("golden_sword");
            case OFFHAND:
                return new Identifier("crossbow");
            default:
                return null;
        }
    }
}
