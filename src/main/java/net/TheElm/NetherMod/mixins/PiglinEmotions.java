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

import net.TheElm.NetherMod.interfaces.EmotionalPiglins;
import net.TheElm.NetherMod.utils.EntityUtils;
import net.TheElm.NetherMod.utils.ItemUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Mixin(PiglinEntity.class)
public abstract class PiglinEmotions extends HostileEntity implements CrossbowUser, EmotionalPiglins {
    
    private static @NotNull ItemStack STICK_ITEM = new ItemStack(Items.STICK);
    @Shadow private SimpleInventory inventory;
    
    private boolean shareGold = false;
    
    protected PiglinEmotions(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }
    
    @Inject(at = @At("RETURN"), method = "addItem")
    protected void onAddItem(ItemStack stack, CallbackInfoReturnable<ItemStack> callback) {
        this.craftingCheck();
    }
    
    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void onReadFromTag(@NotNull NbtCompound tag, @NotNull CallbackInfo callback) {
        this.shareGold = EntityUtils.hasFullArmorSet(this);
    }
    
    @Override
    public SimpleInventory getInventory() {
        return this.inventory;
    }
    
    @Override
    public boolean hasFullArmorSet() {
        return this.shareGold;
    }
    
    /**
     * Show a status particle effect around piglins
     * @param particle The particle effect to show around the piglin
     */
    @Override
    public void produceParticles(@NotNull ParticleEffect particle) {
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
    public ItemStack incrementGold(@NotNull ItemStack stack) {
        ItemStack remainder = PiglinEmotions.addTradedItemToInventory(this.inventory, stack.copy(), true);
        this.craftingCheck();
        return remainder;
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/inventory/SimpleInventory.addStack(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"), method = "addItem")
    protected ItemStack onAddToInventory(@NotNull SimpleInventory inventory, @NotNull ItemStack stack) {
        // If the stack picked up is NOT EMPTY
        if (!stack.isEmpty()) {
            // If the item is the BARTER ITEM, add it to the inventory for later
            if (stack.getItem() == PiglinBrain.BARTERING_ITEM)
                return PiglinEmotions.addTradedItemToInventory(inventory, stack, true);
            
            // If the item can be MADE into the BARTER ITEM, Attempt crafting it
            Optional<CraftingRecipe> recipes = PiglinEmotions.getMakesTradingItem(this.world, stack);
            if ( recipes.isPresent() ) {
                CraftingRecipe recipe = recipes.get();
                
                // Get the output of the crafting
                ItemStack makes = recipe.getOutput();
                PiglinEmotions.addVanishingCurse(makes);
                
                // Try to add the crafted items into the inventory
                while ( !stack.isEmpty() && inventory.canInsert(makes) ) {
                    // Decrement the item used to make the recipe
                    stack.decrement(1);
                    ItemStack remainder = inventory.addStack(makes);
                    
                    // Drop any leftover crafted items
                    if ( !remainder.isEmpty() ) // Drop the items WITHOUT the VANISHING curse
                        this.dropStack(new ItemStack(remainder.getItem(), remainder.getCount()));
                }
            }
        }
        
        // Fallback to adding the item to the inventory
        return inventory.addStack(stack);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/inventory/SimpleInventory.clearToList()Ljava/util/List;"), method = "dropEquipment")
    public List<ItemStack> onDropEquipment(@NotNull SimpleInventory inventory) {
        return ItemUtils.getDroppableItems(inventory.clearToList());
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/inventory/SimpleInventory.clearToList()Ljava/util/List;"), method = "zombify")
    public List<ItemStack> onZombify(@NotNull SimpleInventory inventory) {
        return ItemUtils.getDroppableItems(inventory.clearToList());
    }
    
    public void craftingCheck() {
        if (!this.inventory.isEmpty()) {
            // Count the number of armors we're wearing
            int armors = 0;
            
            // Check every equipment to attempt crafting more
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack armorStack = this.getEquippedStack(slot);
                
                // Skip to the next armor slot
                if (!armorStack.isEmpty()) {
                    // If the slot is occupied by armor
                    if (slot.getType() == EquipmentSlot.Type.ARMOR)
                        armors++;
                    continue;
                }
                
                ItemStack createStack = this.tryCraftItem(slot);
                if (!createStack.isEmpty()) {
                    this.world.playSoundFromEntity(null, this, SoundEvents.ITEM_ARMOR_EQUIP_GOLD, SoundCategory.AMBIENT, 1.0F, 1.0F);
                    this.equipStack(slot, createStack);
                }
            }
            
            // Attempt to give gold to a different Piglin
            this.shareGold = (armors >= 4);
        }
    }
    
    @Override
    public boolean canCraftItem(@NotNull Item item) {
        return !this.tryCraftItem(PiglinEmotions.getItemRecipeId(item), false)
            .isEmpty();
    }
    
    @Override
    public @NotNull ItemStack tryCraftItem(@NotNull Item item) {
        return this.tryCraftItem(PiglinEmotions.getItemRecipeId(item), true);
    }
    private @NotNull ItemStack tryCraftItem(@NotNull EquipmentSlot slot) {
        return this.tryCraftItem(PiglinEmotions.getGoldArmorRecipeId(slot), true);
    }
    private @NotNull ItemStack tryCraftItem(@NotNull Optional<Identifier> identifier, final boolean doCraft) {
        // Find the recipe for the equipment slot
        Optional<CraftingRecipe> recipes = PiglinEmotions.getArmorRecipe(this.world, identifier);
        if (!recipes.isPresent()) // Check that a recipe was found
            return ItemStack.EMPTY;
        CraftingRecipe recipe = recipes.get();
        
        // Check for all of the ingredients
        boolean hasAllIngredients = true;
        
        List<Ingredient> ingredients = recipe.getIngredients();
        List<ItemStack> craftingTable = new ArrayList<>();
        
        // Remove the ingredients
        for (Ingredient ingredient : ingredients) {
            boolean hasIngredient = false;
            
            // Remove
            for ( int iSlot = 0; iSlot < this.inventory.size(); iSlot++ ) {
                ItemStack iStack = this.inventory.getStack(iSlot);
                if ( !ingredient.test(iStack) ) // If not match, skip slot
                    continue;
                
                // Decrement and stop searching the inventory
                if (hasIngredient = ( ingredient.isEmpty() || ( iStack.getCount() >= 1 && craftingTable.add(iStack.split(1)) )))
                    break;
            }
            
            if (!hasIngredient) {
                if ( ingredient.test(PiglinEmotions.STICK_ITEM) )
                    continue;
            
                hasAllIngredients = false;
                break;
            }
        }
        
        // If all of the ingredients are present
        if (!hasAllIngredients || !doCraft)
            craftingTable.forEach(this.inventory::addStack);
        if ( hasAllIngredients ) {
            // Clone the recipe output (Or you modify the recipe!)
            ItemStack output = recipe.getOutput()
                .copy();
            
            if ( doCraft ) {
                // Get enchantment possibilities based off of difficulty
                LocalDifficulty difficulty = this.world.getLocalDifficulty(this.getBlockPos());
                float f = difficulty.getClampedLocalDifficulty();
                
                // Attempt to enchant the item
                if (this.random.nextFloat() < 0.5F * f)
                    return EnchantmentHelper.enchant(random, output, (int) (5.0F + f * (float) this.random.nextInt(18)), false);
                if ( !this.world.isClient )
                    this.world.playSoundFromEntity(null, this, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.AMBIENT, 1.0F, 1.0F);
            }
            
            return output;
        }
        
        return ItemStack.EMPTY;
    }
    
    private static @NotNull ItemStack addTradedItemToInventory( @NotNull SimpleInventory inventory, @NotNull ItemStack stack, boolean traded) {
        if (traded && !EnchantmentHelper.hasVanishingCurse(stack))
            PiglinEmotions.addVanishingCurse(stack);
        return inventory.addStack(stack);
    }
    private static @NotNull Optional<CraftingRecipe> getArmorRecipe(@NotNull World world, @NotNull Optional<Identifier> identifier) {
        return identifier.flatMap((value) -> PiglinEmotions.getWorldCraftingRecipes(world)
            .filter(recipe -> recipe.getId().equals(value))
            .findFirst());
    }
    private static @NotNull Optional<CraftingRecipe> getMakesTradingItem( @NotNull World world, @NotNull ItemStack stack) {
        return PiglinEmotions.getWorldCraftingRecipes(world)
            .filter((recipe) -> {
                Collection<Ingredient> ingredients = recipe.getIngredients();
                return ingredients.size() == 1 && recipe.getOutput().getItem() == PiglinBrain.BARTERING_ITEM && ingredients.stream()
                    .anyMatch(ingredient -> ingredient.test(stack));
            })
            .findFirst();
    }
    private static @NotNull Optional<Identifier> getGoldArmorRecipeId(@NotNull EquipmentSlot slot) {
        return ItemUtils.getOptionalPiglinEquipmentSlotItem(slot)
            .flatMap(PiglinEmotions::getItemRecipeId);
    }
    private static @NotNull Optional<Identifier> getItemRecipeId(@NotNull Item slot) {
        return Optional.of(Registry.ITEM.getId(slot));
    }
    private static @NotNull Stream<CraftingRecipe> getWorldCraftingRecipes(@NotNull World world) {
        return world.getRecipeManager()
            .values()
            .stream()
            .map((recipe) -> recipe instanceof CraftingRecipe ? (CraftingRecipe) recipe : null)
            .filter(Objects::nonNull);
    }
    private static void addVanishingCurse(@NotNull ItemStack stack) {
        EnchantmentHelper.set(new HashMap<Enchantment, Integer>() {{
            put(Enchantments.VANISHING_CURSE, 1);
        }}, stack);
    }
}
