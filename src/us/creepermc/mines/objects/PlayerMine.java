package us.creepermc.mines.objects;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import us.creepermc.mines.Core;
import us.creepermc.mines.managers.MinesManager;
import us.creepermc.mines.utils.BlockUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerMine {
	UUID owner;
	Mine mine;
	Location placed;
	@NonFinal
	Upgrade upgrade;
	@NonFinal
	int progress;
	Map<MaterialData, Integer> storage;
	long timePlaced;
	@NonFinal
	long lastReset;
	@NonFinal
	boolean removed;
	
	public PlayerMine(UUID owner, Mine mine, Location placed) {
		this.owner = owner;
		this.mine = mine;
		this.placed = placed;
		this.upgrade = mine.getUpgrades().get(0);
		this.progress = 0;
		this.storage = new HashMap<>();
		this.timePlaced = System.currentTimeMillis();
		this.lastReset = 0;
	}
	
	public void initialize(Core core) {
		createSigns(core);
		reset(core.getManager(MinesManager.class).getBlocksPerTick());
	}
	
	private void createSigns(Core core) {
		new BukkitRunnable() {
			@Override
			public void run() {
				int wallId = Material.WALL_SIGN.getId();
				Block block1 = placed.clone().add(0, mine.getHeight() + 2, 1).getBlock();
				BlockUtil.setBlockInNativeChunkSection(block1.getWorld(), block1.getX(), block1.getY(), block1.getZ(), wallId, (byte) 3);
				Block block2 = placed.clone().add(1, mine.getHeight() + 2, 0).getBlock();
				BlockUtil.setBlockInNativeChunkSection(block2.getWorld(), block2.getX(), block2.getY(), block2.getZ(), wallId, (byte) 5);
				Block block3 = placed.clone().add(mine.getSize(), mine.getHeight() + 2, 0).getBlock();
				BlockUtil.setBlockInNativeChunkSection(block3.getWorld(), block3.getX(), block3.getY(), block3.getZ(), wallId, (byte) 4);
				Block block4 = placed.clone().add(mine.getSize() + 1, mine.getHeight() + 2, 1).getBlock();
				BlockUtil.setBlockInNativeChunkSection(block4.getWorld(), block4.getX(), block4.getY(), block4.getZ(), wallId, (byte) 3);
				Block block5 = placed.clone().add(mine.getSize() + 1, mine.getHeight() + 2, mine.getSize()).getBlock();
				BlockUtil.setBlockInNativeChunkSection(block5.getWorld(), block5.getX(), block5.getY(), block5.getZ(), wallId, (byte) 0);
				Block block6 = placed.clone().add(mine.getSize(), mine.getHeight() + 2, mine.getSize() + 1).getBlock();
				BlockUtil.setBlockInNativeChunkSection(block6.getWorld(), block6.getX(), block6.getY(), block6.getZ(), wallId, (byte) 4);
				Block block7 = placed.clone().add(1, mine.getHeight() + 2, mine.getSize() + 1).getBlock();
				BlockUtil.setBlockInNativeChunkSection(block7.getWorld(), block7.getX(), block7.getY(), block7.getZ(), wallId, (byte) 5);
				Block block8 = placed.clone().add(0, mine.getHeight() + 2, mine.getSize()).getBlock();
				BlockUtil.setBlockInNativeChunkSection(block8.getWorld(), block8.getX(), block8.getY(), block8.getZ(), wallId, (byte) 0);
				new BukkitRunnable() {
					@Override
					public void run() {
						Arrays.asList(block1, block2, block3, block4, block5, block6, block7, block8).forEach(block -> {
							Sign sign = (Sign) block.getState();
							for(int i = 0; i < mine.getSignText().size(); i++) sign.setLine(i, mine.getSignText().get(i));
							sign.update();
						});
					}
				}.runTask(core);
			}
		}.runTask(core);
	}
	
	public boolean isInMine(Location location) {
		return location.getWorld().equals(placed.getWorld())
				&& location.getBlockX() >= placed.getBlockX() && placed.getBlockX() + mine.getSize() + 1 >= location.getBlockX()
				&& location.getBlockY() >= placed.getBlockY() && placed.getBlockY() + mine.getHeight() + 2 >= location.getBlockY()
				&& location.getBlockZ() >= placed.getBlockZ() && placed.getBlockZ() + mine.getSize() + 1 >= location.getBlockZ();
	}
	
	public void reset(int blocksPerTick, boolean setTime) {
		if(removed) return;
		if(setTime) lastReset = System.currentTimeMillis();
		CompletableFuture.runAsync(() -> {
			Queue<BlockUpdate> blocks = new LinkedList<>();
			for(int y = 1; y <= mine.getHeight(); y++)
				for(int x = 1; x <= mine.getSize(); x++)
					for(int z = 1; z <= mine.getSize(); z++) {
						Location loc = placed.clone().add(x, y, z);
						if(loc == null) continue;
						blocks.add(new BlockUpdate(loc, upgrade.getData().getItemTypeId(), upgrade.getData().getData()));
					}
			BlockUtil.queueBlockUpdates(blocks, blocksPerTick);
			try {
				Thread.sleep(blocks.size() / blocksPerTick * 50);
			} catch(InterruptedException ignored) {
			}
			Location teleport = getCenterLocation();
			Bukkit.getServer().getOnlinePlayers().stream().filter(player -> isInMine(player.getLocation())).forEach(player -> player.teleport(teleport));
		});
	}
	
	public void reset(int blocksPerTick) {
		reset(blocksPerTick, false);
	}
	
	public void clear() {
		CompletableFuture.runAsync(() -> {
			for(int x = 0; x <= mine.getSize() + 1; x++)
				for(int z = 0; z <= mine.getSize() + 1; z++)
					for(int y = 0; y <= mine.getHeight() + 1; y++) {
						Location loc = placed.clone().add(x, y, z);
						BlockUtil.setBlockInNativeChunkSection(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 0, (byte) 0);
					}
		});
		for(int x = 0; x <= mine.getSize() + 1; x++)
			for(int z = 0; z <= mine.getSize() + 1; z++) {
				Location loc = placed.clone().add(x, mine.getHeight() + 2, z);
				loc.getBlock().setType(Material.AIR);
			}
		removed = true;
	}
	
	public Location getCenterLocation() {
		return placed.clone().add(mine.getSize() / 2.0 + 1, mine.getHeight() + 4, mine.getSize() / 2.0 + 1);
	}
	
	public boolean isInCooldown() {
		return getCooldown() != 0;
	}
	
	public long getCooldown() {
		long time = lastReset + mine.getResetDelay() * 60000 - System.currentTimeMillis();
		return Math.max(0, time);
	}
	
	public void addProgress() {
		progress++;
	}
	
	public void addStorage(MaterialData data, int amount) {
		storage.put(data, storage.getOrDefault(data, 0) + amount);
	}
	
	public void upgrade(Upgrade upgrade) {
		progress = 0;
		this.upgrade = upgrade;
	}
	
	public boolean isPastLifetime() {
		return removed || System.currentTimeMillis() >= timePlaced + mine.getLifeSpan();
	}
	
	public long getLifeLeft() {
		return timePlaced + mine.getLifeSpan() - System.currentTimeMillis();
	}
	
	public boolean canRemove() {
		return !isPastLifetime() && timePlaced + mine.getRemoveSafety() > System.currentTimeMillis();
	}
}