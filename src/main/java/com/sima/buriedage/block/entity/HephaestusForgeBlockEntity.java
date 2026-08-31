package com.sima.buriedage.block.entity;

import java.util.ArrayList;
import java.util.List;

import com.sima.buriedage.block.HephaestusForgeBlock;
import com.sima.buriedage.item.AncientBlueprintItem;
import com.sima.buriedage.registry.ModBlockEntities;
import com.sima.buriedage.registry.ModEnchantments;
import com.sima.buriedage.registry.ModTags;
import com.sima.buriedage.registry.ModTriggers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class HephaestusForgeBlockEntity extends BlockEntity {
    public static final int SLOT_BLUEPRINT = 0;
    public static final int SLOT_CATALYST = 1;
    public static final int SLOT_TARGET = 2;
    public static final int HITS_TO_FORGE = 3;
    public static final int HIT_COOLDOWN = 10;

    public static final double GLYPH_COLUMN_X = 0.5;
    public static final double GLYPH_COLUMN_Z = 0.5;
    public static final double GLYPH_COLUMN_BASE_Y = 1.0;
    public static final int GLYPH_COLUMN_STEPS = 6;
    public static final double GLYPH_COLUMN_STEP_HEIGHT = 0.35;
    public static final int GLYPH_PARTICLES_PER_STEP = 18;

    private static final String UPGRADED_KEY = "buried_age_upgraded";

    private ItemStack blueprint = ItemStack.EMPTY;
    private ItemStack catalyst = ItemStack.EMPTY;
    private ItemStack target = ItemStack.EMPTY;
    private int hits;
    private long lastHitTick = -HIT_COOLDOWN;
    private final List<Integer> insertionOrder = new ArrayList<>();

    public HephaestusForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEPHAESTUS_FORGE.get(), pos, state);
    }

    public ItemStack getBlueprint() {
        return this.blueprint;
    }

    public ItemStack getTarget() {
        return this.target;
    }

    public ItemStack getCatalyst() {
        return this.catalyst;
    }

    public boolean isCharged() {
        return !this.blueprint.isEmpty() && !this.catalyst.isEmpty() && !this.target.isEmpty();
    }

    private ItemStack getSlot(int slot) {
        return switch (slot) {
            case SLOT_BLUEPRINT -> this.blueprint;
            case SLOT_CATALYST -> this.catalyst;
            default -> this.target;
        };
    }

    private void setSlot(int slot, ItemStack stack) {
        switch (slot) {
            case SLOT_BLUEPRINT -> this.blueprint = stack;
            case SLOT_CATALYST -> this.catalyst = stack;
            default -> this.target = stack;
        }
    }

    public boolean insert(int slot, ItemStack held) {
        if (!this.getSlot(slot).isEmpty()) {
            return false;
        }

        this.setSlot(slot, held.split(1));
        this.insertionOrder.remove(Integer.valueOf(slot));
        this.insertionOrder.add(slot);
        this.hits = 0;
        this.markUpdated();
        return true;
    }

    public boolean returnLastInserted(Player player) {
        for (int i = this.insertionOrder.size() - 1; i >= 0; i--) {
            int slot = this.insertionOrder.get(i);
            ItemStack stack = this.getSlot(slot);
            this.insertionOrder.remove(i);
            if (!stack.isEmpty()) {
                this.setSlot(slot, ItemStack.EMPTY);
                this.hits = 0;
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }

                this.markUpdated();
                return true;
            }
        }

        for (int slot : new int[] { SLOT_TARGET, SLOT_CATALYST, SLOT_BLUEPRINT }) {
            ItemStack stack = this.getSlot(slot);
            if (!stack.isEmpty()) {
                this.setSlot(slot, ItemStack.EMPTY);
                this.hits = 0;
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }

                this.markUpdated();
                return true;
            }
        }

        return false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            Block.popResource(this.level, pos, this.blueprint);
            Block.popResource(this.level, pos, this.catalyst);
            Block.popResource(this.level, pos, this.target);
            this.blueprint = ItemStack.EMPTY;
            this.catalyst = ItemStack.EMPTY;
            this.target = ItemStack.EMPTY;
            this.insertionOrder.clear();
        }
    }

    public boolean strike(ServerLevel level, BlockPos pos, @Nullable Player player) {
        long now = level.getGameTime();
        if (now - this.lastHitTick < HIT_COOLDOWN) {
            return false;
        }

        this.lastHitTick = now;

        if (!this.isCharged()) {
            playDull(level, pos);
            tell(player, Component.translatable("message.buried_age.forge.not_charged"));
            return true;
        }

        Verdict verdict = this.verdict();
        if (!verdict.allowed()) {
            this.hits = 0;
            playRejected(level, pos);
            tell(player, verdict.refusal());
            this.markUpdated();
            return true;
        }

        this.hits++;
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 0.9F + 0.1F * this.hits);
        level.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 12, 0.25, 0.1, 0.25, 0.05);
        level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.0);

        if (this.hits < HITS_TO_FORGE) {
            tell(player, Component.translatable("message.buried_age.forge.strike", this.hits, HITS_TO_FORGE));
            this.markUpdated();
            return true;
        }

        this.hits = 0;
        this.applyForge(verdict);
        tell(player, Component.translatable("message.buried_age.forge.success",
                Enchantment.getFullname(verdict.enchantment(), verdict.newLevel())));
        this.blueprint = ItemStack.EMPTY;
        this.catalyst = ItemStack.EMPTY;
        this.insertionOrder.remove(Integer.valueOf(SLOT_BLUEPRINT));
        this.insertionOrder.remove(Integer.valueOf(SLOT_CATALYST));
        playSuccess(level, pos);
        if (player instanceof ServerPlayer serverPlayer) {
            ModTriggers.FORGE_RITUAL.get().trigger(serverPlayer, this.target);
        }

        this.markUpdated();
        return true;
    }

    private record Verdict(@Nullable Holder<Enchantment> enchantment, int newLevel, @Nullable Component refusal) {
        static Verdict refuse(Component reason) {
            return new Verdict(null, 0, reason);
        }

        static Verdict allow(Holder<Enchantment> enchantment, int newLevel) {
            return new Verdict(enchantment, newLevel, null);
        }

        boolean allowed() {
            return this.refusal == null;
        }
    }

    private Verdict verdict() {
        Holder<Enchantment> enchantment = AncientBlueprintItem.getEnchantment(this.blueprint);
        if (enchantment == null) {
            return Verdict.refuse(Component.translatable("message.buried_age.forge.reject.blank_blueprint"));
        }

        int level = this.target
                .getOrDefault(EnchantmentHelper.getComponentType(this.target), ItemEnchantments.EMPTY)
                .getLevel(enchantment);

        if (enchantment.is(ModEnchantments.WRATH_OF_ZEUS)) {
            if (!this.target.is(ModTags.WRATH_OF_ZEUS_TARGETS)) {
                return Verdict.refuse(Component.translatable("message.buried_age.forge.reject.not_melee"));
            }

            if (level > 0) {
                return Verdict.refuse(Component.translatable("message.buried_age.forge.reject.already_present",
                        enchantment.value().description()));
            }

            return Verdict.allow(enchantment, 1);
        }

        if (wasUpgradedHere(this.target, enchantment)) {
            return Verdict.refuse(Component.translatable("message.buried_age.forge.reject.already_upgraded",
                    enchantment.value().description()));
        }

        int maxLevel = enchantment.value().getMaxLevel();
        if (level != maxLevel) {
            return Verdict.refuse(Component.translatable("message.buried_age.forge.reject.needs_max",
                    Enchantment.getFullname(enchantment, maxLevel), level));
        }

        return Verdict.allow(enchantment, maxLevel + 1);
    }

    private void applyForge(Verdict verdict) {
        Holder<Enchantment> enchantment = verdict.enchantment();
        DataComponentType<ItemEnchantments> componentType = EnchantmentHelper.getComponentType(this.target);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
                this.target.getOrDefault(componentType, ItemEnchantments.EMPTY));
        mutable.set(enchantment, verdict.newLevel());
        this.target.set(componentType, mutable.toImmutable());
        markUpgradedHere(this.target, enchantment);
    }

    private static void tell(@Nullable Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, true);
        }
    }

    private static boolean wasUpgradedHere(ItemStack stack, Holder<Enchantment> enchantment) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getCompoundOrEmpty(UPGRADED_KEY)
                .getBooleanOr(enchantment.getRegisteredName(), false);
    }

    private static void markUpgradedHere(ItemStack stack, Holder<Enchantment> enchantment) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag upgraded = tag.getCompoundOrEmpty(UPGRADED_KEY);
            upgraded.putBoolean(enchantment.getRegisteredName(), true);
            tag.put(UPGRADED_KEY, upgraded);
        });
    }

    private static void playDull(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4F, 0.5F);
    }

    private static void playRejected(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 0.6F);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.25, 0.2, 0.25, 0.02);
    }

    private static void playSuccess(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.4F);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.6F);
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + GLYPH_COLUMN_BASE_Y,
                pos.getZ() + 0.5, 24, 0.2, 0.3, 0.2, 0.06);
        for (int step = 0; step < GLYPH_COLUMN_STEPS; step++) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    pos.getX() + GLYPH_COLUMN_X,
                    pos.getY() + GLYPH_COLUMN_BASE_Y + step * GLYPH_COLUMN_STEP_HEIGHT,
                    pos.getZ() + GLYPH_COLUMN_Z,
                    GLYPH_PARTICLES_PER_STEP, 0.15, 0.1, 0.15, 0.03);
        }
    }

    private void markUpdated() {
        this.setChanged();
        if (this.level == null) {
            return;
        }

        BlockState current = this.getBlockState();
        if (current.hasProperty(HephaestusForgeBlock.HAS_BLUEPRINT)) {
            boolean hasBlueprint = !this.blueprint.isEmpty();
            if (current.getValue(HephaestusForgeBlock.HAS_BLUEPRINT) != hasBlueprint) {
                this.level.setBlock(this.getBlockPos(),
                        current.setValue(HephaestusForgeBlock.HAS_BLUEPRINT, hasBlueprint), Block.UPDATE_ALL);
            }
        }

        this.level.sendBlockUpdated(this.getBlockPos(), current, this.getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.blueprint = input.read("blueprint", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.catalyst = input.read("catalyst", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.target = input.read("target", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.hits = input.getIntOr("hits", 0);
        this.insertionOrder.clear();
        input.getIntArray("insertion_order").ifPresent(order -> {
            for (int slot : order) {
                this.insertionOrder.add(slot);
            }
        });
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.blueprint.isEmpty()) {
            output.store("blueprint", ItemStack.CODEC, this.blueprint);
        }

        if (!this.catalyst.isEmpty()) {
            output.store("catalyst", ItemStack.CODEC, this.catalyst);
        }

        if (!this.target.isEmpty()) {
            output.store("target", ItemStack.CODEC, this.target);
        }

        output.putInt("hits", this.hits);
        int[] order = new int[this.insertionOrder.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = this.insertionOrder.get(i);
        }

        output.putIntArray("insertion_order", order);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static boolean isCatalyst(ItemStack stack) {
        return stack.is(Items.DIAMOND_BLOCK);
    }
}
