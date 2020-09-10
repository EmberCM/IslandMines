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
	MineTask mineTask;
	int saveTask;
	
	public StorageManager(Core core) {
		super(core);
	}
	
	@Override
	public void initialize() {
		if(minesManager != null) saveJSON("mines.json");
		if(mineTask != null) mineTask.cancel();
		if(saveTask != 0) getCore().getServer().getScheduler().cancelTask(saveTask);
		mines.clear();
		
		minesManager = getCore().getManager(MinesManager.class);
		worthManager = getCore().getManager(WorthManager.class);
		loadJSON("mines.json");
		saveTask = new BukkitRunnable() {
			@Override
			public void run() {
				saveJSON("mines.json");
			}
		}.runTaskTimerAsynchronously(getCore(), 36000, 36000 /* 30 minutes */).getTaskId();
		(mineTask = new MineTask()).runTaskTimer(getCore(), 20, 20);
	}
	
	@Override
	public void deinitialize() {
		if(mineTask != null) mineTask.cancel();
		if(saveTask != 0) getCore().getServer().getScheduler().cancelTask(saveTask);
		saveJSON("mines.json");
		minesManager = null;
		worthManager = null;
		if(getCore().isUsingHD()) mines.forEach(mine -> HolographicDisplaysHook.deleteHologram(getCore(), mine));
		mines.clear();
		mineTask = null;
		saveTask = 0;
	}
	
	public void createMine(Mine mine, Player player, Location location) {
		PlayerMine pmine = new PlayerMine(player.getUniqueId(), mine, location);
		mines.add(pmine);
		pmine.initialize(getCore());
	}
	
	public void deleteMine(PlayerMine mine, Player player, boolean giveBack) {
		mine.clear();
		if(getCore().isUsingHD()) HolographicDisplaysHook.deleteHologram(getCore(), mine);
		mines.remove(mine);
		if(player == null) return;
		if(!giveBack) {
			getCore().sendMsg(player, "MINE_DIED", mine.getMine().getPrettyId());
			return;
		}
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
	
	public double sellStorage(PlayerMine mine, Player player) {
		double worth = getWorth(mine, player);
		getCore().getEcon().depositPlayer(player, worth);
		mine.getStorage().clear();
		return worth;
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
				boolean removed = Boolean.parseBoolean(v.containsKey("REMOVED") ? v.get("REMOVED").toString() : "false");
				mines.add(new PlayerMine(owner, mine, placed, upgrade, progress, storage, timePlaced, 0, removed));
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
			object.put("REMOVED", mine.isRemoved());
			all.put(UUID.randomUUID().toString(), object);
		});
		try {
			java.nio.file.Files.write(Paths.get(getCore().getDataFolder().getPath() + "/" + file), all.toJSONString().getBytes());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public class MineTask extends BukkitRunnable {
		@Override
		public void run() {
			List<PlayerMine> reset = mines.stream().filter(mine ->
					mine.isPastLifetime() || (System.currentTimeMillis() - mine.getTimePlaced() >= 30000 && mine.getPlaced().getChunk().isLoaded() && mine.getPlaced().getBlock().getType() != Material.BEDROCK)
			).collect(Collectors.toList());
			if(!reset.isEmpty()) {
				new BukkitRunnable() {
					@Override
					public void run() {
						reset.forEach(mine -> deleteMine(mine, getCore().getServer().getPlayer(mine.getOwner()), false));
					}
				}.runTask(getCore());
				mines.removeAll(reset);
			}
			if(getCore().isUsingHD()) mines.forEach(mine -> HolographicDisplaysHook.updateHologram(getCore(), mine));
			List<PlayerMine> list = mines.stream()
					.filter(mine -> {
						long difference = System.currentTimeMillis() - mine.getTimePlaced();
						while(difference >= mine.getMine().getAutomaticReset()) difference -= mine.getMine().getAutomaticReset();
						return difference <= 1000 && mine.getPlaced().getChunk().isLoaded();
					}).collect(Collectors.toList());
			if(!list.isEmpty())
				new BukkitRunnable() {
					@Override
					public void run() {
						list.forEach(PlayerMine::reset);
					}
				}.runTask(getCore());
		}
	}
}