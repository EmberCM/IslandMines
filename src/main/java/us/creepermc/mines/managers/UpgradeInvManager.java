package us.creepermc.mines.managers;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.creepermc.mines.Core;
import us.creepermc.mines.objects.*;
import us.creepermc.mines.templates.XListener;
import us.creepermc.mines.utils.Files;
import us.creepermc.mines.utils.Util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpgradeInvManager extends XListener {
	final Map<Mine, XInv> inventories = new HashMap<>();
	final Map<Mine, List<UpgradeItems>> upgradeItems = new HashMap<>();
	MinesManager minesManager;
	StorageManager storageManager;
	
	public UpgradeInvManager(Core core) {
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
				inventories.put(mine, new XInv(config, path + "upgrade-inventory"));
				List<UpgradeItems> upgradeItems = new ArrayList<>();
				config.getConfigurationSection(path + "upgrade-inventory.upgrades").getKeys(false).forEach(uid -> {
					String upath = path + "upgrade-inventory.upgrades." + uid + ".";
					int slot = config.getInt(upath + "slot");
					ItemStack unlocked = Util.getItem(config, upath + "unlocked");
					ItemStack locked = Util.getItem(config, upath + "locked");
					upgradeItems.add(new UpgradeItems(uid, slot, unlocked, locked));
				});
				this.upgradeItems.put(mine, upgradeItems);
			});
	}
	
	@Override
	public void deinitialize() {
		minesManager = null;
		storageManager = null;
	}
	
	private Entry<Mine, XInv> getInventory(Inventory inventory) {
		return inventories.entrySet().stream().filter(entry -> inventory.getTitle().equals(entry.getValue().getInventory().getTitle())).findFirst().orElse(null);
	}
	
	private UpgradeItems getUpgradeItems(Mine mine, int slot) {
		return upgradeItems.get(mine).stream().filter(items -> items.getSlot() == slot).findFirst().orElse(null);
	}
	
	public void openInventory(Player player, PlayerMine mine) {
		XInv inv = inventories.get(mine.getMine());
		if(inv == null) return;
		getCore().sendMsg(player, "OPEN_UPGRADE_INVENTORY");
		Inventory copy = getCore().getServer().createInventory(null, inv.getInventory().getInventory().getSize(), inv.getInventory().getTitle());
		copy.setContents(inv.getInventory().getInventory().getContents());
		int upgradeIndex = minesManager.getUpgradeIndex(mine.getMine(), mine.getUpgrade());
		upgradeItems.get(mine.getMine()).forEach(items -> {
			List<Files.Pair<String, String>> replace = new ArrayList<>();
			Upgrade upgrade = minesManager.getUpgrade(mine.getMine(), items.getId());
			if(upgrade == null) return;
			int index = minesManager.getUpgradeIndex(mine.getMine(), upgrade);
			boolean has = upgradeIndex >= index;
			boolean next = index - upgradeIndex == 1;
			replace.add(new Files.Pair<>("{cost}", NumberFormat.getNumberInstance().format(upgrade.getPrice())));
			replace.add(new Files.Pair<>("{blocks}", NumberFormat.getNumberInstance().format(upgrade.getBlocks())));
			replace.add(new Files.Pair<>("{progress}", next ? NumberFormat.getNumberInstance().format(mine.getProgress()) : "0"));
			copy.setItem(items.getSlot(), Util.replace(has ? items.getUnlocked().clone() : items.getLocked().clone(), replace));
		});
		player.openInventory(copy);
	}
	
	@EventHandler
	public void inventoryClick(InventoryClickEvent event) {
		if(event.getInventory() == null) return;
		Entry<Mine, XInv> entry = getInventory(event.getInventory());
		if(entry == null) return;
		event.setCancelled(true);
		UpgradeItems items = getUpgradeItems(entry.getKey(), event.getRawSlot());
		if(items == null) return;
		Player player = (Player) event.getWhoClicked();
		PlayerMine mine = storageManager.getMine(player);
		if(mine == null) return;
		Upgrade upgrade = minesManager.getUpgrade(mine.getMine(), items.getId());
		if(upgrade == null) return;
		int upgradeIndex = minesManager.getUpgradeIndex(mine.getMine(), mine.getUpgrade());
		int index = minesManager.getUpgradeIndex(mine.getMine(), upgrade);
		if(upgradeIndex >= index) {
			getCore().sendMsg(player, "ALREADY_HAVE");
			return;
		} else if(index - upgradeIndex > 1) {
			getCore().sendMsg(player, "NOT_IN_ORDER");
			return;
		} else if(mine.getProgress() < upgrade.getBlocks()) {
			getCore().sendMsg(player, "NOT_ENOUGH_BLOCKS");
			return;
		} else if(getCore().getEcon().getBalance(player) < upgrade.getPrice()) {
			getCore().sendMsg(player, "NOT_ENOUGH_MONEY");
			return;
		}
		player.closeInventory();
		getCore().getEcon().withdrawPlayer(player, upgrade.getPrice());
		mine.upgrade(upgrade);
		mine.reset(minesManager.getBlocksPerTick());
		getCore().sendMsg(player, "UPGRADED", upgrade.getPrettyId());
	}
}