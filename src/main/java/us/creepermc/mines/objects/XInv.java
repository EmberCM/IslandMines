package us.creepermc.mines.objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import us.creepermc.mines.templates.XInvManager;
import us.creepermc.mines.utils.Util;

import java.util.ArrayList;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class XInv {
	List<XInvManager.MenuItem> menuItems = new ArrayList<>();
	String path;
	Util.XInventory inventory;
	
	public XInv(YamlConfiguration config, String ogpath) {
		this.path = ogpath;
		
		if(config.isConfigurationSection(path + ".menuitems"))
			config.getConfigurationSection(path + ".menuitems").getKeys(false).forEach(id -> {
				String path = this.path + ".menuitems." + id + ".";
				XInvManager.MenuItemType type;
				try {
					type = XInvManager.MenuItemType.valueOf(id.toUpperCase());
				} catch(Exception ex) {
					return;
				}
				if(type == XInvManager.MenuItemType.INFO && config.getInt(path + "slot", -1) == -1) {
					config.getConfigurationSection(path.substring(0, path.length() - 1)).getKeys(false).forEach(mid -> {
						String mpath = path + mid + ".";
						int slot = config.getInt(mpath + "slot");
						ItemStack item = Util.getItem(config, mpath);
						menuItems.add(new XInvManager.MenuItem(mid, type, slot, item));
					});
					return;
				} else if(type == XInvManager.MenuItemType.PURCHASE && config.getInt(path + "slot", -1) == -1) {
					config.getConfigurationSection(path.substring(0, path.length() - 1)).getKeys(false).forEach(mid -> {
						String mpath = path + mid + ".";
						int slot = config.getInt(mpath + "slot");
						double price = config.getDouble(mpath + "price");
						ItemStack item = Util.getItem(config, mpath);
						Object reward = config.getStringList(mpath + "commands");
						if(((List<String>) reward).isEmpty()) reward = Util.getItem(config, mpath + "reward");
						menuItems.add(new XInvManager.PurchaseItem(mid, type, slot, price, item, reward));
					});
					return;
				}
				int slot = config.getInt(path + "slot");
				ItemStack item = Util.getItem(config, path);
				menuItems.add(new XInvManager.MenuItem(id, type, slot, item));
			});
		inventory = Util.getInventory(config, path);
	}
	
	public XInvManager.MenuItem getMenuItem(int slot) {
		return menuItems.stream().filter(mitem -> mitem.getSlot() == slot).findFirst().orElse(null);
	}
}