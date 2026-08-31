package com.sima.buriedage.entity;

import com.sima.buriedage.registry.ModEntities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A ghost of somebody who lived here. Pure decoration: no AI, no pathfinding, no drops, never
 * saved. Everything it does is a small timer-driven state machine, see {@link EchoTuning}.
 *
 * <p>It extends LivingEntity only so the vanilla villager model and its profession clothing can be
 * reused by the renderer; nothing about it is alive in the gameplay sense.
 */
public class EchoEntity extends LivingEntity {
    /** Which villager look this echo wears. Index into {@link EchoTuning#PROFESSIONS}. */
    private static final EntityDataAccessor<Integer> DATA_PROFESSION =
            SynchedEntityData.defineId(EchoEntity.class, EntityDataSerializers.INT);
    /** 0..1, drives the fade in the renderer. */
    private static final EntityDataAccessor<Float> DATA_OPACITY =
            SynchedEntityData.defineId(EchoEntity.class, EntityDataSerializers.FLOAT);

    /** What this echo is doing with its brief existence. */
    public enum Mode {
        DRIFT,
        WANDER,
        MEETING
    }

    private Mode mode = Mode.DRIFT;
    private int age;
    private int lifetime = EchoTuning.DRIFT_LIFETIME_MIN;
    private Vec3 heading = Vec3.ZERO;
    /** False until someone has given this echo a life to live. */
    private boolean configured;
    private float speed = EchoTuning.DRIFT_SPEED;

    /** WANDER: alternates walking and pausing. */
    private int scriptStepsLeft;
    private int phaseTicksLeft;
    private boolean paused;

    /** MEETING: where the pair stops, and who the partner is. */
    private Vec3 meetingPoint = Vec3.ZERO;
    private int meetingId = -1;
    private boolean meetingReached;

    public EchoEntity(EntityType<? extends EchoEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PROFESSION, 0);
        builder.define(DATA_OPACITY, 1.0F);
    }

    // ---------------------------------------------------------------- setup

    /** Called by the spawner right after the entity is created, before it is added to the level. */
    public void configure(Mode mode, Vec3 heading, int professionIndex) {
        this.configured = true;
        this.mode = mode;
        this.heading = heading.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : heading.normalize();
        this.entityData.set(DATA_PROFESSION, professionIndex);
        this.setYRot((float) (Mth.atan2(this.heading.z, this.heading.x) * 180.0F / Math.PI) - 90.0F);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();

        switch (mode) {
            case DRIFT -> {
                this.lifetime = EchoTuning.randomBetween(this.random, EchoTuning.DRIFT_LIFETIME_MIN, EchoTuning.DRIFT_LIFETIME_MAX);
                this.speed = EchoTuning.DRIFT_SPEED;
            }
            case WANDER -> {
                this.lifetime = EchoTuning.randomBetween(this.random, EchoTuning.WANDER_LIFETIME_MIN, EchoTuning.WANDER_LIFETIME_MAX);
                this.speed = EchoTuning.WANDER_SPEED;
                this.scriptStepsLeft = EchoTuning.randomBetween(this.random, EchoTuning.WANDER_STEPS_MIN, EchoTuning.WANDER_STEPS_MAX);
                this.beginWalkPhase();
            }
            case MEETING -> {
                this.lifetime = EchoTuning.randomBetween(this.random, EchoTuning.MEETING_LIFETIME_MIN, EchoTuning.MEETING_LIFETIME_MAX);
                this.speed = EchoTuning.MEETING_SPEED;
            }
        }
    }

    /** Gives a life to an echo that was summoned rather than placed by the spawner. Meetings need a
     * partner, so a lone echo takes that share as a wander instead. */
    private void configureRandomly() {
        float angle = this.random.nextFloat() * Mth.TWO_PI;
        Vec3 direction = new Vec3(Mth.cos(angle), 0.0, Mth.sin(angle));
        int roll = this.random.nextInt(EchoTuning.WEIGHT_DRIFT + EchoTuning.WEIGHT_WANDER + EchoTuning.WEIGHT_MEETING);
        Mode picked = roll < EchoTuning.WEIGHT_DRIFT ? Mode.DRIFT : Mode.WANDER;
        this.configure(picked, direction, this.random.nextInt(EchoTuning.PROFESSIONS.length));
    }

    /** MEETING only: where the two of them stop, and the partner to turn towards. */
    public void configureMeeting(Vec3 meetingPoint, EchoEntity partner) {
        this.meetingPoint = meetingPoint;
        this.meetingId = partner.getId();
    }

    public Mode getMode() {
        return this.mode;
    }

    public int getProfessionIndex() {
        return this.entityData.get(DATA_PROFESSION);
    }

    public float getOpacity() {
        return this.entityData.get(DATA_OPACITY);
    }

    // ---------------------------------------------------------------- behaviour

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        // Spawn eggs and /summon build the entity straight from the constructor and never call
        // configure, which used to leave the heading at zero: the echo just stood there.
        if (!this.configured) {
            this.configureRandomly();
        }

        this.age++;

        if (this.age >= this.lifetime) {
            this.discard();
            return;
        }

        switch (this.mode) {
            case DRIFT -> this.tickDrift();
            case WANDER -> this.tickWander();
            case MEETING -> this.tickMeeting();
        }

        this.entityData.set(DATA_OPACITY, this.computeOpacity());
    }

    private void tickDrift() {
        this.move(this.heading, this.speed);
        this.bob(EchoTuning.DRIFT_BOB);
    }

    private void tickWander() {
        this.phaseTicksLeft--;
        if (this.paused) {
            this.bob(EchoTuning.WANDER_PAUSE_BOB);
            // Busy with something: glance around while standing still.
            if (this.phaseTicksLeft % EchoTuning.WANDER_GLANCE_INTERVAL == 0) {
                this.yHeadRot = this.getYRot() + (this.random.nextFloat() - 0.5F) * EchoTuning.WANDER_GLANCE_SPREAD;
            }

            if (this.phaseTicksLeft <= 0) {
                if (--this.scriptStepsLeft <= 0) {
                    this.lifetime = Math.min(this.lifetime, this.age + EchoTuning.FADE_TICKS);
                } else {
                    this.turnRandomly();
                    this.beginWalkPhase();
                }
            }
        } else {
            this.move(this.heading, this.speed);
            this.bob(EchoTuning.WANDER_BOB);
            if (this.phaseTicksLeft <= 0) {
                this.paused = true;
                this.phaseTicksLeft = EchoTuning.randomBetween(this.random,
                        EchoTuning.WANDER_PAUSE_MIN, EchoTuning.WANDER_PAUSE_MAX);
            }
        }
    }

    private void tickMeeting() {
        if (!this.meetingReached) {
            Vec3 toMeeting = this.meetingPoint.subtract(this.position());
            if (toMeeting.horizontalDistanceSqr() < EchoTuning.MEETING_ARRIVAL_RADIUS_SQR) {
                this.meetingReached = true;
                this.phaseTicksLeft = EchoTuning.randomBetween(this.random,
                        EchoTuning.MEETING_TALK_MIN, EchoTuning.MEETING_TALK_MAX);
            } else {
                this.heading = new Vec3(toMeeting.x, 0.0, toMeeting.z).normalize();
                this.faceHeading();
                this.move(this.heading, this.speed);
                this.bob(EchoTuning.MEETING_BOB);
            }

            return;
        }

        if (this.phaseTicksLeft > 0) {
            this.phaseTicksLeft--;
            this.bob(EchoTuning.MEETING_TALK_BOB);
            // Turn the head towards the partner for the length of the conversation.
            if (this.level().getEntity(this.meetingId) instanceof EchoEntity partner) {
                Vec3 toPartner = partner.position().subtract(this.position());
                this.yHeadRot = (float) (Mth.atan2(toPartner.z, toPartner.x) * 180.0F / Math.PI) - 90.0F;
            }

            if (this.phaseTicksLeft == 0) {
                // Conversation over: walk away in opposite directions.
                this.heading = this.heading.reverse();
                this.faceHeading();
            }

            return;
        }

        this.move(this.heading, this.speed);
        this.bob(EchoTuning.MEETING_BOB);
    }

    private void beginWalkPhase() {
        int blocks = EchoTuning.randomBetween(this.random, EchoTuning.WANDER_LEG_BLOCKS_MIN, EchoTuning.WANDER_LEG_BLOCKS_MAX);
        this.paused = false;
        this.phaseTicksLeft = Math.round(blocks / EchoTuning.WANDER_SPEED);
        this.faceHeading();
    }

    private void turnRandomly() {
        float turn = (this.random.nextFloat() - 0.5F) * EchoTuning.WANDER_TURN_SPREAD;
        double radians = Math.toRadians(turn);
        double x = this.heading.x * Math.cos(radians) - this.heading.z * Math.sin(radians);
        double z = this.heading.x * Math.sin(radians) + this.heading.z * Math.cos(radians);
        this.heading = new Vec3(x, 0.0, z).normalize();
    }

    private void faceHeading() {
        this.setYRot((float) (Mth.atan2(this.heading.z, this.heading.x) * 180.0F / Math.PI) - 90.0F);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private void move(Vec3 direction, float blocksPerTick) {
        this.setPos(this.getX() + direction.x * blocksPerTick,
                this.getY(),
                this.getZ() + direction.z * blocksPerTick);
    }

    /** Gentle vertical sway so a standing echo never looks frozen. */
    private void bob(float amplitude) {
        double offset = Math.sin((this.age + this.getId()) * EchoTuning.BOB_SPEED) * amplitude;
        this.setPos(this.getX(), this.getY() + offset, this.getZ());
    }

    private float computeOpacity() {
        int left = this.lifetime - this.age;
        if (left < EchoTuning.FADE_TICKS) {
            return Math.max(0.0F, (float) left / EchoTuning.FADE_TICKS);
        }

        if (this.age < EchoTuning.FADE_IN_TICKS) {
            return (float) this.age / EchoTuning.FADE_IN_TICKS;
        }

        return 1.0F;
    }

    // ---------------------------------------------------------------- being a ghost

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // Touched by anything at all: dissolve rather than take a hit.
        this.dissolve(level);
        return false;
    }

    private void dissolve(ServerLevel level) {
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.0, this.getZ(),
                EchoTuning.DISSOLVE_PARTICLES, 0.2, 0.4, 0.2, 0.01);
        this.discard();
    }

    /** Whisper on arrival. Called by the spawner once the entity is in the level. */
    public void playArrivalSound(ServerLevel level) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.SCULK_CLICKING, SoundSource.AMBIENT,
                EchoTuning.WHISPER_VOLUME, EchoTuning.WHISPER_PITCH);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        // Nothing: echoes never persist.
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // Nothing: echoes never persist.
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity other) {
        // Ghosts do not shove anybody.
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(net.minecraft.world.entity.@org.jspecify.annotations.Nullable Entity entity) {
        return false;
    }

    @Override
    public boolean isAffectedByFluids() {
        return false;
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        // Silent.
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public EntityType<?> getType() {
        return ModEntities.ECHO.get();
    }
}
