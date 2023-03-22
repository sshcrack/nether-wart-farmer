package me.sshcrack.netherwarts.mixin;


import me.sshcrack.netherwarts.MainMod;
import me.sshcrack.netherwarts.MessageManager;
import me.sshcrack.netherwarts.Rect;
import me.sshcrack.netherwarts.manager.FarmState;
import me.sshcrack.netherwarts.manager.GeneralTimerAccess;
import me.sshcrack.netherwarts.manager.KeyOverwrite;
import me.sshcrack.netherwarts.manager.inv.GeneralHelper;
import me.sshcrack.netherwarts.manager.inv.storage.StorageManager;
import me.sshcrack.netherwarts.manager.inv.multiple.QuickItemMover;
import me.sshcrack.netherwarts.manager.inv.single.SingleItemMover;
import me.sshcrack.netherwarts.manager.movement.MovementHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.FoodComponents;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GameRenderer.class)
public abstract class GeneralTimerMixin implements GeneralTimerAccess {
    @Shadow private long lastWindowFocusedTime;
    private static final int MAX_WART_AGE = 3;

    private static final int HOE_SLOT = 0;
    private static final int WART_SLOT = 8;
    private static  final int BREAD_SLOT = 1;

    private static final int TRIGGER_FOOD_LEVEL = 20 - FoodComponents.BREAD.getHunger();
    private static final int MIN_SLOTS_FREE = 4;

    private boolean enabled = false;
    private Pair<BlockPos, BlockPos> rect;

    private ClientPlayerEntity player;


    private final List<BlockPos> toMine = new ArrayList<>();
    private final List<BlockPos> errorBlocksReported = new ArrayList<>();

    private List<BlockPos> foodShulkers = new ArrayList<>();
    private BlockPos bed;
    private BlockPos prevPos;

    private BlockPos curr = null;
    private FarmState state = FarmState.WALKING;

    private SingleItemMover singleMover;
    private QuickItemMover quickMover;
    private MovementHandler movement;
    private StorageManager wartStorage;
    private ShulkerBoxBlockEntity cachedShulker;
    private GameOptions options;

    private int currTick = 0;

    @Inject(method="render", at=@At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;openPauseMenu(Z)V"), cancellable = true)
    private void onGamePause(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        if(enabled) {
            ci.cancel();
            lastWindowFocusedTime = Util.getMeasuringTimeMs();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (enabled)
            this.innerTick();
    }

    @Override
    public void innerTick() {
        if (player.getHealth() < 5 || player.getVelocity().y > .5) {
            panic();
            return;
        }

        if(!wartStorage.isInitialized() && !wartStorage.initializeTick())
            return;

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

        handleState();
    }

    public void handleState() {
        if(state == FarmState.IDLE) {
            return;
        }

        if (state == FarmState.WALKING) {
            if (!movement.walk(curr))
                return;

            state = FarmState.MOVING_HOE;
            MessageManager.debugMsg(Formatting.YELLOW + "Moving to hoe...");
        }

        if(state == FarmState.MOVING_HOE) {
            if(!singleMover.scrollToHoe(HOE_SLOT))
                return;

            state = FarmState.BREAKING;
            MessageManager.debugMsgF(Formatting.YELLOW + "Breaking block at %s...", curr);
        }

        if (state == FarmState.BREAKING) {
            player.setPitch(90);

            KeyOverwrite.press(options.attackKey);
            if (!player.getWorld().isAir(curr))
                return;


            KeyOverwrite.unset(options.attackKey);
            state = FarmState.MOVING_WARTS;
            MessageManager.debugMsg(Formatting.YELLOW + "Moving warts to place...");
        }

        if(state == FarmState.MOVING_WARTS) {
            boolean canContinue = singleMover.tickScrollWart(WART_SLOT);
            if (!canContinue)
                return;

            MessageManager.debugMsg(Formatting.YELLOW + "Placing warts...");
            state = FarmState.PLACING;
        }

        if (state == FarmState.PLACING) {
            player.setPitch(90);
            KeyOverwrite.press(options.useKey);

            if (player.getWorld().isAir(curr))
                return;

            KeyOverwrite.unset(options.useKey);
            state = FarmState.CHECKING;
            currTick = 1;
            MessageManager.debugMsg(Formatting.YELLOW + "Checking inventory for space left...");
        }

        if(state == FarmState.CHECKING) {
            currTick++;
            cachedShulker = MinecraftClient.getInstance().world.getBlockEntity(foodShulkers.get(0), BlockEntityType.SHULKER_BOX).get();

            boolean shouldEat = player.getHungerManager().getFoodLevel() < TRIGGER_FOOD_LEVEL;
            boolean shouldMove = singleMover.getSlotsFree() < MIN_SLOTS_FREE;
            boolean shouldSleep = GeneralHelper.canSleep();
            if(shouldMove) {
                MessageManager.debugMsg(Formatting.YELLOW + "Moving items...");
                state = FarmState.FULL_INV;
            } else if(shouldEat) {
                MessageManager.debugMsg(Formatting.BLUE + "Eating (Going to shulker)...");
                if(player.getInventory().contains(Items.BREAD.getDefaultStack())) {
                    state = FarmState.EAT_TO_HOTBAR;
                } else {
                    state = FarmState.EAT_QUICK_MOVE;
                }
            } else if(shouldSleep) {
               MessageManager.debugMsg(Formatting.BLUE + "Sleeping...");
               state = FarmState.GOTO_BED;
            }  else {
                if(currTick % 10 == 0) {
                    next();
                }
            }
            return;
        }

        if(state == FarmState.FULL_INV) {
            if(!wartStorage.store(Items.NETHER_WART, true))
                return;

            state = FarmState.CHECKING;
        }

        tickEat();
        tickSleep();
    }

    private void tickSleep() {
        if(state == FarmState.GOTO_BED) {
            if(!movement.walk(bed,p -> bed.getSquaredDistance(p) <= 2))
                return;

            MessageManager.sendMsg(Formatting.BLUE + "Using bed...");
            prevPos = player.getBlockPos();
            state = FarmState.SLEEP;
        }

        if(state == FarmState.SLEEP) {
            MessageManager.sendMsg("Sleeping...");
            GeneralHelper.interactBlock(bed);
            state = FarmState.WAIT_SLEEP;
        }

        if(state == FarmState.WAIT_SLEEP) {
            if(player.isSleeping())
                return;

            state = FarmState.GO_BACK_SLEEP;
        }

        if(state == FarmState.GO_BACK_SLEEP) {
            if(!movement.walk(prevPos,p -> prevPos.getSquaredDistance(p) <= 2))
                return;

            state = FarmState.CHECKING;
        }
    }

    private void tickEat() {
        if(state == FarmState.EAT_GOTO) {
            if(!movement.walk(foodShulkers.get(0),p -> foodShulkers.get(0).getSquaredDistance(p) <= 2))
                return;

            MessageManager.sendMsg(Formatting.BLUE + "Quick moving bread...");
            state = FarmState.EAT_QUICK_MOVE;
        }

        if(state == FarmState.EAT_QUICK_MOVE) {
            if(!quickMover.moveSingleBool(Items.BREAD, false, cachedShulker))
                return;

            MessageManager.sendMsg(Formatting.BLUE + "Moving bread to hotbar...");
            state = FarmState.EAT_TO_HOTBAR;
        }

        if(state == FarmState.EAT_TO_HOTBAR) {
            if(!singleMover.moveBreadTick(BREAD_SLOT))
                return;

            MessageManager.sendMsg(Formatting.BLUE + "Eating bread...");
            state = FarmState.EAT;
        }

        if(state == FarmState.EAT) {
            KeyOverwrite.press(options.useKey);

            if(player.getHungerManager().isNotFull())
                return;

            KeyOverwrite.unset(options.useKey);
            state = FarmState.CHECKING;
        }
    }

    private void next() {
        MessageManager.sendMsg(Formatting.LIGHT_PURPLE +  "Ready for next wart.");
        toMine.remove(curr);
        curr = null;
        state = FarmState.IDLE;
    }

    public void checkWarts() {
        ClientWorld world = player.clientWorld;

        BlockPos start = rect.getLeft().up();
        BlockPos end = rect.getRight().up();

        int diffX = end.getX() - start.getX();
        int diffZ = end.getZ() - start.getZ();

        for (int x = 0; x <= diffX; x++) {
            for (int z = 0; z <= diffZ; z++) {
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
                    MessageManager.debugMsgF(Formatting.GREEN +  "Grown Wart found at %s", pos);
                    MainMod.LOGGER.info("Grown Wart found at {}", pos);
                    toMine.add(pos);
                }
            }
        }
    }


    @Override
    public boolean start() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        this.player = player;

        assert player != null;
        singleMover = new SingleItemMover(player);
        quickMover = new QuickItemMover(player);
        movement = new MovementHandler(player);
        options = MinecraftClient.getInstance().options;

        ClientWorld world = player.clientWorld;
        BlockPos pos = player.getBlockPos();

        rect = Rect.detectRect(pos, world);
        if (rect == null) {
            MessageManager.sendMsg(Formatting.RED + "Could not find matching field to farm.");
            return false;
        }

        List<BlockPos> storageBlocks = StorageManager.getOuterBlocks(world, rect, Blocks.WHITE_SHULKER_BOX);
        foodShulkers = StorageManager.getOuterBlocks(world, rect, Blocks.LIGHT_GRAY_SHULKER_BOX);
        List<BlockPos> beds = StorageManager.getOuterBlocks(world, rect, Blocks.WHITE_BED);
        if(beds.size() == 0) {
            MessageManager.sendMsg(Formatting.RED + "Could not find white bed.");
            return false;
        }

        if(foodShulkers.size() == 0){
            MessageManager.sendMsgF(Formatting.RED + "Could not find food shulker (light gray shulker box)");
            return false;
        }

        if(storageBlocks.size() == 0){
            MessageManager.sendMsgF(Formatting.RED + "Could not find storage shulker (white shulker box)");
            return false;
        }

        bed = beds.get(0);


        wartStorage = new StorageManager(player, storageBlocks);

        MessageManager.sendMsg("Starting to farm...");
        state = FarmState.IDLE;
        curr = null;
        enabled = true;
        return true;
    }

    @Override
    public boolean stop() {
        KeyOverwrite.reset();

        enabled = false;
        MessageManager.sendMsg("Alright boss, stopped farming!");
        return true;
    }

    public void panic() {
        MessageManager.sendMsg(Formatting.DARK_RED + "Detected Health under 5HP. Teleporting you home...");
        MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("/home");

        stop();
    }
}
