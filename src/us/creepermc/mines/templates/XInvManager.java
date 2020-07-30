package us.creepermc.mines.templates;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.creepermc.mines.Core;
import us.creepermc.mines.utils.Files;
import us.creepermc.mines.utils.Util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class XInvManager extends XListener {
	List<MenuItem> menuItems = new ArrayList<>();
	final String path;
	@NonFinal
	Util.XInventory inventory;
	
	public XInvManager(Core core, String path) {
		super(core);
		this.path = path;
	}
	
	@Override
	public void initialize() {
		deinitialize();
		
		if(getCore().getConfig().isConfigurationSection(path + ".menuitems"))
			getCore().getConfig().getConfigurationSection(path + ".menuitems").getKeys(false).forEach(id -> {
				String path = this.path + ".menuitems." + id + ".";
				MenuItemType type;
				try {
					type = MenuItemType.valueOf(id.toUpperCase());
				} catch(Exception ex) {
					return;
				}
				if(type == MenuItemType.INFO && getCore().getConfig().getInt(path + "slot", -1) == -1) {
					getCore().getConfig().getConfigurationSection(path.substring(0, path.length() - 1)).getKeys(false).forEach(mid -> {
						String mpath = path + mid + ".";
						int slot = getCore().getConfig().getInt(mpath + "slot");
						ItemStack item = Util.getItem(getCore().getConfig(), mpath);
						menuItems.add(new MenuItem(mid, type, slot, item));
					});
					return;
				} else if(type == MenuItemType.PURCHASE && getCore().getConfig().getInt(path + "slot", -1) == -1) {
					getCore().getConfig().getConfigurationSection(path.substring(0, path.length() - 1)).getKeys(false).forEach(mid -> {
						String mpath = path + mid + ".";
						int slot = getCore().getConfig().getInt(mpath + "slot");
						double price = getCore().getConfig().getDouble(mpath + "price");
						ItemStack item = Util.getItem(getCore().getConfig(), mpath);
						Object reward = getCore().getConfig().getStringList(mpath + "commands");
						if(((List<String>) reward).isEmpty()) reward = Util.getItem(getCore().getConfig(), mpath + "reward");
						menuItems.add(new PurchaseItem(mid, type, slot, price, item, reward));
					});
					return;
				}
				int slot = getCore().getConfig().getInt(path + "slot");
				ItemStack item = Util.getItem(getCore().getConfig(), path);
				menuItems.add(new MenuItem(id, type, slot, item));
			});
		inventory = Util.getInventory(getCore().getConfig(), path);
	}
	
	@Override
	public void deinitialize() {
		menuItems.clear();
		if(inventory != null) {
			inventory.getInventory().clear();
			inventory = null;
		}
	}
	
	public MenuItem getMenuItem(int slot) {
		return menuItems.stream().filter(mitem -> mitem.getSlot() == slot).findFirst().orElse(null);
	}
	
	public void openInventory(Player player, String message, List<Files.Pair<String, String>> replace) {
		getCore().sendMsg(player, message);
		if(menuItems.isEmpty()) {
			player.openInventory(inventory.getInventory());
			return;
		}
		Inventory copy = getCore().getServer().createInventory(null, inventory.getInventory().getSize(), inventory.getTitle());
		copy.setContents(inventory.getInventory().getContents());
		menuItems.forEach(mitem -> copy.setItem(mitem.getSlot(), Util.replace(mitem.getItem(), replace)));
		player.openInventory(copy);
	}
	
	@SafeVarargs
	public final void openInventory(Player player, String message, Files.Pair<String, String>... replace) {
		openInventory(player, message, Arrays.asList(replace));
	}
	
	public void openInventory(Player player, List<Files.Pair<String, String>> replace) {
		openInventory(player, "OPEN_INVENTORY", replace);
	}
	
	@SafeVarargs
	public final void openInventory(Player player, Files.Pair<String, String>... replace) {
		openInventory(player, "OPEN_INVENTORY", Arrays.asList(replace));
	}
	
	public void updateMenuItems(Inventory inventory, List<Files.Pair<String, String>> replace) {
		menuItems.forEach(mitem -> inventory.setItem(mitem.getSlot(), Util.replace(mitem.getItem(), replace)));
		inventory.getViewers().stream().map(Player.class::cast).forEach(Player::updateInventory);
	}
	
	@SafeVarargs
	public final void updateMenuItems(Inventory inventory, Files.Pair<String, String>... replace) {
		updateMenuItems(inventory, Arrays.asList(replace));
	}
	
	@EventHandler
	public void inventoryClick(InventoryClickEvent event) {
		if(event.getInventory() == null || inventory == null) return;
		if(!event.getView().getTitle().equals(inventory.getTitle())) return;
		event.setCancelled(true);
		invClick(event);
		if(menuItems.isEmpty()) return;
		MenuItem mitem = getMenuItem(event.getRawSlot());
		if(mitem == null) return;
		menuItemClick(event, mitem);
	}
	
	@EventHandler
	public void inventoryClose(InventoryCloseEvent event) {
		if(event.getInventory() == null || inventory == null) return;
		if(!event.getView().getTitle().equals(inventory.getTitle())) return;
		invClose((Player) event.getPlayer());
	}
	
	public void invClick(InventoryClickEvent event) {
	}
	
	public void menuItemClick(InventoryClickEvent event, MenuItem menuItem) {
	}
	
	public void invClose(Player player) {
	}
	
	@Getter
	@AllArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	public static class MenuItem {
		String id;
		MenuItemType type;
		int slot;
		ItemStack item;
	}
	
	@Getter
	public static class PurchaseItem extends MenuItem {
		private final double price;
		private final Object reward;
		
		public PurchaseItem(String id, MenuItemType type, int slot, double price, ItemStack item, Object reward) {
			super(id, type, slot, item);
			this.price = price;
			this.reward = reward;
		}
		
		public void giveReward(Core core, Player player) {
			core.sendMsg(player, "PURCHASED", getId().replace("_", " "), NumberFormat.getNumberInstance().format(price));
			if(reward instanceof ItemStack) {
				if(player.getInventory().firstEmpty() != -1)
					player.getInventory().addItem((ItemStack) reward);
				else {
					player.getWorld().dropItemNaturally(player.getLocation(), (ItemStack) reward);
					core.sendMsg(player, "FULL_INVENTORY");
				}
				return;
			}
			((List<String>) reward).forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName())));
		}
	}
	
	public enum MenuItemType {
		INFO,
		PURCHASE,
		PREVIOUS_PAGE,
		PREVIOUS_PAGE_INVALID,
		CURRENT_PAGE,
		NEXT_PAGE,
		NEXT_PAGE_INVALID,
	}
}