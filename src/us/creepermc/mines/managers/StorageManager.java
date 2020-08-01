package us.creepermc.mines.managers;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import us.creepermc.mines.Core;
import us.creepermc.mines.objects.Mine;
import us.creepermc.mines.objects.PlayerMine;
import us.creepermc.mines.objects.Upgrade;
import us.creepermc.mines.templates.XManager;
import us.creepermc.mines.utils.Files;
import us.creepermc.mines.utils.Util;

import java.io.FileReader;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class StorageManager extends XManager {
	final List<PlayerMine> mines = new ArrayList<>();
	MinesManager minesManager;
	WorthManager worthManager;
	int resetTask;
	int saveTask;
	
	public StorageManager(Core core) {
		super(core);
	}
	
	@Override
	public void initialize() {
		if(!mines.isEmpty()) deinitialize();
		
		minesManager = getCore().getManager(MinesManager.class);
		worthManager = getCore().getManager(WorthManager.class);
		loadJSON("mines.json");
		saveTask = new BukkitRunnable() {
			@Override
			public void run() {
				saveJSON("mines.json");
			}
		}.runTaskTimerAsynchronously(getCore(), 36000, 36000 /* 30 minutes */).getTaskId();
		resetTask = new BukkitRunnable() {
			int index = 0;
			
			@Override
			public void run() {
				List<PlayerMine> list = mines.stream()
						.filter(mine -> index % mine.getMine().getAutomaticReset() == 0 && mine.getPlaced().getChunk().isLoaded())
						.collect(Collectors.toList());
				if(!list.isEmpty())
					new BukkitRunnable() {
						@Override
						public void run() {
							list.forEach(PlayerMine::reset);
						}
					}.runTask(getCore());
				index++;
			}
		}.runTaskTimerAsynchronously(getCore(), 1200, 1200).getTaskId();
	}
	
	@Override
	public void deinitialize() {
		if(resetTask != 0) getCore().getServer().getScheduler().cancelTask(resetTask);
		if(saveTask != 0) getCore().getServer().getScheduler().cancelTask(saveTask);
		saveJSON("mines.json");
		minesManager = null;
		worthManager = null;
		mines.clear();
		resetTask = 0;
		saveTask = 0;
	}
	
	public void createMine(Mine mine, Player player, Location location) {
		PlayerMine pmine = new PlayerMine(player.getUniqueId(), mine, location, mine.getUpgrades().get(0), 0, new HashMap<>(), System.currentTimeMillis(), mine.getLifeSpan(), 0L);
		pmine.createSigns(getCore());
		mines.add(pmine);
		pmine.reset();
	}
	
	public void deleteMine(PlayerMine mine, Player player) {
		mine.clear();
		mines.remove(mine);
		getCore().sendMsg(player, "DELETED_MINE", mine.getMine().getPrettyId());
		if(player.getInventory().firstEmpty() != -1) player.getInventory().addItem(mine.getMine().getItem());
		else {
			player.getWorld().dropItemNaturally(player.getLocation(), mine.getMine().getItem());
			getCore().sendMsg(player, "FULL_INVENTORY");
		}
	}
	
	public PlayerMine getMine(Location location) {
		for(PlayerMine mine : mines) if(mine.isInMine(location)) return mine;
		return null;
	}
	
	public PlayerMine getMine(Player player) {
		try {
			return getMine(Util.stringToLocation(player.getMetadata("islandmines_mine").get(0).asString()));
		} catch(Exception ex) {
			return null;
		}
	}
	
	public double getWorth(PlayerMine mine, Player player, MaterialData data) {
		if(!mine.getStorage().containsKey(data)) return 0;
		int amount = mine.getStorage().get(data);
		return worthManager.getWorth(player, data) * amount;
	}
	
	public double getWorth(PlayerMine mine, Player player) {
		if(mine.getStorage().isEmpty()) return 0;
		double total = 0;
		for(MaterialData data : mine.getStorage().keySet()) total += getWorth(mine, player, data);
		return total;
	}
	
	private void loadJSON(String file) {
		try {
			JSONObject json = (JSONObject) new JSONParser().parse(new FileReader(Files.getFile(getCore(), file, true)));
			((HashMap<String, HashMap<String, Object>>) json).forEach((k, v) -> {
				UUID owner = UUID.fromString(v.get("OWNER").toString());
				Mine mine = minesManager.getMine(v.get("MINE").toString());
				Location placed = Util.stringToLocation(v.get("PLACED").toString());
				Upgrade upgrade = minesManager.getUpgrade(mine, v.get("UPGRADE").toString());
				int progress = Long.valueOf(v.get("PROGRESS").toString()).intValue();
				Map<MaterialData, Integer> storage = new HashMap<>();
				((HashMap<String, Long>) v.get("STORAGE")).forEach((k2, v2) -> {
					String[] split = k2.split(":");
					storage.put(new MaterialData(Material.valueOf(split[0]), (byte) Integer.parseInt(split[1])), v2.intValue());
				});
				long timePlaced = Long.parseLong(v.get("TIME_PLACED").toString());
				long lifeSpan = Long.parseLong(v.get("LIFE_SPAN").toString());
				mines.add(new PlayerMine(owner, mine, placed, upgrade, progress, storage, timePlaced, lifeSpan, 0));
			});
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void saveJSON(String file) {
		JSONObject all = new JSONObject();
		mines.forEach(mine -> {
			JSONObject object = new JSONObject();
			object.put("OWNER", mine.getOwner().toString());
			object.put("MINE", mine.getMine().getId());
			object.put("PLACED", Util.simpleLocationToString(mine.getPlaced()));
			object.put("UPGRADE", mine.getUpgrade().getId());
			object.put("PROGRESS", mine.getProgress());
			JSONObject storage = new JSONObject();
			mine.getStorage().forEach((k, v) -> storage.put(k.getItemType() + ":" + k.getData(), v));
			object.put("STORAGE", storage);
			object.put("TIME_PLACED", mine.getTimePlaced());
			object.put("LIFE_SPAN", mine.getLifeSpan());
			all.put(UUID.randomUUID().toString(), object);
		});
		try {
			java.nio.file.Files.write(Paths.get(getCore().getDataFolder().getPath() + "/" + file), all.toJSONString().getBytes());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}