package us.creepermc.mines.utils;

import net.minecraft.server.v1_8_R3.*;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import us.creepermc.mines.objects.BlockUpdate;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;

public class BlockUtil {
	public static void setBlockInNativeChunkSection(World world, int x, int y, int z, int blockId, byte data) {
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		Chunk nmsChunk = worldServer.getChunkAt(x >> 4, z >> 4);
		nmsChunk.f(true);
		nmsChunk.mustSave = true;
//		nmsChunk.setNeedsSaving(true);
		IBlockData ibd = Block.getByCombinedId(blockId + (data << 12));
		ChunkSection chunksection = nmsChunk.getSections()[y >> 4];
		if(chunksection == null) chunksection = nmsChunk.getSections()[y >> 4] = new ChunkSection(y >> 4 << 4, !(nmsChunk.getWorld()).worldProvider.o());
		chunksection.setType(x & 0xF, y & 0xF, z & 0xF, ibd);
//		BlockPosition bp = new BlockPosition(x, y, z);
//		worldServer.setTypeAndData(bp, ibd, 2);
		notify(world, x, y, z);
	}
	
	public static void setBlockInNativeChunkSection(BlockUpdate block) {
		if(block == null) return;
		setBlockInNativeChunkSection(block.getWorld(), block.getX(), block.getY(), block.getZ(), block.getId(), block.getData());
	}
	
	private static void notify(World world, int x, int y, int z) {
		try {
			WorldServer worldServer = ((CraftWorld) world).getHandle();
			PlayerChunkMap playerChunkMap = worldServer.getPlayerChunkMap();
			playerChunkMap.flagDirty(new BlockPosition(x, y, z));
//			ChunkProviderServer chunkProvider = worldServer.getChunkProvider();
//			chunkProvider.flagDirty(new BlockPosition(x, y, z));
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void queueBlockUpdates(BlockingDeque<BlockUpdate> blocks, int amountPerTick) {
		CompletableFuture.runAsync(() -> {
			int index = 0;
			while(!blocks.isEmpty()) {
				setBlockInNativeChunkSection(blocks.poll());
				if(++index == amountPerTick) index = 0;
				else continue;
				try {
					Thread.sleep(50);
				} catch(Exception exception) {
					exception.printStackTrace();
				}
			}
		});
	}
	
	public static void updateChunk(Player player, org.bukkit.Chunk chunk) {
		((CraftPlayer) player).getHandle().chunkCoordIntPairQueue.add(new ChunkCoordIntPair(chunk.getX(), chunk.getZ()));
	}
}