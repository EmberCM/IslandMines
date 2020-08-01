package us.creepermc.mines.listeners;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import us.creepermc.mines.Core;
import us.creepermc.mines.managers.StorageManager;
import us.creepermc.mines.objects.PlayerMine;
import us.creepermc.mines.templates.XListener;

import java.util.List;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockListener extends XListener {
	StorageManager storageManager;
	
	public BlockListener(Core core) {
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
		event.setCancelled(!event.getPlayer().hasPermission("islandmines.admin"));
		Block block = event.getBlock();
		boolean redstoneMatch = block.getType() == Material.GLOWING_REDSTONE_ORE && mine.getUpgrade().getData().getItemType() == Material.REDSTONE_ORE;
		if(!block.getState().getData().equals(mine.getUpgrade().getData()) && !redstoneMatch) return;
		mine.addProgress();
		mine.addStorage(redstoneMatch ? mine.getUpgrade().getData() : block.getState().getData());
		block.setType(Material.AIR);
	}
	
	@EventHandler
	public void blockPlace(BlockPlaceEvent event) {
		if(event.getPlayer().hasPermission("islandmines.admin")) return;
		PlayerMine mine = storageManager.getMine(event.getBlock().getLocation());
		if(mine == null) return;
		event.setCancelled(true);
	}
	
	@EventHandler
	public void explode(EntityExplodeEvent event) {
		List<Block> remove = event.blockList().stream()
				.filter(block -> block.getType() == Material.WALL_SIGN && storageManager.getMine(block.getLocation()) != null)
				.collect(Collectors.toList());
		event.blockList().removeAll(remove);
	}
}