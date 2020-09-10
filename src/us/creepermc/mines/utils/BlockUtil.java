package us.creepermc.mines.utils;

import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class BlockUtil {
	public static void setBlockInNativeChunkSection(World world, int x, int y, int z, int blockId, byte data) {
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		Chunk nmsChunk = worldServer.getChunkAt(x >> 4, z >> 4);
		IBlockData ibd = Block.getByCombinedId(blockId + (data << 12));
		ChunkSection chunksection = nmsChunk.getSections()[y >> 4];
		if(chunksection == null) chunksection = nmsChunk.getSections()[y >> 4] = new ChunkSection(y >> 4 << 4, !(nmsChunk.getWorld()).worldProvider.o());
		chunksection.setType(x & 0xF, y & 0xF, z & 0xF, ibd);
		notify(world, x, y, z);
	}
	
	private static void notify(World world, int x, int y, int z) {
		try {
			WorldServer worldServer = ((CraftWorld) world).getHandle();
			PlayerChunkMap playerChunkMap = worldServer.getPlayerChunkMap();
			playerChunkMap.flagDirty(new BlockPosition(x, y, z));
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void sendBlockChange(Player player, Location loc, int blockId, byte data) {
		if(((CraftPlayer) player).getHandle().playerConnection == null) return;
		PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(((CraftWorld) loc.getWorld()).getHandle(), new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
		packet.block = Block.getByCombinedId(blockId + (data << 12));
		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
	}
}