package us.creepermc.mines.listeners;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import us.creepermc.mines.Core;
import us.creepermc.mines.managers.MainInvManager;
import us.creepermc.mines.managers.StorageManager;
import us.creepermc.mines.objects.PlayerMine;
import us.creepermc.mines.templates.XListener;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class InteractListener extends XListener {
	MainInvManager mainInvManager;
	StorageManager storageManager;
	
	public InteractListener(Core core) {
		super(core);
	}
	
	@Override
	public void initialize() {
		deinitialize();
		
		mainInvManager = getCore().getManager(MainInvManager.class);
		storageManager = getCore().getManager(StorageManager.class);
	}
	
	@Override
	public void deinitialize() {
		mainInvManager = null;
		storageManager = null;
	}
	
	@EventHandler
	public void interact(PlayerInteractEvent event) {
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if(event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.BEDROCK) return;
		PlayerMine mine = storageManager.getMine(event.getClickedBlock().getLocation());
		if(mine == null) return;
		event.setCancelled(true);
		Player player = event.getPlayer();
		if(!player.getUniqueId().equals(mine.getOwner())) {
			getCore().sendMsg(player, "NOT_OWNER");
			return;
		}
		mainInvManager.openInventory(player, mine);
	}
}