package us.creepermc.mines.listeners;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import us.creepermc.mines.Core;
import us.creepermc.mines.managers.MainInvManager;
import us.creepermc.mines.managers.MinesManager;
import us.creepermc.mines.managers.StorageManager;
import us.creepermc.mines.managers.SuperiorSkyblockHook;
import us.creepermc.mines.objects.PlayerMine;
import us.creepermc.mines.templates.XListener;
import us.creepermc.mines.utils.Util;
import us.creepermc.mines.utils.XMaterial;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class InteractListener extends XListener {
	MainInvManager mainInvManager;
	MinesManager minesManager;
	StorageManager storageManager;
	
	public InteractListener(Core core) {
		super(core);
	}
	
	@Override
	public void initialize() {
		deinitialize();
		
		mainInvManager = getCore().getManager(MainInvManager.class);
		minesManager = getCore().getManager(MinesManager.class);
		storageManager = getCore().getManager(StorageManager.class);
	}
	
	@Override
	public void deinitialize() {
		mainInvManager = null;
		minesManager = null;
		storageManager = null;
	}
	
	@EventHandler
	public void interact(PlayerInteractEvent event) {
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if(event.getClickedBlock() == null) return;
		Material type = event.getClickedBlock().getType();
		if(type != XMaterial.BEDROCK.parseMaterial() && type != XMaterial.WALL_SIGN.parseMaterial()) return;
		Location location = event.getClickedBlock().getLocation();
		PlayerMine mine = storageManager.getMine(location);
		if(mine == null) return;
		event.setCancelled(true);
		Player player = event.getPlayer();
		if(type == XMaterial.BEDROCK.parseMaterial()) {
			if(!player.hasPermission("islandmines.admin") &&
					(getCore().isUsingSSB() ? !SuperiorSkyblockHook.isAllowed(player, location) : !player.getUniqueId().equals(mine.getOwner()))) {
				getCore().sendMsg(player, "NOT_OWNER");
				return;
			}
			mainInvManager.openInventory(player, mine);
			return;
		}
		if(mine.isInCooldown()) {
			getCore().sendMsg(player, "RESET_COOLDOWN", Util.timeFromMillis(mine.getCooldown(), "medium"));
			return;
		}
		mine.reset(minesManager.getBlocksPerTick(), true);
		getCore().sendMsg(player, "RESET");
	}
}