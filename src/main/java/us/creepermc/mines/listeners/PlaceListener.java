package us.creepermc.mines.listeners;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import us.creepermc.mines.Core;
import us.creepermc.mines.managers.MinesManager;
import us.creepermc.mines.managers.SuperiorSkyblockHook;
import us.creepermc.mines.objects.Mine;
import us.creepermc.mines.objects.TempMine;
import us.creepermc.mines.templates.XListener;
import us.creepermc.mines.utils.BlockUtil;
import us.creepermc.mines.utils.Files;
import us.creepermc.mines.utils.Util;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlaceListener extends XListener {
	MinesManager manager;
	
	public PlaceListener(Core core) {
		super(core);
	}
	
	@Override
	public void initialize() {
		deinitialize();
		
		manager = getCore().getManager(MinesManager.class);
	}
	
	@Override
	public void deinitialize() {
		manager = null;
	}
	
	@EventHandler
	public void place(BlockPlaceEvent event) {
		if(event.getItemInHand() == null) return;
		Mine mine = manager.getMine(event.getItemInHand());
		if(mine == null) return;
		event.setCancelled(true);
		Player player = event.getPlayer();
		Location location = event.getBlock().getLocation();
		if(getCore().isUsingSSB() && !SuperiorSkyblockHook.isAllowed(player, location)) {
			getCore().sendMsg(player, "NOT_YOUR_ISLAND");
			return;
		}
		Location invalidLocation = manager.isLocationAvailable(location, mine);
		if(invalidLocation != null) {
			String[] split = Util.simpleLocationToString(invalidLocation).split(",");
			getCore().sendMsg(player, "NOT_AVAILABLE",
					new Files.Pair<>("{x}", split[1]),
					new Files.Pair<>("{y}", split[2]),
					new Files.Pair<>("{z}", split[3]),
					new Files.Pair<>("{block}", Util.caps(invalidLocation.getBlock().getType().toString()).replace("_", " "))
			);
			return;
		}
		if(player.getItemInHand().getAmount() > 1) player.getItemInHand().setAmount(player.getItemInHand().getAmount() - 1);
		else player.setItemInHand(new ItemStack(Material.AIR));
		manager.testPlace(player, location, mine);
	}
	
	@EventHandler
	public void chat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if(!manager.getTempMines().containsKey(player.getUniqueId())) return;
		event.setCancelled(true);
		switch(event.getMessage().toLowerCase()) {
			case "y":
			case "yes":
			case "true":
				manager.confirmPlace(player);
				break;
			case "n":
			case "no":
			case "false":
				TempMine mine = manager.getTempMines().remove(player.getUniqueId());
				mine.getLocations().forEach(loc -> BlockUtil.setBlockInNativeChunkSection(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 0, (byte) 0));
				getCore().sendMsg(player, "CANCELLED");
				if(player.getInventory().firstEmpty() != -1) player.getInventory().addItem(mine.getMine().getItem());
				else {
					player.getWorld().dropItemNaturally(player.getLocation(), mine.getMine().getItem());
					getCore().sendMsg(player, "FULL_INVENTORY");
				}
				break;
			default:
				getCore().sendMsg(player, "PLACED_MINE", manager.getTempMines().get(player.getUniqueId()).getMine().getPrettyId());
				break;
		}
	}
	
	@EventHandler
	public void quit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		TempMine mine = manager.getTempMines().remove(player.getUniqueId());
		if(mine == null) return;
		player.getInventory().addItem(mine.getMine().getItem());
		mine.getLocations().forEach(loc -> BlockUtil.setBlockInNativeChunkSection(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 0, (byte) 0));
	}
	
	@EventHandler
	public void kick(PlayerKickEvent event) {
		Player player = event.getPlayer();
		TempMine mine = manager.getTempMines().remove(player.getUniqueId());
		if(mine == null) return;
		player.getInventory().addItem(mine.getMine().getItem());
		mine.getLocations().forEach(loc -> BlockUtil.setBlockInNativeChunkSection(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 0, (byte) 0));
	}
}