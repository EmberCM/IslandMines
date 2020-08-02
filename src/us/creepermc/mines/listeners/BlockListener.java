package us.creepermc.mines.listeners;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import us.creepermc.mines.Core;
import us.creepermc.mines.managers.StorageManager;
import us.creepermc.mines.managers.SuperiorSkyblockHook;
import us.creepermc.mines.objects.PlayerMine;
import us.creepermc.mines.templates.XListener;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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
		Player player = event.getPlayer();
		if(getCore().isUsingSSB() && !SuperiorSkyblockHook.isAllowed(player, event.getBlock().getLocation())) {
			event.setCancelled(true);
			return;
		}
		event.setCancelled(!player.hasPermission("islandmines.admin"));
		Block block = event.getBlock();
		boolean redstoneMatch = block.getType() == Material.GLOWING_REDSTONE_ORE && mine.getUpgrade().getData().getItemType() == Material.REDSTONE_ORE;
		if(!block.getState().getData().equals(mine.getUpgrade().getData()) && !redstoneMatch) return;
		mine.addProgress();
		int amount = hasFortune(player.getItemInHand()) ? ThreadLocalRandom.current().nextInt(getFortune(player.getItemInHand()) + 1) + 1 : 1;
		mine.addStorage(redstoneMatch ? mine.getUpgrade().getData() : block.getState().getData(), amount);
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
	
	private boolean hasFortune(ItemStack item) {
		return item != null && item.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS);
	}
	
	private int getFortune(ItemStack item) {
		return item.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
	}
}