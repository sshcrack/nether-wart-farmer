package me.sshcrack.netherwarts.manager.inv.storage;

import me.sshcrack.netherwarts.MessageManager;
import me.sshcrack.netherwarts.Rect;
import me.sshcrack.netherwarts.manager.GeneralHandler;
import me.sshcrack.netherwarts.manager.inv.baic.InventoryManager;
import me.sshcrack.netherwarts.manager.inv.multiple.QuickItemMover;
import me.sshcrack.netherwarts.manager.movement.MovementHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StorageManager extends GeneralHandler {
    private State state = State.INITIALIZING;
    private final QuickItemMover quick;
    private final MovementHandler movement;
    private final List<StorageInfo> allShulkers = new ArrayList<>();
    private int currShulker;

    private InitState initState = InitState.WALKING;
    private int currTick = 1;
    private final List<BlockPos> unloadedShulkers;

    private boolean initialized = false;

    public StorageManager(ClientPlayerEntity player, List<BlockPos> shulkers) {
        super(player);
        movement = new MovementHandler(player);
        quick = new QuickItemMover(player);
        this.unloadedShulkers = shulkers;
    }

    public int getEmptyShulker() {
        allShulkers.sort(Comparator.comparingInt(StorageInfo::getSlotsFree).reversed());
        for (int i = 0; i < allShulkers.size(); i++) {
            StorageInfo info = allShulkers.get(i);
            if(info.getSlotsFree() != 0)
                return i;
        }

        return -1;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean initializeTick() {
        if(initState == InitState.END) {
            initialized = true;
            return true;
        }

        if(unloadedShulkers.size() == 0) {
            initState = InitState.END;
            return false;
        }
        BlockPos curr = unloadedShulkers.get(0);

        if(initState == InitState.WALKING) {
            if(!movement.walk(curr, p -> p.getSquaredDistance(curr) <= 3))
                return false;

            initState = InitState.OPENING;
            currTick = 1;
        } else if(initState == InitState.OPENING) {
            currTick++;
            if(currTick % 3 == 0) {
                Optional<ShulkerBoxBlockEntity> opt = player.clientWorld.getBlockEntity(curr, BlockEntityType.SHULKER_BOX);
                if(opt.isEmpty()) {
                    MessageManager.sendMsgF(Formatting.RED + "Block Entity is not shulker box at %s", curr);
                    initState = InitState.END;
                    return false;
                }

                ShulkerBoxBlockEntity entity = opt.get();
                BlockPos pos = entity.getPos();

                BlockHitResult blockHit = new BlockHitResult(pos.toCenterPos(), Direction.EAST, pos, false);
                MinecraftClient.getInstance().interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHit);

                initState = InitState.CHECK;
                currTick = 1;
            }
            return false;
        } else if(initState == InitState.CHECK) {
            currTick++;
            if(currTick % 10 == 0) {
                StorageInfo info = StorageInfo.fromScreenHandler(curr, player.currentScreenHandler);
                allShulkers.add(info);
                unloadedShulkers.remove(curr);

                player.clientWorld.getBlockEntity(curr, BlockEntityType.SHULKER_BOX).get().onClose(player);
                player.currentScreenHandler.close(player);
                MinecraftClient.getInstance().setScreen(null);
                initState = InitState.WALKING;
            }
        }

        return false;
    }

    private static List<BlockPos> getOutline(Pair<BlockPos, BlockPos> rect) {
        List<BlockPos> list = new ArrayList<>();
        BlockPos start = rect.getLeft();
        BlockPos end = rect.getRight();

        int diffX = end.getX() - start.getX();
        int diffZ = end.getZ() - start.getZ();

        for(int x = 0; x < diffX; x++) {
            list.add(start.add(x, 0, 0));
            list.add(end.subtract(new Vec3i(x, 0, 0)));
        }


        for(int z = 0; z < diffZ; z++) {
            list.add(start.add(0, 0, z));
            list.add(end.subtract(new Vec3i(0, 0, z)));
        }

        return list;
    }

    public static List<BlockPos> getOuterBlocks(ClientWorld world, Pair<BlockPos, BlockPos> rect, Block kind) {
        List<BlockPos> blocks = new ArrayList<>();

        List<BlockPos> outline = getOutline(rect);
        for (BlockPos pos : outline) {
            BlockPos outer = null;
            for (Direction dir : Rect.DIRECTIONS) {
                BlockPos inDir = pos.offset(dir);
                if(!world.getBlockState(inDir).isAir())
                    continue;

                outer = inDir;
                break;
            }

            if(outer == null)
                continue;

            BlockState state = world.getBlockState(outer.up());
            if(!state.isOf(kind))
                continue;

            blocks.add(outer.up());
        }

        return blocks;
    }

    public boolean store(Item kind, boolean playerToChest) {
        if(allShulkers.size() == 0)
            return true;

        if(state == State.INITIALIZING) {
            currShulker = getEmptyShulker();
            if(currShulker == -1) {
                MessageManager.sendMsgF(Formatting.DARK_RED + "Could not find empty shulker.");
                return true;
            }

            state = State.WALKING;
            MessageManager.sendMsgF(Formatting.YELLOW + "Walking towards shulker at %s...", allShulkers.get(currShulker).getPos());

            return false;
        }

        StorageInfo info = allShulkers.get(currShulker);
        BlockPos pos = info.getPos();
        ShulkerBoxBlockEntity entity = info.getEntity();

        if(state == State.WALKING) {
            if(!movement.walk(pos, p -> pos.getSquaredDistance(p) <= 2))
                return false;

            state = State.STORING;
            MessageManager.sendMsg(Formatting.YELLOW + "Storing items...");
        } else if(state == State.STORING) {
            if(!quick.moveAllBool(kind, playerToChest, entity, () -> allShulkers.set(currShulker, StorageInfo.fromScreenHandler(pos, player.currentScreenHandler))))
                return false;

            int slotsWithItem = InventoryManager.getSlotsWithItem(player.getInventory(), kind);
            state = State.INITIALIZING;

            if(slotsWithItem > 1) {
                MessageManager.sendMsgF(Formatting.AQUA+ "Starting all over again (slotsWithItem %s)...", slotsWithItem);
                return false;
            }

            MessageManager.sendMsgF(Formatting.GRAY + "Done storing.");
            return true;
        }

        return false;
    }

    enum State {
        INITIALIZING,
        WALKING,
        STORING
    }

    enum InitState {
        WALKING,
        OPENING,
        CHECK,
        END
    }
}
