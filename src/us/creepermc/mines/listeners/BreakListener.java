package us.creepermc.mines.listeners;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import us.creepermc.mines.Core;
import us.creepermc.mines.managers.StorageManager;
import us.creepermc.mines.objects.PlayerMine;
import us.creepermc.mines.templates.XListener;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BreakListener extends XListener {
	StorageManager storageManager;
	
	public BreakListener(Core core) {
		super(core);
	}
	
	@Override
	public void initialize() {
		deinitialize();
		
		storageManager = getCore().getManager(StorageManager.class);
	}
	
	@Override
	public void deinitialize() {
		storageManager = null;
	}
	
	@EventHandler
	public void blockBreak(BlockBreakEvent event) {
		PlayerMine mine = storageManager.getMine(event.getBlock().getLocation());
		if(mine == null) return;
		event.setCancelled(true);
		Block block = event.getBlock();
		boolean redstoneMatch = block.getType() == Material.GLOWING_REDSTONE_ORE && mine.getUpgrade().getData().getItemType() == Material.REDSTONE_ORE;
		if(block.getState().getData().equals(mine.getUpgrade().getData()) || redstoneMatch) mine.addProgress();
		mine.addStorage(redstoneMatch ? mine.getUpgrade().getData() : block.getState().getData());
		block.setType(Material.AIR);
	}
}