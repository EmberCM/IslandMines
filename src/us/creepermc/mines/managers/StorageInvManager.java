package us.creepermc.mines.managers;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import us.creepermc.mines.Core;
import us.creepermc.mines.objects.Mine;
import us.creepermc.mines.objects.PlayerMine;
import us.creepermc.mines.objects.Upgrade;
import us.creepermc.mines.objects.XInv;
import us.creepermc.mines.templates.XInvManager;
import us.creepermc.mines.templates.XListener;
import us.creepermc.mines.utils.Files;
import us.creepermc.mines.utils.Util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class StorageInvManager extends XListener {
	final Map<Mine, XInv> inventories = new HashMap<>();
	MinesManager minesManager;
	StorageManager storageManager;
	
	public StorageInvManager(Core core) {
		super(core);
	}
	
	@Override
	public void initialize() {
		deinitialize();
		
		minesManager = getCore().getManager(MinesManager.class);
		storageManager = getCore().getManager(StorageManager.class);
		YamlConfiguration config = Files.getConfiguration(Files.getFile(getCore(), "mines.yml", true));
		if(config.isConfigurationSection("mines"))
			config.getConfigurationSection("mines").getKeys(false).forEach(id -> {
				String path = "mines." + id + ".";
				Mine mine = minesManager.getMine(id);
				if(mine == null) return;
				inventories.put(mine, new XInv(config, path + "storage-inventory"));
			});
	}
	
	@Override
	public void deinitialize() {
		minesManager = null;
		storageManager = null;
	}
	
	public XInv getInventory(Inventory inventory) {
		return inventories.values().stream().filter(inv -> inventory.getTitle().equals(inv.getInventory().getTitle())).findFirst().orElse(null);
	}
	
	public void openInventory(Player player, PlayerMine mine) {
		XInv inv = inventories.get(mine.getMine());
		if(inv == null) return;
		getCore().sendMsg(player, "OPEN_STORAGE_INVENTORY");
		Inventory copy = getCore().getServer().createInventory(null, inv.getInventory().getInventory().getSize(), inv.getInventory().getTitle());
		copy.setContents(inv.getInventory().getInventory().getContents());
		inv.getMenuItems().forEach(mitem -> {
			List<Files.Pair<String, String>> replace = new ArrayList<>();
			Upgrade upgrade = minesManager.getUpgrade(mine.getMine(), mitem.getId());
			if(upgrade != null) {
				replace.add(new Files.Pair<>("{quantity}", NumberFormat.getNumberInstance().format(mine.getStorage().getOrDefault(upgrade.getData(), 0))));
				replace.add(new Files.Pair<>("{worth}", NumberFormat.getNumberInstance().format(storageManager.getWorth(mine, player, upgrade.getData()))));
			} else {
				replace.add(new Files.Pair<>("{quantity}", "0"));
				replace.add(new Files.Pair<>("{worth}", "0"));
				replace.add(new Files.Pair<>("{worth_total}", NumberFormat.getNumberInstance().format(storageManager.getWorth(mine, player))));
			}
			copy.setItem(mitem.getSlot(), Util.replace(mitem.getItem(), replace));
		});
		player.openInventory(copy);
	}
	
	@EventHandler
	public void inventoryClick(InventoryClickEvent event) {
		if(event.getInventory() == null) return;
		XInv inventory = getInventory(event.getInventory());
		if(inventory == null) return;
		event.setCancelled(true);
		if(inventory.getMenuItems().isEmpty()) return;
		XInvManager.MenuItem mitem = inventory.getMenuItem(event.getRawSlot());
		if(mitem == null) return;
		menuItemClick(event, mitem);
	}
	
	public void menuItemClick(InventoryClickEvent event, XInvManager.MenuItem menuItem) {
		if(!menuItem.getId().equalsIgnoreCase("sellall")) return;
		Player player = (Player) event.getWhoClicked();
		player.closeInventory();
		PlayerMine mine = storageManager.getMine(player);
		if(mine == null) return;
		double worth = storageManager.getWorth(mine, player);
		getCore().getEcon().depositPlayer(player, worth);
		mine.getStorage().clear();
		getCore().sendMsg(player, "SOLD", NumberFormat.getNumberInstance().format(worth));
	}
}