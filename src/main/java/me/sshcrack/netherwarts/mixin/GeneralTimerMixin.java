package me.sshcrack.netherwarts.mixin;


import me.sshcrack.netherwarts.MainMod;
import me.sshcrack.netherwarts.MessageManager;
import me.sshcrack.netherwarts.Rect;
import me.sshcrack.netherwarts.manager.FarmState;
import me.sshcrack.netherwarts.manager.GeneralTimerAccess;
import me.sshcrack.netherwarts.manager.inv.StorageManager;
import me.sshcrack.netherwarts.manager.inv.baic.InventoryManager;
import me.sshcrack.netherwarts.manager.inv.multiple.MultipleReturnState;
import me.sshcrack.netherwarts.manager.inv.multiple.QuickItemMover;
import me.sshcrack.netherwarts.manager.inv.single.SingleItemMover;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(GameRenderer.class)
public abstract class GeneralTimerMixin implements GeneralTimerAccess {
    private static final int MAX_WART_AGE = 3;
    private static final int HOE_SLOT = 2;
    private static final int WART_SLOT = 8;
    private static final int MIN_SLOTS_FREE = 4;

    private boolean enabled = false;
    private Pair<BlockPos, BlockPos> rect;


    private final List<BlockPos> toMine = new ArrayList<>();
    private final List<BlockPos> errorBlocksReported = new ArrayList<>();
    private List<BlockPos> storageBlocks = new ArrayList<>();

    private BlockPos curr = null;
    private FarmState state = FarmState.WALKING;

    private SingleItemMover singleMover;
    private QuickItemMover quickMover;

    private int currTick = 0;

    private ShulkerBoxBlockEntity cachedTest = null;
    private boolean testMode = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if(testMode) {
            if(storageBlocks.size() == 0 || cachedTest == null) {
                ClientWorld world = player.clientWorld;
                BlockPos pos = player.getBlockPos();

                rect = Rect.detectRect(pos, world);
                if (rect == null) {
                    MessageManager.sendMsg(Formatting.RED + "Could not find matching field to farm.");
                    return;
                }

                storageBlocks = StorageManager.getOuterBlocks(world, rect, Blocks.RED_SHULKER_BOX);
                BlockPos desired = storageBlocks.get(0);
                Optional<ShulkerBoxBlockEntity> entity = player.getWorld().getBlockEntity(desired, BlockEntityType.SHULKER_BOX);
                if(entity.isEmpty())
                    return;

                cachedTest = entity.get();
            }

            if(quickMover == null)
                quickMover = new QuickItemMover(player);

            MultipleReturnState state = quickMover.moveAll(Items.NETHER_WART, true, cachedTest, () -> {
                BlockPos pos = storageBlocks.get(0);

                BlockHitResult blockHit = new BlockHitResult(pos.toCenterPos(), Direction.EAST, pos, false);
                MinecraftClient.getInstance().interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHit);
            });

            if(state == MultipleReturnState.OK || state == MultipleReturnState.NO_SPACE_LEFT || state == MultipleReturnState.ITEM_NOT_FOUND || state == MultipleReturnState.SCREEN_NULL) {
                testMode = false;
            }

        }

        if (enabled)
            this.inner_tick();
    }

    public void test() { testMode = !testMode; }

    @Override
    public void inner_tick() {
        /*
        if (player.getHealth() < 5 || player.getVelocity().y > .5) {
            panic();
            return;
        }

        currTick++;
        if (currTick % 20 == 0)
            checkWarts();

        if (curr == null) {
            BlockPos pPos = player.getBlockPos();
            toMine.sort((a, b) -> (int) (pPos.getSquaredDistance(a) - pPos.getSquaredDistance(b)));
            if (toMine.isEmpty()) {
                state = FarmState.IDLE;
                return;
            }

            curr = toMine.get(0);
            state = FarmState.WALKING;
            MessageManager.sendMsgF(Formatting.YELLOW + "Walking to wart at %s", curr);
        }

        handleState();*/
    }

    private void lookAtYawOnly(Vec3d target) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        Vec3d pos = player.getPos();
        double d = target.x - pos.x;
        double f = target.z - pos.z;

        player.setYaw(MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875D) - 90.0F));
        player.setHeadYaw(player.getYaw());
        player.prevYaw = player.getYaw();
    }

    public void handleState() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        GameOptions options = MinecraftClient.getInstance().options;
        if(state == FarmState.IDLE) {
            return;
        }

        if (state == FarmState.WALKING) {
            options.forwardKey.setPressed(true);
            Vec3d currCenter = Vec3d.ofCenter(curr, 0);

            lookAtYawOnly(currCenter);
            BlockPos playerPos = player.getBlockPos().up();
            if (playerPos.compareTo(curr) != 0)
                return;

            options.forwardKey.setPressed(false);
            state = FarmState.MOVING_HOE;

        }

        if(state == FarmState.MOVING_HOE) {
            MessageManager.sendMsgF(Formatting.YELLOW + "Moving to hoe...");
            boolean hasHoe = singleMover.scrollToHoe(HOE_SLOT);
            if(!hasHoe)
                MainMod.LOGGER.info("Could not find hoe to use.");

            state = FarmState.BREAKING;
            MessageManager.sendMsgF(Formatting.YELLOW + "Breaking block...");
        }

        if (state == FarmState.BREAKING) {
            player.setPitch(90);

            options.attackKey.setPressed(true);
            if (!player.getWorld().isAir(curr))
                return;


            options.attackKey.setPressed(false);
            state = FarmState.MOVING_WARTS;
            MessageManager.sendMsgF(Formatting.YELLOW + "Moving warts to place...");
        }

        if(state == FarmState.MOVING_WARTS) {
            boolean canContinue = singleMover.tickScrollWart(WART_SLOT);
            if (!canContinue)
                return;

            MessageManager.sendMsgF(Formatting.YELLOW + "Placing warts...");
            state = FarmState.PLACING;
        }

        if (state == FarmState.PLACING) {
            player.setPitch(90);
            options.useKey.setPressed(true);

            if (player.getWorld().isAir(curr))
                return;

            options.useKey.setPressed(false);
            state = FarmState.CHECKING;
            MessageManager.sendMsgF(Formatting.YELLOW + "Checking inventory for space left...");
        }

        if(state == FarmState.CHECKING) {
            boolean shouldMove = singleMover.getSlotsFree() < MIN_SLOTS_FREE;
            if(!shouldMove) {
                next();
                return;
            }
        }
    }

    private void next() {
        toMine.remove(curr);
        curr = null;
        state = FarmState.IDLE;
    }

    public void checkWarts() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientWorld world = player.clientWorld;

        BlockPos start = rect.getLeft().up();
        BlockPos end = rect.getRight().up();

        int diffX = end.getX() - start.getX();
        int diffZ = end.getZ() - start.getZ();

        for (int x = 0; x < diffX; x++) {
            for (int z = 0; z < diffZ; z++) {
                BlockPos pos = start.add(x, 0, z);
                BlockState state = world.getBlockState(pos);

                if (!(state.getBlock() instanceof NetherWartBlock)) {
                    if(!errorBlocksReported.contains(pos) && !state.isAir()) {
                        errorBlocksReported.add(pos);
                        MessageManager.sendMsgF(Formatting.YELLOW + "Invalid State Block found %s at %s", state.getBlock().getTranslationKey(), pos);
                    }
                    continue;
                }

                int age = state.get(NetherWartBlock.AGE);
                boolean contains = toMine.contains(pos);

                if (age != MAX_WART_AGE) {
                    if (contains)
                        toMine.remove(pos);
                    continue;
                }

                if (!contains) {
                    MainMod.LOGGER.info("Grown Wart found at {}", pos);
                    toMine.add(pos);
                }
            }
        }
    }


    @Override
    public boolean start() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        singleMover = new SingleItemMover(player);
        quickMover = new QuickItemMover(player);

        ClientWorld world = player.clientWorld;
        BlockPos pos = player.getBlockPos();

        rect = Rect.detectRect(pos, world);
        if (rect == null) {
            MessageManager.sendMsg(Formatting.RED + "Could not find matching field to farm.");
            return false;
        }

        storageBlocks = StorageManager.getOuterBlocks(world, rect, Blocks.RED_SHULKER_BOX);
        MessageManager.sendMsg("Starting to farm...");
        enabled = true;
        return true;
    }

    @Override
    public boolean stop() {
        enabled = false;
        MessageManager.sendMsg("Alright boss, stopped farming!");
        return true;
    }

    public void panic() {
        MessageManager.sendMsg(Formatting.DARK_RED + "Detected Health under 5HP. Teleporting you home...");
        MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("/home");
    }
}
