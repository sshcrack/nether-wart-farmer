package me.sshcrack.netherwarts.manager.inv;

import me.sshcrack.netherwarts.Rect;
import net.fabricmc.loader.impl.lib.sat4j.core.VecInt;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

public class StorageManager {
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
}
