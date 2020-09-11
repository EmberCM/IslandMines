package us.creepermc.mines.cmds;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import us.creepermc.mines.Core;
import us.creepermc.mines.managers.MinesManager;
import us.creepermc.mines.objects.Mine;
import us.creepermc.mines.templates.XCommand;
import us.creepermc.mines.utils.Files;
import us.creepermc.mines.utils.Util;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class IslandMinesCmd extends XCommand {
	MinesManager manager;
	
	public IslandMinesCmd(Core core) {
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
	
	@Override
	public void execute(CommandSender sender, String[] args) {
		if(args.length > 0 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("islandmines.admin")) {
			getCore().initConfig();
			getCore().sendMsg(sender, "RELOADED");
			return;
		} else if(args.length > 0 && args[0].equalsIgnoreCase("give") && sender.hasPermission("islandmines.admin")) {
			if(args.length < 3) {
				getCore().sendMsg(sender, "USAGE_GIVE");
				return;
			}
			Player target = getCore().getServer().getPlayer(args[1]);
			if(target == null) {
				getCore().sendMsg(sender, "OFFLINE", args[1]);
				return;
			}
			Mine mine = manager.getMine(args[2]);
			if(mine == null) {
				getCore().sendMsg(sender, "INVALID_MINE", args[2]);
				return;
			}
			ItemStack item = mine.getItem().clone();
			int amount = args.length > 3 && Util.isInt(args[3]) ? Integer.parseInt(args[3]) : 1;
			item.setAmount(amount);
			if(!sender.equals(target))
				getCore().sendMsg(sender, "GAVE", new Files.Pair<>("{player}", target.getName()), new Files.Pair<>("{mine}", mine.getPrettyId()), new Files.Pair<>("{amount}", String.valueOf(amount)));
			getCore().sendMsg(target, "GIVEN", new Files.Pair<>("{mine}", mine.getPrettyId()), new Files.Pair<>("{amount}", String.valueOf(amount)));
			if(target.getInventory().firstEmpty() != -1) target.getInventory().addItem(item);
			else {
				target.getWorld().dropItemNaturally(target.getLocation(), item);
				getCore().sendMsg(target, "FULL_INVENTORY");
			}
			return;
		}
		getCore().sendMsg(sender, "USAGE");
	}
}