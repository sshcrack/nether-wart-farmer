package me.sshcrack.netherwarts;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Rect {
    public static final List<Direction> DIRECTIONS = Arrays.asList(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH);

    @Nullable
    public static Pair<BlockPos, BlockPos> detectRect(BlockPos startPoint, ClientWorld world) {
        BlockPos start = startPoint;
        BlockPos end = startPoint;
        List<Long> visited = new ArrayList<>();

        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPoint);


        int maxIterations = 100000;
        int i = 0;
        while (!queue.isEmpty() && i < maxIterations) {
            BlockPos nextBlock = queue.poll();
            for (Direction direction : DIRECTIONS) {
                BlockPos inDir = nextBlock.offset(direction);
                BlockState state = world.getBlockState(inDir);

                if(state.isAir())
                    continue;

                if(inDir.compareTo(start) < 0)
                    start = inDir;

                if(inDir.compareTo(end) > 0)
                    end = inDir;

                if(!visited.contains(inDir.asLong())) {
                    queue.add(inDir);
                    visited.add(inDir.asLong());
                }
            }

            i++;
        }

        int biggestX = Math.max(start.getX(), end.getX());
        int biggestZ = Math.max(start.getZ(), end.getZ());

        int smallestX = Math.min(start.getX(), end.getX());
        int smallestZ = Math.min(start.getZ(), end.getZ());

        BlockPos nStart = new BlockPos(smallestX, start.getY(), smallestZ);
        BlockPos nEnd = new BlockPos(biggestX, end.getY(), biggestZ);
        return new Pair<>(nStart, nEnd);
    }
}
