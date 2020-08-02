package us.creepermc.mines;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import us.creepermc.mines.templates.XManager;
import us.creepermc.mines.utils.Files;
import us.creepermc.mines.utils.Util;
import us.creepermc.mines.utils.XSound;

import java.io.File;
import java.util.*;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Core extends JavaPlugin {
	final List<XManager> managers = new ArrayList<>();
	final List<Files.XFile<?>> send = new ArrayList<>();
	String allowed;
	
	boolean usingHD;
	boolean usingPAPI;
	boolean usingSSB;
	Economy econ;
	
	@Override
	public void onEnable() {
		send.addAll(Arrays.asList(new Files.Messages(this), new Files.Sounds(this)));
		
		initConfig();
		
		Util.registerHooks(this);
		econ = Util.setupVault(Economy.class);
		usingHD = getServer().getPluginManager().isPluginEnabled("HolographicDisplays");
		usingPAPI = getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
		usingSSB = getServer().getPluginManager().isPluginEnabled("SuperiorSkyblock2");
	}
	
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		
		managers.forEach(XManager::deinitialize);
		managers.clear();
		send.stream().map(Files.XFile::getStorage).forEach(Map::clear);
		send.clear();
		allowed = null;
		usingHD = false;
		usingPAPI = false;
		usingSSB = false;
		econ = null;
	}
	
	public void initConfig() {
		if(!new File(getDataFolder(), "config.yml").exists()) saveDefaultConfig();
		reloadConfig();
		managers.forEach(XManager::initialize);
		send.forEach(Files.XFile::reloadStorage);
		
		Map<String, String> defMsgs = new HashMap<>();
		Map<String, Files.SSound> defSnds = new HashMap<>();
		defMsgs.put("PREFIX", "&6" + getDescription().getName() + " &8\u00BB &7");
		putMsg(defMsgs, defSnds, "RELOADED", "You have updated the config file(s)", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "USAGE", "Use &c/islandmines [reload, give]", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "USAGE_GIVE", "Use &c/islandmines give <player> <mine> [amount]", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "OFFLINE", "That player is offline", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "INVALID_MINE", "Could not find the mine called %s", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "GAVE", "You gave {player} {amount} {mine} Mine(s)", XSound.ORB_PICKUP);
		putMsg(defMsgs, defSnds, "GIVEN", "You were given {amount} {mine} Mine(s)", XSound.ORB_PICKUP);
		putMsg(defMsgs, defSnds, "FULL_INVENTORY", "Your inventory was full so your items were dropped on the ground", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "NOT_AVAILABLE", "There is a {block} obstructing your mine at {x}, {y}, {z}", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "CANCELLED", "You have cancelled placing your mine", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "PLACED_MINE", "You have placed a %s mine. Type 'yes' or 'no' in chat to confirm placement", XSound.ORB_PICKUP);
		putMsg(defMsgs, defSnds, "CONFIRMED", "You have successfully placed a %s mine", XSound.LEVEL_UP);
		putMsg(defMsgs, defSnds, "NOT_OWNER", "That is not your mine", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "RESET_COOLDOWN", "You must wait another %s before resetting the mine again", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "RESET", "You have reset your mine", XSound.ORB_PICKUP);
		putMsg(defMsgs, defSnds, "CANCELLED_REMOVE", "You have cancelled removing your mine", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "DELETED_MINE", "You have deleted your %s mine", XSound.ORB_PICKUP);
		putMsg(defMsgs, defSnds, "SOLD", "You have sold the items from your Mine for $%s", XSound.ORB_PICKUP);
		putMsg(defMsgs, defSnds, "OPEN_MAIN_INVENTORY", "", XSound.ORB_PICKUP);
		putMsg(defMsgs, defSnds, "OPEN_CONFIRM_INVENTORY", "", XSound.ORB_PICKUP);
		putMsg(defMsgs, defSnds, "OPEN_STORAGE_INVENTORY", "", XSound.ORB_PICKUP);
		putMsg(defMsgs, defSnds, "ALREADY_HAVE", "Your mine already has that upgrade", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "NOT_IN_ORDER", "You must upgrade your mine in order", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "NOT_ENOUGH_BLOCKS", "You haven't mined enough blocks to upgrade yet", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "NOT_ENOUGH_MONEY", "You don't have enough money for that upgrade", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "UPGRADED", "You have upgraded your mine to %s", XSound.ORB_PICKUP);
		putMsg(defMsgs, defSnds, "NOT_YOUR_ISLAND", "You can only place a mine on your island", XSound.FIZZ);
		putMsg(defMsgs, defSnds, "MINE_DIED", "Your %s Mine has ran out of time and been removed", XSound.FIZZ);
		getSend(Files.Messages.class).load(defMsgs);
		getSend(Files.Sounds.class).load(defSnds);
	}
	
	public void sendMsg(CommandSender sender, String key, String... replace) {
		send.forEach(xfile -> xfile.send(sender, key, replace));
	}
	
	@SafeVarargs
	public final void sendMsg(CommandSender sender, String key, Files.Pair<String, String>... replace) {
		send.forEach(xfile -> xfile.send(sender, key, replace));
	}
	
	public void sendMsg(CommandSender sender, String key, List<Files.Pair<String, String>> replace) {
		send.forEach(xfile -> xfile.send(sender, key, replace));
	}
	
	public void sendMsg(CommandSender sender, String key) {
		send.forEach(xfile -> xfile.send(sender, key, new String[0]));
	}
	
	public <T> T getManager(Class<T> clazz) {
		return (T) managers.stream().filter(manager -> manager.getClass().equals(clazz)).findFirst().orElse(null);
	}
	
	public <T> T getSend(Class<T> clazz) {
		return (T) send.stream().filter(xfile -> xfile.getClass().equals(clazz)).findFirst().orElse(null);
	}
	
	private void putMsg(Map<String, String> msgs, Map<String, Files.SSound> sounds, String key, String msg, XSound sound) {
		msgs.put(key, msg);
		sounds.put(key, new Files.SSound(sound.bukkitSound()));
	}
}