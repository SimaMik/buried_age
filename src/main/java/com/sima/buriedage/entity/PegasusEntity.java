package com.sima.buriedage.entity;

import com.sima.buriedage.client.pegasus.PegasusClientBridge;
import com.sima.buriedage.network.PegasusFlightPayload;
import com.sima.buriedage.registry.ModEntities;
import com.sima.buriedage.registry.ModTriggers;

import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A horse that flies. On the ground it is a vanilla horse in everything but jumping: the jump key
 * is the wing key. In the air the rider's client integrates the flight model (it is the authority
 * for a ridden mount, exactly as for a horse) and tells the server about takeoffs, landings and
 * crashes; the server owns stamina, the animation mode, sounds and the advancements.
 *
 * <p>All numbers live in {@link PegasusTuning}.
 */
public class PegasusEntity extends AbstractHorse {
    private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_STAMINA = SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.FLOAT);

    /** What the wings are doing, for animation and sound. Written by the server, read everywhere. */
    public enum Mode {
        GROUND, TAKEOFF, CLIMB, CRUISE, GLIDE, STALL, LANDING;

        static final Mode[] VALUES = values();

        public boolean airborne() {
            return this != GROUND && this != LANDING;
        }
    }

    public final AnimationState idleAnimation = new AnimationState();
    public final AnimationState walkAnimation = new AnimationState();
    public final AnimationState gallopAnimation = new AnimationState();
    public final AnimationState takeoffAnimation = new AnimationState();
    public final AnimationState flyAnimation = new AnimationState();
    public final AnimationState glideAnimation = new AnimationState();
    public final AnimationState landAnimation = new AnimationState();

    private boolean flying;
    private float airspeed;
    private float flightPitch;
    private float heading;
    private int stallTicks;
    private int stumbleTicks;
    private int takeoffCharge;
    private int airborneTicks;

    private float stamina = 1.0F;
    private int modeTimer;
    private int flapTimer;
    private double distanceFlown;
    private boolean everFlewWithRider;

    private float roll;
    private float visualPitch;
    private float yawRate;

    private @Nullable UUID pendingOwner;

    public PegasusEntity(EntityType<? extends PegasusEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.LAVA, -1.0F);
        this.setPathfindingMalus(PathType.FIRE, -1.0F);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGING, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, -1.0F);
        this.setPathfindingMalus(PathType.WATER, 12.0F);
        this.getNavigation().setCanFloat(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseHorseAttributes();
    }

    // ---------------------------------------------------------------- synced data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_FLYING, false);
        entityData.define(DATA_STAMINA, 1.0F);
        entityData.define(DATA_MODE, (byte) 0);
        entityData.define(DATA_PITCH, 0.0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_FLYING.equals(accessor) && !this.isLocalInstanceAuthoritative()) {
            this.flying = this.entityData.get(DATA_FLYING);
            if (this.flying) {
                this.airborneTicks = 0;
            }
        }
    }

    public boolean isFlying() {
        return this.flying;
    }

    /** 0..1, the jump bar. */
    public float getStamina() {
        return this.level().isClientSide() ? this.entityData.get(DATA_STAMINA) : this.stamina;
    }

    public Mode getMode() {
        int index = this.entityData.get(DATA_MODE);
        return index >= 0 && index < Mode.VALUES.length ? Mode.VALUES[index] : Mode.GROUND;
    }

    private void setMode(Mode mode) {
        if (this.getMode() != mode) {
            this.entityData.set(DATA_MODE, (byte) mode.ordinal());
            this.flapTimer = 0;
        }
    }

    public float getAirspeed() {
        return this.airspeed;
    }

    /** Bank angle for the renderer, degrees. */
    public float getRoll() {
        return this.roll;
    }

    /** Flight-path pitch for the renderer, degrees, positive nose down. */
    public float getVisualPitch() {
        return this.visualPitch;
    }

    // ---------------------------------------------------------------- birth and breeding

    @Override
    protected void randomizeAttributes(RandomSource random) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
                PegasusTuning.HEALTH_BASE + random.nextInt(PegasusTuning.HEALTH_RANDOM_A) + random.nextInt(PegasusTuning.HEALTH_RANDOM_B));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                PegasusTuning.SPEED_BASE + random.nextDouble() * PegasusTuning.SPEED_RANDOM + random.nextDouble() * PegasusTuning.SPEED_RANDOM);
        this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateJumpStrength(random::nextDouble));
        this.setHealth(this.getMaxHealth());
    }

    @Override
    public boolean canMate(Animal partner) {
        return partner != this && partner instanceof PegasusEntity other && this.canParent() && other.canParent();
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        PegasusEntity foal = ModEntities.PEGASUS.get().create(level, EntitySpawnReason.BREEDING);
        if (foal != null && partner instanceof PegasusEntity) {
            this.setOffspringAttributes(partner, foal);
            foal.setHealth(foal.getMaxHealth());
            LivingEntity owner = this.getOwner();
            if (owner != null) {
                foal.setOwner(owner);
                foal.setTamed(true);
            }
        }
        return foal;
    }

    @Override
    protected void setOffspringAttributes(AgeableMob partner, AbstractHorse foal) {
        this.inherit(partner, foal, Attributes.MAX_HEALTH, PegasusTuning.HEALTH_MIN, PegasusTuning.HEALTH_MAX);
        this.inherit(partner, foal, Attributes.MOVEMENT_SPEED, PegasusTuning.SPEED_MIN, PegasusTuning.SPEED_MAX);
        foal.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateJumpStrength(this.random::nextDouble));
    }

    /** Vanilla's rule for horse foals, with the pegasus ranges: parents' average plus a spread. */
    private void inherit(AgeableMob partner, AbstractHorse foal, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double min, double max) {
        double a = Mth.clamp(this.getAttributeBaseValue(attribute), min, max);
        double b = Mth.clamp(partner.getAttributeBaseValue(attribute), min, max);
        double margin = 0.15 * (max - min);
        double range = Math.abs(a - b) + margin * 2.0;
        double quality = (this.random.nextDouble() + this.random.nextDouble() + this.random.nextDouble()) / 3.0 - 0.5;
        double value = (a + b) / 2.0 + range * quality;
        if (value > max) {
            value = max - (value - max);
        } else if (value < min) {
            value = min + (min - value);
        }
        foal.getAttribute(attribute).setBaseValue(value);
    }

    /** The player who placed the egg was not online when it hatched: claim them when they next appear. */
    public void setPendingOwner(@Nullable UUID owner) {
        this.pendingOwner = owner;
    }

    public @Nullable LivingEntity getOwner() {
        var reference = this.getOwnerReference();
        return reference == null ? null : reference.getEntity(this.level(), LivingEntity.class);
    }

    // ---------------------------------------------------------------- food and interaction

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.ENCHANTED_GOLDEN_APPLE) || stack.is(Items.GOLDEN_APPLE) || super.isFood(stack);
    }

    @Override
    protected boolean handleEating(Player player, ItemStack stack) {
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            boolean used = false;
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(PegasusTuning.GOLDEN_APPLE_HEAL);
                used = true;
            }
            if (!this.level().isClientSide() && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
                this.setInLove(player);
                used = true;
            }
            if (!this.level().isClientSide() && this.restoreStamina()) {
                used = true;
            }
            if (used) {
                this.eating();
            }
            return used;
        }

        if (stack.is(Items.GOLDEN_APPLE)) {
            boolean used = false;
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(PegasusTuning.GOLDEN_APPLE_HEAL);
                used = true;
            }
            if (this.isBaby() && !this.isAgeLocked()) {
                this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
                if (!this.level().isClientSide()) {
                    this.ageUp(PegasusTuning.GOLDEN_APPLE_AGE_SECONDS);
                    used = true;
                }
            }
            if (!this.level().isClientSide() && this.restoreStamina()) {
                used = true;
            }
            if (used) {
                this.eating();
            }
            return used;
        }

        return super.handleEating(player, stack);
    }

    private void eating() {
        this.setEating(true);
        this.gameEvent(net.minecraft.world.level.gameevent.GameEvent.EAT);
    }

    private boolean restoreStamina() {
        if (this.stamina >= 1.0F) {
            return false;
        }
        this.stamina = 1.0F;
        this.entityData.set(DATA_STAMINA, 1.0F);
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean openInventory = !this.isBaby() && this.isTamed() && player.isSecondaryUseActive();
        if (!this.isVehicle() && !openInventory) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty()) {
                if (this.isFood(stack)) {
                    return this.fedFood(player, stack);
                }
                if (!this.isTamed()) {
                    this.makeMad();
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        return true;
    }

    @Override
    protected void hurtArmor(DamageSource source, float damage) {
        this.doHurtEquipment(source, damage, EquipmentSlot.BODY);
    }

    // ---------------------------------------------------------------- sounds

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HORSE_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HORSE_HURT;
    }

    @Override
    protected SoundEvent getEatingSound() {
        return SoundEvents.HORSE_EAT;
    }

    @Override
    protected SoundEvent getAngrySound() {
        return SoundEvents.HORSE_ANGRY;
    }

    @Override
    protected void playJumpSound() {
    }

    // ---------------------------------------------------------------- the jump key is the wing key

    @Override
    public boolean canJump() {
        return this.isSaddled() && PegasusTuning.FLIGHT_ENABLED && !this.isBaby();
    }

    @Override
    public void onPlayerJump(int jumpAmount) {
    }

    @Override
    public void handleStartJump(int jumpScale) {
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        if (!this.isBaby() && PegasusTuning.FLIGHT_ENABLED) {
            return false;
        }
        return super.causeFallDamage(fallDistance, damageModifier, source);
    }

    private static Input riderInput(Player rider) {
        if (rider instanceof ServerPlayer serverPlayer) {
            return serverPlayer.getLastClientInput();
        }
        return rider.level().isClientSide() ? PegasusClientBridge.input(rider) : Input.EMPTY;
    }

    /** What the rider is asking for this tick. Computed the same way on both sides. */
    private record Intent(boolean forward, boolean climb, float pitch) {
        static final Intent NONE = new Intent(false, false, PegasusTuning.GLIDE_PITCH);

        static Intent of(Player rider) {
            Input input = riderInput(rider);
            float pitch = Mth.clamp(rider.getXRot(), -PegasusTuning.MAX_PITCH, PegasusTuning.MAX_PITCH);
            return new Intent(input.forward(), input.jump(), pitch);
        }
    }

    private @Nullable Player rider() {
        return this.getControllingPassenger() instanceof Player player ? player : null;
    }

    // ---------------------------------------------------------------- riding

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        if (!this.isLocalInstanceAuthoritative() || !PegasusTuning.FLIGHT_ENABLED || this.isBaby()) {
            return;
        }

        if (this.flying) {
            this.takeoffCharge = 0;
            return;
        }

        Input input = riderInput(controller);
        if (this.onGround() && input.jump() && this.stumbleTicks == 0 && this.getStamina() >= PegasusTuning.TAKEOFF_MIN_STAMINA) {
            this.takeoffCharge++;
            double groundSpeed = this.getDeltaMovement().horizontalDistance();
            boolean running = this.takeoffCharge >= 2 && groundSpeed >= PegasusTuning.RUN_TAKEOFF_SPEED;
            if (this.takeoffCharge >= PegasusTuning.TAKEOFF_CHARGE_TICKS || running) {
                this.takeOff(groundSpeed);
            }
        } else {
            this.takeoffCharge = 0;
        }
    }

    @Override
    protected Vec2 getRiddenRotation(LivingEntity controller) {
        if (this.flying) {
            return new Vec2(this.getXRot(), this.getYRot());
        }
        return super.getRiddenRotation(controller);
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        if (this.stumbleTicks > 0) {
            return Vec3.ZERO;
        }
        return super.getRiddenInput(controller, selfInput);
    }

    private void takeOff(double groundSpeed) {
        this.flying = true;
        this.airborneTicks = 0;
        this.takeoffCharge = 0;
        this.stallTicks = 0;
        this.heading = this.getYRot();
        this.flightPitch = PegasusTuning.TAKEOFF_PITCH;
        this.airspeed = (float) Math.max(groundSpeed + 0.1, PegasusTuning.TAKEOFF_SPEED);
        this.reportFlight(PegasusFlightPayload.Kind.TAKEOFF, this.airspeed);
    }

    private void land(float speed) {
        this.flying = false;
        this.airspeed = 0.0F;
        this.stallTicks = 0;
        this.stumbleTicks = speed > PegasusTuning.SOFT_LANDING_SPEED ? PegasusTuning.STUMBLE_TICKS : 0;
        this.reportFlight(PegasusFlightPayload.Kind.LANDING, speed);
    }

    private void crash(float speed) {
        this.airspeed = speed * PegasusTuning.CRASH_SPEED_KEPT;
        this.reportFlight(PegasusFlightPayload.Kind.CRASH, speed);
    }

    /** The rider's client tells the server; the server applies the same change to itself directly. */
    private void reportFlight(PegasusFlightPayload.Kind kind, float speed) {
        if (this.level().isClientSide()) {
            PegasusClientBridge.send(new PegasusFlightPayload(this.getId(), kind, speed));
        } else if (this.level() instanceof ServerLevel) {
            this.applyFlightEvent(kind, speed, this.rider() instanceof ServerPlayer rider ? rider : null);
        }
    }

    /** Server side. Everything a flight event changes that other players must see or that costs health. */
    public void applyFlightEvent(PegasusFlightPayload.Kind kind, float speed, @Nullable ServerPlayer rider) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        switch (kind) {
            case TAKEOFF -> {
                this.flying = true;
                this.airborneTicks = 0;
                this.distanceFlown = 0.0;
                this.entityData.set(DATA_FLYING, true);
                this.setMode(Mode.TAKEOFF);
                this.modeTimer = PegasusTuning.TAKEOFF_ANIMATION_TICKS;
                this.playSound(SoundEvents.PHANTOM_FLAP, PegasusTuning.FLAP_VOLUME, 0.8F);
                if (rider != null) {
                    this.everFlewWithRider = true;
                    ModTriggers.PEGASUS.get().trigger(rider, "first_flight");
                }
            }
            case LANDING -> {
                this.flying = false;
                this.entityData.set(DATA_FLYING, false);
                this.setMode(Mode.LANDING);
                this.modeTimer = PegasusTuning.LANDING_ANIMATION_TICKS;
                this.distanceFlown = 0.0;
                if (speed > PegasusTuning.SOFT_LANDING_SPEED) {
                    float damage = (speed - PegasusTuning.SOFT_LANDING_SPEED) * PegasusTuning.HARD_LANDING_DAMAGE;
                    this.playSound(SoundEvents.HORSE_LAND, 0.8F, 0.8F);
                    if (rider != null) {
                        rider.hurtServer(level, level.damageSources().fall(), damage);
                    }
                    this.hurtServer(level, level.damageSources().fall(), damage * 0.5F);
                    this.stumbleTicks = PegasusTuning.STUMBLE_TICKS;
                } else {
                    this.playSound(SoundEvents.HORSE_LAND, 0.4F, 1.0F);
                }
            }
            case CRASH -> {
                if (speed > PegasusTuning.CRASH_SPEED) {
                    float damage = (speed - PegasusTuning.CRASH_SPEED) * PegasusTuning.CRASH_DAMAGE;
                    if (rider != null) {
                        rider.hurtServer(level, level.damageSources().flyIntoWall(), damage);
                    }
                    this.hurtServer(level, level.damageSources().flyIntoWall(), damage);
                }
            }
            case STALL -> {
                this.setMode(Mode.STALL);
                this.modeTimer = PegasusTuning.STALL_TICKS;
            }
        }
    }

    // ---------------------------------------------------------------- flight model

    @Override
    public void travel(Vec3 input) {
        if (this.flying && PegasusTuning.FLIGHT_ENABLED && this.isLocalInstanceAuthoritative()) {
            this.flyTick();
            return;
        }
        super.travel(input);
    }

    /**
     * One tick of flight on the authoritative side. The core is an exchange between height and
     * airspeed: the nose angle decides how much gravity feeds into speed, wings add a little thrust,
     * drag takes a share proportional to speed squared. Direction changes are rate-limited and
     * get slower with speed; a nose-up attitude without the speed to carry it stalls.
     */
    private void flyTick() {
        Player rider = this.rider();
        Intent intent = rider != null ? Intent.of(rider) : Intent.NONE;
        boolean hasStamina = this.getStamina() > 0.0F;
        boolean stalled = this.stallTicks > 0;

        float targetPitch;
        if (stalled) {
            targetPitch = PegasusTuning.STALL_NOSE_DOWN;
        } else if (intent.climb() && hasStamina) {
            targetPitch = Math.min(intent.forward() ? intent.pitch() : PegasusTuning.CLIMB_PITCH, PegasusTuning.CLIMB_PITCH);
        } else if (intent.forward()) {
            targetPitch = intent.pitch();
        } else {
            targetPitch = PegasusTuning.GLIDE_PITCH;
        }
        if (!hasStamina) {
            targetPitch = Math.max(targetPitch, PegasusTuning.FORCED_GLIDE_MIN_PITCH);
        }
        float pitchRate = stalled ? PegasusTuning.STALLED_PITCH_RATE : PegasusTuning.PITCH_RATE;
        this.flightPitch = approach(this.flightPitch, targetPitch, pitchRate);

        float targetHeading = rider != null ? rider.getYRot() : this.heading;
        float turnRate = PegasusTuning.TURN_RATE / (1.0F + this.airspeed / PegasusTuning.TURN_SPEED_SCALE);
        if (stalled) {
            turnRate *= PegasusTuning.STALLED_TURN_FACTOR;
        }
        this.heading = approachAngle(this.heading, targetHeading, turnRate);

        float pitchRad = this.flightPitch * Mth.DEG_TO_RAD;
        float taper = Math.max(0.0F, 1.0F - this.airspeed / PegasusTuning.CRUISE_MAX_SPEED);
        float thrust = 0.0F;
        if (!stalled && hasStamina) {
            if (intent.forward()) {
                thrust += PegasusTuning.THRUST * taper;
            }
            if (intent.climb()) {
                thrust += PegasusTuning.CLIMB_THRUST_BONUS * taper;
            }
        }
        float delta = PegasusTuning.GRAVITY_EXCHANGE * Mth.sin(pitchRad) + thrust - PegasusTuning.DRAG * this.airspeed * this.airspeed;
        this.airspeed = Mth.clamp(this.airspeed + delta, 0.0F, PegasusTuning.DIVE_MAX_SPEED);

        if (!stalled && this.flightPitch < PegasusTuning.STALL_PITCH && this.airspeed < PegasusTuning.STALL_SPEED) {
            this.stallTicks = PegasusTuning.STALL_TICKS;
            this.reportFlight(PegasusFlightPayload.Kind.STALL, this.airspeed);
        } else if (stalled) {
            this.stallTicks--;
            if (this.stallTicks == 0 && this.airspeed < PegasusTuning.STALL_EXIT_SPEED) {
                this.stallTicks = 5;
            }
        }

        float yawRad = this.heading * Mth.DEG_TO_RAD;
        float horizontal = Mth.cos(pitchRad) * this.airspeed;
        Vec3 velocity = new Vec3(-Mth.sin(yawRad) * horizontal, -Mth.sin(pitchRad) * this.airspeed, Mth.cos(yawRad) * horizontal);
        if (this.airspeed < PegasusTuning.MIN_AIRSPEED) {
            velocity = velocity.add(0.0, -0.08, 0.0);
        }

        this.setDeltaMovement(velocity);
        this.move(MoverType.SELF, velocity);
        this.resetFallDistance();

        this.setYRot(this.heading);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.heading;
        this.setXRot(this.flightPitch * 0.5F);
        this.visualPitch = this.flightPitch;
        this.airborneTicks++;

        if (this.airborneTicks > PegasusTuning.TAKEOFF_GRACE_TICKS) {
            if (this.onGround() || this.verticalCollisionBelow) {
                this.land(this.airspeed);
            } else if (this.horizontalCollision && this.airspeed > PegasusTuning.CRASH_SPEED) {
                this.crash(this.airspeed);
            }
        }
    }

    private static float approach(float current, float target, float rate) {
        float diff = target - current;
        return Math.abs(diff) <= rate ? target : current + Math.signum(diff) * rate;
    }

    private static float approachAngle(float current, float target, float rate) {
        float diff = Mth.wrapDegrees(target - current);
        return Math.abs(diff) <= rate ? target : current + Math.signum(diff) * rate;
    }

    // ---------------------------------------------------------------- per tick

    @Override
    public void tick() {
        Vec3 observed = this.position().subtract(this.xo, this.yo, this.zo);
        super.tick();

        if (this.stumbleTicks > 0) {
            this.stumbleTicks--;
        }

        if (this.level() instanceof ServerLevel level) {
            this.serverTick(level, observed);
        } else {
            this.clientTick();
        }
    }

    private void serverTick(ServerLevel level, Vec3 observed) {
        if (this.pendingOwner != null && this.tickCount % 20 == 0) {
            Player owner = level.getPlayerByUUID(this.pendingOwner);
            if (owner != null) {
                this.setOwner(owner);
                this.setTamed(true);
                this.pendingOwner = null;
                if (owner instanceof ServerPlayer serverOwner) {
                    ModTriggers.PEGASUS.get().trigger(serverOwner, "hatched");
                }
            }
        }

        Player rider = this.rider();
        Intent intent = rider != null ? Intent.of(rider) : Intent.NONE;
        double observedSpeed = observed.length();

        if (!this.isLocalInstanceAuthoritative()) {
            this.visualPitch = observedSpeed > 0.05
                    ? (float) (-Math.atan2(observed.y, observed.horizontalDistance()) * Mth.RAD_TO_DEG)
                    : this.visualPitch * 0.9F;
        }
        this.entityData.set(DATA_PITCH, this.flying ? this.visualPitch : 0.0F);

        if (this.flying) {
            float cost;
            boolean climbing = intent.climb() && this.stamina > 0.0F;
            boolean powered = intent.forward() && this.stamina > 0.0F && this.visualPitch <= PegasusTuning.FORCED_GLIDE_MIN_PITCH;
            if (climbing) {
                cost = PegasusTuning.CLIMB_COST;
            } else if (powered) {
                cost = PegasusTuning.CRUISE_COST;
            } else {
                cost = 0.0F;
            }
            this.stamina = Math.max(0.0F, this.stamina - cost);

            this.distanceFlown += observed.horizontalDistance();
            if (rider instanceof ServerPlayer serverRider && this.distanceFlown >= PegasusTuning.ICARUS_DISTANCE) {
                ModTriggers.PEGASUS.get().trigger(serverRider, "icarus");
                this.distanceFlown = 0.0;
            }

            if (this.modeTimer > 0) {
                this.modeTimer--;
            } else if (climbing) {
                this.setMode(Mode.CLIMB);
            } else if (powered) {
                this.setMode(Mode.CRUISE);
            } else {
                this.setMode(Mode.GLIDE);
            }
        } else {
            if (this.onGround()) {
                this.stamina = Math.min(1.0F, this.stamina + PegasusTuning.GROUND_REGEN);
            }
            if (this.modeTimer > 0) {
                this.modeTimer--;
            } else {
                this.setMode(Mode.GROUND);
            }
        }
        this.entityData.set(DATA_STAMINA, this.stamina);

        int interval = switch (this.getMode()) {
            case CLIMB -> PegasusTuning.FLAP_INTERVAL_CLIMB;
            case CRUISE -> PegasusTuning.FLAP_INTERVAL_CRUISE;
            case GLIDE, STALL -> PegasusTuning.FLAP_INTERVAL_GLIDE;
            case TAKEOFF -> PegasusTuning.FLAP_INTERVAL_TAKEOFF;
            default -> 0;
        };
        if (interval > 0 && ++this.flapTimer >= interval) {
            this.flapTimer = 0;
            this.playSound(SoundEvents.PHANTOM_FLAP, PegasusTuning.FLAP_VOLUME, 0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    private void clientTick() {
        Mode mode = this.getMode();
        this.idleAnimation.animateWhen(mode == Mode.GROUND && this.walkAnimation.isStarted() == false && !this.isMovingOnGround(), this.tickCount);
        this.walkAnimation.animateWhen(mode == Mode.GROUND && this.isMovingOnGround() && !this.isGalloping(), this.tickCount);
        this.gallopAnimation.animateWhen(mode == Mode.GROUND && this.isGalloping(), this.tickCount);
        this.takeoffAnimation.animateWhen(mode == Mode.TAKEOFF, this.tickCount);
        this.flyAnimation.animateWhen(mode == Mode.CLIMB || mode == Mode.CRUISE, this.tickCount);
        this.glideAnimation.animateWhen(mode == Mode.GLIDE || mode == Mode.STALL, this.tickCount);
        this.landAnimation.animateWhen(mode == Mode.LANDING, this.tickCount);

        float rate = Mth.wrapDegrees(this.getYRot() - this.yRotO);
        this.yawRate = this.yawRate + (rate - this.yawRate) * 0.3F;
        boolean airborne = this.isLocalInstanceAuthoritative() ? this.flying : mode.airborne();
        float targetRoll = airborne ? Mth.clamp(this.yawRate * PegasusTuning.ROLL_PER_YAW_RATE, -PegasusTuning.MAX_ROLL, PegasusTuning.MAX_ROLL) : 0.0F;
        this.roll += (targetRoll - this.roll) * PegasusTuning.ROLL_SMOOTHING;
        if (!this.isLocalInstanceAuthoritative()) {
            this.visualPitch = this.entityData.get(DATA_PITCH);
        } else if (!this.flying) {
            this.visualPitch *= 0.8F;
        }
    }

    private boolean isMovingOnGround() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 0.0004;
    }

    private boolean isGalloping() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 0.09;
    }

    // ---------------------------------------------------------------- death

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide() && (this.flying || !this.onGround())) {
            for (Entity passenger : this.getPassengers()) {
                if (passenger instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, PegasusTuning.DEATH_SLOW_FALLING_TICKS, 0), this);
                }
            }
        }
        super.die(source);
    }

    // ---------------------------------------------------------------- persistence

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Stamina", this.stamina);
        output.putBoolean("Flying", this.flying);
        output.putBoolean("EverFlewWithRider", this.everFlewWithRider);
        if (this.pendingOwner != null) {
            output.store("PendingOwner", UUIDUtil.CODEC, this.pendingOwner);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.stamina = Mth.clamp(input.getFloatOr("Stamina", 1.0F), 0.0F, 1.0F);
        this.flying = input.getBooleanOr("Flying", false);
        this.everFlewWithRider = input.getBooleanOr("EverFlewWithRider", false);
        this.pendingOwner = input.read("PendingOwner", UUIDUtil.CODEC).orElse(null);
        this.entityData.set(DATA_STAMINA, this.stamina);
        this.entityData.set(DATA_FLYING, this.flying);
    }
}
