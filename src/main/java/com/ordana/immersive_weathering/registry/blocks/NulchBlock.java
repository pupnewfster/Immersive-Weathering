package com.ordana.immersive_weathering.registry.blocks;

import com.ordana.immersive_weathering.registry.ModParticles;
import com.ordana.immersive_weathering.registry.ModTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class NulchBlock extends Block {

    public NulchBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(MOLTEN, false));
    }

    public static final BooleanProperty MOLTEN = BooleanProperty.of("molten");

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (state.get(MOLTEN)) {
            if (!entity.isFireImmune() && entity instanceof LivingEntity && !EnchantmentHelper.hasFrostWalker((LivingEntity) entity)) {
                entity.damage(DamageSource.HOT_FLOOR, 1.0F);
            }
        }
        super.onSteppedOn(world, pos, state, entity);
        if (entity instanceof LivingEntity) {
            if (world.isClient) {
                Random random = world.getRandom();
                boolean bl = entity.lastRenderX != entity.getX() || entity.lastRenderZ != entity.getZ();
                if (bl && random.nextBoolean()) {

                    world.addParticle(ModParticles.NULCH,
                            entity.getX() + MathHelper.nextBetween(random,-0.2f,0.2f),
                            pos.getY() + 1.025,
                            entity.getZ() + MathHelper.nextBetween(random,-0.2f,0.2f),
                            MathHelper.nextBetween(random,-0.9f,-1),
                            -1,
                            0);
                }
            }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!player.shouldCancelInteraction()) {
            // empty bucket into mulch
            if (player.getStackInHand(hand).isOf(Items.LAVA_BUCKET) && !state.get(MOLTEN)) {
                if (!player.isCreative()) {
                    player.setStackInHand(hand, new ItemStack(Items.BUCKET));
                }
                world.setBlockState(pos, state.with(MOLTEN, true));
                world.playSound(player, pos, SoundEvents.ITEM_BUCKET_EMPTY_LAVA, SoundCategory.BLOCKS, 1.0f, 1.0f);
                return ActionResult.SUCCESS;
            }
            // fill bucket from mulch
            else if (player.getStackInHand(hand).isOf(Items.BUCKET) && state.get(MOLTEN)) {
                if (!player.isCreative()) {
                    player.setStackInHand(hand, new ItemStack(Items.LAVA_BUCKET));
                }
                world.setBlockState(pos, state.with(MOLTEN, false));
                world.playSound(player, pos, SoundEvents.ITEM_BUCKET_FILL_LAVA, SoundCategory.BLOCKS, 1.0f, 1.0f);
                return ActionResult.SUCCESS;
            }
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public void randomDisplayTick(BlockState state, World level, BlockPos pos, Random random) {
        if (state.get(MOLTEN)) {
            if (random.nextInt(25) == 1) {
                BlockPos blockpos = pos.down();
                BlockState blockstate = level.getBlockState(blockpos);
                if (!blockstate.isOpaque() || !blockstate.isSideSolidFullSquare(level, blockpos, Direction.UP)) {
                    double d0 = (double) pos.getX() + random.nextDouble();
                    double d1 = (double) pos.getY() - 0.05D;
                    double d2 = (double) pos.getZ() + random.nextDouble();
                    level.addParticle(ParticleTypes.DRIPPING_LAVA, d0, d1, d2, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        var biome = world.getBiome(pos);
        for (Direction direction : Direction.values()) {
            var targetPos = pos.offset(direction);
            BlockState neighborState = world.getBlockState(targetPos);
            if (neighborState.isIn(ModTags.MAGMA_SOURCE)) {
                if (world.hasRain(pos.up())) {
                    return;
                }
                world.setBlockState(pos, state.with(MOLTEN, true), 2);
            }
        }
        if (biome.isIn(ModTags.ICY) || biome.isIn(ModTags.WET)) {
            if (world.random.nextFloat() < 0.4f) {
                world.setBlockState(pos, state.with(MOLTEN, false));
            }
        }
        else if (world.getRegistryKey() == World.NETHER) {
            if (world.random.nextFloat() < 0.5f) {
                world.setBlockState(pos, state.with(MOLTEN, true));
            }
        }
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.handleFallDamage(fallDistance, 0.2F, DamageSource.FALL);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
        stateManager.add(MOLTEN);
    }
}
