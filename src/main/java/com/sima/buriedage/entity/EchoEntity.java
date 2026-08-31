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

public class EchoEntity extends LivingEntity {
    private static final EntityDataAccessor<Integer> DATA_PROFESSION =
            SynchedEntityData.defineId(EchoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_OPACITY =
            SynchedEntityData.defineId(EchoEntity.class, EntityDataSerializers.FLOAT);

    public enum Mode {
        DRIFT,
        WANDER,
        MEETING
    }

    private Mode mode = Mode.DRIFT;
    private int age;
    private int lifetime = EchoTuning.DRIFT_LIFETIME_MIN;
    private Vec3 heading = Vec3.ZERO;
    private boolean configured;
    private float speed = EchoTuning.DRIFT_SPEED;

    private int scriptStepsLeft;
    private int phaseTicksLeft;
    private boolean paused;

    private Vec3 meetingPoint = Vec3.ZERO;
    private int meetingId = -1;
    private boolean meetingReached;

    public EchoEntity(EntityType<? extends EchoEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
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

    private void configureRandomly() {
        float angle = this.random.nextFloat() * Mth.TWO_PI;
        Vec3 direction = new Vec3(Mth.cos(angle), 0.0, Mth.sin(angle));
        int roll = this.random.nextInt(EchoTuning.WEIGHT_DRIFT + EchoTuning.WEIGHT_WANDER);
        Mode picked = roll < EchoTuning.WEIGHT_DRIFT ? Mode.DRIFT : Mode.WANDER;
        this.configure(picked, direction, this.random.nextInt(EchoTuning.PROFESSIONS.length));
    }

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

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

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
            if (this.level().getEntity(this.meetingId) instanceof EchoEntity partner) {
                Vec3 toPartner = partner.position().subtract(this.position());
                this.yHeadRot = (float) (Mth.atan2(toPartner.z, toPartner.x) * 180.0F / Math.PI) - 90.0F;
            }

            if (this.phaseTicksLeft == 0) {
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

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        this.dissolve(level);
        return false;
    }

    private void dissolve(ServerLevel level) {
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.0, this.getZ(),
                EchoTuning.DISSOLVE_PARTICLES, 0.2, 0.4, 0.2, 0.01);
        this.discard();
    }

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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity other) {
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
