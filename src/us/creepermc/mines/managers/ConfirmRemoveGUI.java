package us.creepermc.mines.managers;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import us.creepermc.mines.Core;
import us.creepermc.mines.objects.PlayerMine;
import us.creepermc.mines.templates.XInvManager;
import us.creepermc.mines.utils.Util;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfirmRemoveGUI extends XInvManager {
	StorageManager storageManager;
	
	public ConfirmRemoveGUI(Core core) {
		super(core, "confirm-inventory");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		storageManager = getCore().getManager(StorageManager.class);
		getMenuItems().forEach(mitem -> getInventory().getInventory().setItem(mitem.getSlot(), Util.replace(mitem.getItem())));
	}
	
	@Override
	public void deinitialize() {
		super.deinitialize();
		
		storageManager = null;
	}
	
	public void openInventory(Player player) {
		getCore().sendMsg(player, "OPEN_CONFIRM_INVENTORY");
		player.openInventory(getInventory().getInventory());
	}
	
	@Override
	public void menuItemClick(InventoryClickEvent event, MenuItem menuItem) {
		Player player = (Player) event.getWhoClicked();
		player.closeInventory();
		PlayerMine mine = storageManager.getMine(player);
		if(mine == null) return;
		player.removeMetadata("islandmines_mine", getCore());
		switch(menuItem.getId()) {
			case "confirm":
				if(!mine.canRemove()) {
					getCore().sendMsg(player, "MINE_SAFETY");
					break;
				}
				storageManager.deleteMine(mine, player, true);
				break;
			case "deny":
				getCore().sendMsg(player, "CANCELLED_REMOVE");
				break;
		}
	}
}