package me.sshcrack.netherwarts.manager.inv.storage;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StorageInfo {
    private final BlockPos pos;
    private List<ItemStack> items;
    private final ShulkerBoxBlockEntity entity;

    public StorageInfo(BlockPos pos, List<ItemStack> items, ShulkerBoxBlockEntity entity) {
        this.pos = pos;
        this.items = items;
        this.entity = entity;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void setItems(List<ItemStack> items) {
        this.items = items;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public int getSlotsFree() {
        int left = 0;
        for (ItemStack item : items) {
            if(item.isEmpty())
                left++;
        }

        return left;
    }

    public ShulkerBoxBlockEntity getEntity() {
        return entity;
    }

    @Nullable
    public static StorageInfo fromScreenHandler(BlockPos pos, ScreenHandler handler) {
        ClientWorld world = MinecraftClient.getInstance().world;
        assert world != null;

        Optional<ShulkerBoxBlockEntity> opt = world.getBlockEntity(pos, BlockEntityType.SHULKER_BOX);
        if(opt.isEmpty())
            return null;

        ShulkerBoxBlockEntity entity = opt.get();
        return new StorageInfo(pos, handler.getStacks().subList(0, ShulkerBoxBlockEntity.INVENTORY_SIZE), entity);
    }
}
