package us.creepermc.mines.managers;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.metadata.FixedMetadataValue;
import us.creepermc.mines.Core;
import us.creepermc.mines.objects.PlayerMine;
import us.creepermc.mines.templates.XInvManager;
import us.creepermc.mines.utils.Util;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class MainInvManager extends XInvManager {
	ConfirmRemoveGUI confirmRemoveGUI;
	StorageInvManager storageInvManager;
	StorageManager storageManager;
	UpgradeInvManager upgradeInvManager;
	
	public MainInvManager(Core core) {
		super(core, "main-inventory");
	}
	
	@Override
	public void initialize() {
		deinitialize();
		super.initialize();
		
		confirmRemoveGUI = getCore().getManager(ConfirmRemoveGUI.class);
		storageInvManager = getCore().getManager(StorageInvManager.class);
		storageManager = getCore().getManager(StorageManager.class);
		upgradeInvManager = getCore().getManager(UpgradeInvManager.class);
		getMenuItems().forEach(mitem -> getInventory().getInventory().setItem(mitem.getSlot(), Util.replace(mitem.getItem())));
	}
	
	@Override
	public void deinitialize() {
		super.deinitialize();
		
		confirmRemoveGUI = null;
		storageInvManager = null;
		storageManager = null;
		upgradeInvManager = null;
	}
	
	public void openInventory(Player player, PlayerMine mine) {
		getCore().sendMsg(player, "OPEN_MAIN_INVENTORY");
		player.setMetadata("islandmines_mine", new FixedMetadataValue(getCore(), Util.simpleLocationToString(mine.getPlaced())));
		player.openInventory(getInventory().getInventory());
	}
	
	@Override
	public void menuItemClick(InventoryClickEvent event, MenuItem menuItem) {
		Player player = (Player) event.getWhoClicked();
		player.closeInventory();
		PlayerMine mine = storageManager.getMine(player);
		if(mine == null) return;
		switch(menuItem.getId()) {
			case "upgrade":
				upgradeInvManager.openInventory(player, mine);
				break;
			case "reset":
				if(mine.isInCooldown()) {
					getCore().sendMsg(player, "RESET_COOLDOWN", Util.timeFromMillis(mine.getCooldown(), "medium"));
					break;
				}
				mine.reset(getCore(), true);
				getCore().sendMsg(player, "RESET");
				break;
			case "storage":
				storageInvManager.openInventory(player, mine);
				break;
			case "remove":
				if(!player.hasPermission("islandmines.admin") && !player.getUniqueId().equals(mine.getOwner())) {
					getCore().sendMsg(player, "NOT_OWNER");
					break;
				}
				confirmRemoveGUI.openInventory(player);
				break;
		}
	}
}