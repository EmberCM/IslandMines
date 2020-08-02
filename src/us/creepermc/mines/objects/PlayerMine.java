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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
		reset();
	}
	
	private void createSigns(Core core) {
		Block block1 = placed.clone().add(0, mine.getHeight() + 2, 1).getBlock();
		block1.setType(Material.WALL_SIGN);
		block1.setData((byte) 3);
		Block block2 = placed.clone().add(1, mine.getHeight() + 2, 0).getBlock();
		block2.setType(Material.WALL_SIGN);
		block2.setData((byte) 5);
		Block block3 = placed.clone().add(mine.getSize(), mine.getHeight() + 2, 0).getBlock();
		block3.setType(Material.WALL_SIGN);
		block3.setData((byte) 4);
		Block block4 = placed.clone().add(mine.getSize() + 1, mine.getHeight() + 2, 1).getBlock();
		block4.setType(Material.WALL_SIGN);
		block4.setData((byte) 3);
		Block block5 = placed.clone().add(mine.getSize() + 1, mine.getHeight() + 2, mine.getSize()).getBlock();
		block5.setType(Material.WALL_SIGN);
		Block block6 = placed.clone().add(mine.getSize(), mine.getHeight() + 2, mine.getSize() + 1).getBlock();
		block6.setType(Material.WALL_SIGN);
		block6.setData((byte) 4);
		Block block7 = placed.clone().add(1, mine.getHeight() + 2, mine.getSize() + 1).getBlock();
		block7.setType(Material.WALL_SIGN);
		block7.setData((byte) 5);
		Block block8 = placed.clone().add(0, mine.getHeight() + 2, mine.getSize()).getBlock();
		block8.setType(Material.WALL_SIGN);
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
	
	public boolean isInMine(Location location) {
		return location.getWorld().equals(placed.getWorld())
				&& location.getBlockX() >= placed.getBlockX() && placed.getBlockX() + mine.getSize() + 1 >= location.getBlockX()
				&& location.getBlockY() >= placed.getBlockY() && placed.getBlockY() + mine.getHeight() + 2 >= location.getBlockY()
				&& location.getBlockZ() >= placed.getBlockZ() && placed.getBlockZ() + mine.getSize() + 1 >= location.getBlockZ();
	}
	
	public void reset(boolean setTime) {
		if(setTime) lastReset = System.currentTimeMillis();
		for(int x = 1; x <= mine.getSize(); x++)
			for(int z = 1; z <= mine.getSize(); z++)
				for(int y = 1; y <= mine.getHeight(); y++) {
					Block block;
					try {
						block = placed.clone().add(x, y, z).getBlock();
					} catch(NullPointerException ex) {
						continue;
					}
					if(block == null || block.getType() == upgrade.getData().getItemType()) continue;
					block.setType(upgrade.getData().getItemType());
					block.setData(upgrade.getData().getData());
				}
		Location teleport = getCenterLocation();
		Bukkit.getServer().getOnlinePlayers().stream().filter(player -> isInMine(player.getLocation())).forEach(player -> player.teleport(teleport));
	}
	
	public void reset() {
		reset(false);
	}
	
	public void clear() {
		for(int x = 0; x <= mine.getSize() + 1; x++)
			for(int z = 0; z <= mine.getSize() + 1; z++)
				for(int y = 0; y <= mine.getHeight() + 2; y++)
					placed.clone().add(x, y, z).getBlock().setType(Material.AIR, false);
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
	
	public void addStorage(MaterialData data) {
		storage.put(data, storage.getOrDefault(data, 0) + 1);
	}
	
	public void upgrade(Upgrade upgrade) {
		progress = 0;
		this.upgrade = upgrade;
	}
	
	public boolean isPastLifetime() {
		return System.currentTimeMillis() >= timePlaced + mine.getLifeSpan();
	}
}