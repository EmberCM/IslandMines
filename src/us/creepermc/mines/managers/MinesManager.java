package us.creepermc.mines.managers;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import us.creepermc.mines.Core;
import us.creepermc.mines.objects.Mine;
import us.creepermc.mines.objects.TempMine;
import us.creepermc.mines.objects.Upgrade;
import us.creepermc.mines.templates.XManager;
import us.creepermc.mines.utils.Files;
import us.creepermc.mines.utils.Util;

import java.util.*;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MinesManager extends XManager {
	final Map<UUID, TempMine> tempMines = new HashMap<>();
	final List<Mine> mines = new ArrayList<>();
	StorageManager storageManager;
	
	public MinesManager(Core core) {
		super(core);
	}
	
	@Override
	public void initialize() {
		deinitialize();
		
		storageManager = getCore().getManager(StorageManager.class);
		YamlConfiguration config = Files.getConfiguration(Files.getFile(getCore(), "mines.yml", true));
		if(config.isConfigurationSection("mines"))
			config.getConfigurationSection("mines").getKeys(false).forEach(id -> {
				String path = "mines." + id + ".";
				int size = config.getInt(path + "size");
				int height = config.getInt(path + "height");
				long lifeSpan = config.getLong(path + "lifespan") * 60000;
				int automaticReset = (int) config.getDouble(path + "automatic-reset") * 60;
				int resetDelay = (int) config.getDouble(path + "manual-reset-delay");
				List<String> signText = Util.color(config.getStringList(path + "sign-text"));
				List<String> hologramText = Util.color(config.getStringList(path + "hologram-text"));
				ItemStack item = Util.getItem(config, path + "item");
				List<Upgrade> upgrades = new ArrayList<>();
				if(config.isConfigurationSection(path + "upgrades"))
					config.getConfigurationSection(path + "upgrades").getKeys(false).forEach(uid -> {
						String upath = path + "upgrades." + uid + ".";
						double price = config.getDouble(upath + "price");
						int blocks = config.getInt(upath + "blocks");
						String[] udata = config.getString(upath + "fill").split(":");
						byte durability = udata.length > 1 && Util.isInt(udata[1]) ? (byte) Integer.parseInt(udata[1]) : 0;
						MaterialData data = new MaterialData(Util.getMaterial(udata[0]), durability);
						upgrades.add(new Upgrade(uid, price, blocks, data));
					});
				mines.add(new Mine(id, size, height, lifeSpan, automaticReset, resetDelay, signText, hologramText, item, upgrades));
			});
	}
	
	@Override
	public void deinitialize() {
		storageManager = null;
		tempMines.clear();
		mines.clear();
	}
	
	public Mine getMine(String id) {
		return mines.stream().filter(mine -> mine.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
	}
	
	public Mine getMine(ItemStack item) {
		return mines.stream().filter(mine -> Util.matches(mine.getItem(), item, true)).findFirst().orElse(null);
	}
	
	public Upgrade getUpgrade(Mine mine, String id) {
		return mine.getUpgrades().stream().filter(upgrade -> upgrade.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
	}
	
	public int getUpgradeIndex(Mine mine, Upgrade upgrade) {
		for(int i = 0; i < mine.getUpgrades().size(); i++) if(mine.getUpgrades().get(i).equals(upgrade)) return i;
		return -1;
	}
	
	public Location isLocationAvailable(Location location, Mine mine) {
		for(int x = 0; x < mine.getSize() + 2; x++)
			for(int y = 0; y < mine.getHeight() + 2; y++)
				for(int z = 0; z < mine.getSize() + 2; z++) {
					if(x == 0 && y == 0 && z == 0) continue;
					Location loc = location.clone().add(x, y, z);
					if(loc.getBlock().getType() != Material.AIR) return loc;
				}
		return null;
	}
	
	public void testPlace(Player player, Location location, Mine mine) {
		List<Location> locations = new ArrayList<>();
		int bigSize = mine.getSize() + 1;
		for(int x = 0; x <= bigSize; x++)
			for(int z = 0; z <= bigSize; z++)
				locations.add(location.clone().add(x, 0, z));
		for(int z = 0; z <= bigSize; z += bigSize)
			for(int x = 0; x <= bigSize; x++)
				for(int y = 1; y <= mine.getHeight() + 1; y++)
					locations.add(location.clone().add(x, y, z));
		for(int x = 0; x <= bigSize; x += bigSize)
			for(int z = 0; z <= bigSize; z++)
				for(int y = 1; y <= mine.getHeight() + 1; y++)
					locations.add(location.clone().add(x, y, z));
		locations.add(location.clone().add(0, mine.getHeight() + 2, 0));
		locations.add(location.clone().add(0, mine.getHeight() + 2, bigSize));
		locations.add(location.clone().add(bigSize, mine.getHeight() + 2, 0));
		locations.add(location.clone().add(bigSize, mine.getHeight() + 2, bigSize));
		locations.forEach(loc -> player.sendBlockChange(loc, Material.BEDROCK, (byte) 0));
		tempMines.put(player.getUniqueId(), new TempMine(mine, location, locations));
		getCore().sendMsg(player, "PLACED_MINE", mine.getPrettyId());
	}
	
	public void confirmPlace(Player player) {
		new BukkitRunnable() {
			@Override
			public void run() {
				TempMine tempMine = tempMines.remove(player.getUniqueId());
				tempMine.getLocations().forEach(loc -> loc.getBlock().setType(Material.BEDROCK));
				storageManager.createMine(tempMine.getMine(), player, tempMine.getLocation());
				getCore().sendMsg(player, "CONFIRMED", tempMine.getMine().getPrettyId());
			}
		}.runTask(getCore());
	}
}