package us.creepermc.mines.utils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import us.creepermc.mines.Core;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Files {
	public static File getFile(Plugin plugin, String name, boolean create) {
		if(!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
		File file = new File(plugin.getDataFolder(), name);
		try {
			if(!file.exists() && create) plugin.saveResource(name, false);
			return file;
		} catch(IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static File getFile(Plugin plugin, String name) {
		return getFile(plugin, name, false);
	}
	
	public static YamlConfiguration getConfiguration(Plugin plugin, String name) {
		return YamlConfiguration.loadConfiguration(getFile(plugin, name));
	}
	
	public static YamlConfiguration getConfiguration(File file) {
		return file != null ? YamlConfiguration.loadConfiguration(file) : null;
	}
	
	public static void save(Plugin plugin, YamlConfiguration configuration, String name) {
		try {
			configuration.save(getFile(plugin, name));
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@Getter
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	public abstract static class XFile<T> {
		Core plugin;
		String name;
		Map<String, T> storage = new HashMap<>();
		File file;
		@NonFinal
		YamlConfiguration config;
		
		public XFile(Core plugin, String name) {
			this.plugin = plugin;
			this.name = name;
			if(!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
			file = new File(plugin.getDataFolder(), name);
			try {
				if(!file.exists() && !file.createNewFile()) return;
			} catch(Exception ignored) {
			}
			reloadStorage();
		}
		
		public <V> void load(Map<String, V> map) {
			boolean saveFile = false;
			for(Map.Entry<String, V> entry : map.entrySet()) {
				if(getValue(entry.getKey()) != null) continue;
				put(entry.getKey(), (T) entry.getValue());
				saveFile = true;
			}
			if(saveFile) saveStorage();
			map.clear();
		}
		
		public T getValue(String key) {
			return storage.getOrDefault(key, null);
		}
		
		public void put(String key, T value) {
			storage.put(key, value);
		}
		
		public abstract void send(CommandSender sender, String key, String... replace);
		
		@SafeVarargs
		public final void send(CommandSender sender, String key, Pair<String, String>... replace) {
			send(sender, key, Arrays.asList(replace));
		}
		
		public abstract void send(CommandSender sender, String key, List<Pair<String, String>> replace);
		
		public void saveStorage() {
			try {
				storage.forEach((k, v) -> config.set("Storage." + k, v.toString()));
				config.save(file);
			} catch(Exception ignored) {
			}
		}
		
		public void reloadStorage() {
			try {
				config = YamlConfiguration.loadConfiguration(file);
				storage.clear();
				if(config.isConfigurationSection("Storage"))
					for(String s : config.getConfigurationSection("Storage").getValues(false).keySet())
						storage.put(s, registerValue(config.getString("Storage." + s)));
			} catch(Exception ignored) {
			}
		}
		
		public T registerValue(String string) {
			return (T) string;
		}
	}
	
	public static class Messages extends XFile<String> {
		public Messages(Core plugin) {
			super(plugin, "messages.yml");
		}
		
		@Override
		public String getValue(String key) {
			String get = getStorage().get(key);
			return get == null ? null : Util.color(get);
		}
		
		@Override
		public void send(CommandSender sender, String key, String... replace) {
			String get = getValue(key);
			String pre = getValue("PREFIX");
			if(get == null || get.isEmpty() || sender == null) return;
			for(String rep : replace) get = get.replaceFirst("%s", rep);
			if(getPlugin().isUsingPAPI() && sender instanceof Player) {
				if(pre != null) pre = PlaceholderAPI.setPlaceholders((Player) sender, pre);
				get = PlaceholderAPI.setPlaceholders((Player) sender, get);
			}
			if(get.contains("\n")) Arrays.stream(get.split("\n")).forEach(sender::sendMessage);
			else sender.sendMessage((pre == null ? "" : pre) + get);
		}
		
		@Override
		public void send(CommandSender sender, String key, List<Pair<String, String>> replace) {
			String get = getValue(key);
			String pre = getValue("PREFIX");
			if(get == null || get.isEmpty() || sender == null) return;
			for(Pair<?, ?> rep : replace) get = get.replace(rep.getKey().toString(), rep.getValue().toString());
			if(getPlugin().isUsingPAPI() && sender instanceof Player) {
				if(pre != null) pre = PlaceholderAPI.setPlaceholders((Player) sender, pre);
				get = PlaceholderAPI.setPlaceholders((Player) sender, get);
			}
			if(get.contains("\n")) Arrays.stream(get.split("\n")).forEach(sender::sendMessage);
			else sender.sendMessage((pre == null ? "" : pre) + get);
		}
	}
	
	public static class Sounds extends XFile<SSound> {
		public Sounds(Core plugin) {
			super(plugin, "sounds.yml");
		}
		
		public void put(String key, Sound value, float volume, float pitch) {
			getStorage().put(key, new SSound(value, volume, pitch));
		}
		
		public void put(String key, Sound value) {
			put(key, value, 1.0f, 1.0f);
		}
		
		@Override
		public void send(CommandSender sender, String key, String... replace) {
			if(key == null || key.isEmpty() || !(sender instanceof Player)) return;
			SSound get = getValue(key);
			if(get == null) return;
			Player player = (Player) sender;
			player.playSound(player.getLocation(), get.getSound(), get.getVolume(), get.getPitch());
		}
		
		@Override
		public void send(CommandSender sender, String key, List<Pair<String, String>> replace) {
			send(sender, key, new String[0]);
		}
		
		@Override
		public SSound registerValue(String string) {
			try {
				String[] args = string.split(",");
				Sound sound = Sound.valueOf(args[0].toUpperCase());
				float volume = args.length > 1 ? Float.parseFloat(args[1]) : 1.0f;
				float pitch = args.length > 2 ? Float.parseFloat(args[2]) : 1.0f;
				return new SSound(sound, volume, pitch);
			} catch(Exception ignored) {
				return null;
			}
		}
	}
	
	@Getter
	@AllArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	public static class SSound {
		Sound sound;
		float volume;
		float pitch;
		
		public SSound(Sound sound) {
			this.sound = sound;
			volume = 1.0f;
			pitch = 1.0f;
		}
		
		@Override
		public String toString() {
			return sound.toString() + "," + volume + "," + pitch;
		}
	}
	
	public static class Titles extends XFile<Title> {
		public Titles(Core plugin) {
			super(plugin, "titles.yml");
		}
		
		public void put(String key, Pair<String, String> title) {
			getStorage().put(key, new Title(title.getKey(), title.getValue(), 5, 25, 5));
		}
		
		public void put(String key, Title title) {
			getStorage().put(key, title);
		}
		
		@Override
		public void send(CommandSender sender, String key, String... replace) {
			if(key == null || key.isEmpty() || !(sender instanceof Player)) return;
			Title get = getValue(key);
			if(get == null) return;
			String title = get.getTitle();
			String subtitle = get.getSubtitle();
			boolean titleEmpty = title == null || title.isEmpty();
			boolean subtitleEmpty = subtitle == null || subtitle.isEmpty();
			if(titleEmpty && subtitleEmpty) return;
			for(String rep : replace) {
				if(!titleEmpty) title = title.replaceFirst("%s", rep);
				if(!subtitleEmpty) subtitle = subtitle.replaceFirst("%s", rep);
			}
			Player player = (Player) sender;
			if(getPlugin().isUsingPAPI()) {
				if(!titleEmpty) title = PlaceholderAPI.setPlaceholders(player, title);
				if(!subtitleEmpty) subtitle = PlaceholderAPI.setPlaceholders(player, subtitle);
			}
			TitleManager.sendTitle(player, get.getFadeIn(), get.getStay(), get.getFadeOut(), title, subtitle);
		}
		
		@Override
		public void send(CommandSender sender, String key, List<Pair<String, String>> replace) {
			if(key == null || key.isEmpty() || !(sender instanceof Player)) return;
			Title get = getValue(key);
			if(get == null) return;
			String title = get.getTitle();
			String subtitle = get.getSubtitle();
			boolean titleEmpty = title == null || title.isEmpty();
			boolean subtitleEmpty = subtitle == null || subtitle.isEmpty();
			if(titleEmpty && subtitleEmpty) return;
			for(Pair<?, ?> rep : replace) {
				if(!titleEmpty) title = title.replaceFirst(rep.getKey().toString(), rep.getValue().toString());
				if(!subtitleEmpty) subtitle = subtitle.replaceFirst(rep.getKey().toString(), rep.getValue().toString());
			}
			Player player = (Player) sender;
			if(getPlugin().isUsingPAPI()) {
				if(!titleEmpty) title = PlaceholderAPI.setPlaceholders(player, title);
				if(!subtitleEmpty) subtitle = PlaceholderAPI.setPlaceholders(player, subtitle);
			}
			TitleManager.sendTitle(player, get.getFadeIn(), get.getStay(), get.getFadeOut(), title, subtitle);
		}
		
		@Override
		public Title registerValue(String string) {
			try {
				String[] args = string.split("~");
				String title = args[0];
				String subtitle = args[1];
				int fadeIn = args.length > 2 && Util.isInt(args[2]) ? Integer.parseInt(args[2]) : 5;
				int stay = args.length > 3 && Util.isInt(args[3]) ? Integer.parseInt(args[3]) : 25;
				int fadeOut = args.length > 4 && Util.isInt(args[4]) ? Integer.parseInt(args[4]) : 5;
				return new Title(title, subtitle, fadeIn, stay, fadeOut);
			} catch(Exception ex) {
				return null;
			}
		}
	}
	
	@Getter
	@AllArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	public static class Title {
		String title;
		String subtitle;
		int fadeIn;
		int stay;
		int fadeOut;
		
		public Title(String title, String subtitle) {
			this.title = title;
			this.subtitle = subtitle;
			this.fadeIn = 5;
			this.stay = 25;
			this.fadeOut = 5;
		}
		
		@Override
		public String toString() {
			return title + "~" + subtitle + "~" + fadeIn + "~" + stay + "~" + fadeOut;
		}
	}
	
	@Getter
	@AllArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	public static class Pair<K, V> {
		K key;
		V value;
		
		@Override
		public String toString() {
			return key + "~" + value;
		}
	}
	
	public static class ActionBars extends XFile<String> {
		public ActionBars(Core plugin) {
			super(plugin, "action-bars.yml");
		}
		
		@Override
		public String getValue(String key) {
			String get = getStorage().get(key);
			return get == null ? null : Util.color(get);
		}
		
		@Override
		public void send(CommandSender sender, String key, String... replace) {
			String get = getValue(key);
			if(get == null || get.isEmpty() || !(sender instanceof Player)) return;
			for(String rep : replace) get = get.replaceFirst("%s", rep);
			if(getPlugin().isUsingPAPI()) get = PlaceholderAPI.setPlaceholders((Player) sender, get);
			ActionBarManager.sendActionText((Player) sender, get);
		}
		
		@Override
		public void send(CommandSender sender, String key, List<Pair<String, String>> replace) {
			String get = getValue(key);
			if(get == null || get.isEmpty() || !(sender instanceof Player)) return;
			for(Pair<?, ?> rep : replace) get = get.replaceFirst(rep.getKey().toString(), rep.getValue().toString());
			if(getPlugin().isUsingPAPI()) get = PlaceholderAPI.setPlaceholders((Player) sender, get);
			ActionBarManager.sendActionText((Player) sender, get);
		}
	}
}