package us.creepermc.mines.utils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Util {
	// » \u00BB |
	public static ItemStack createHead(String owner, String name, String... lore) {
		ItemStack item = new ItemStack(XMaterial.SKELETON_SKULL.parseMaterial(), 1, (short) 3);
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		meta.setOwner(owner);
		meta.setDisplayName(color(name));
		meta.setLore(color(Arrays.asList(lore)));
		item.setItemMeta(meta);
		return item;
	}
	
	public static ItemStack createItem(Material mat, int amt, int durability, String name, List<String> lore) {
		ItemStack item = new ItemStack(mat, amt);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(color(name));
		meta.setLore(color(lore));
		if(durability != 0) item.setDurability((short) durability);
		item.setItemMeta(meta);
		return item;
	}
	
	public static ItemStack createItem(Material mat, int amt, int durability, String name, String... lore) {
		return createItem(mat, amt, durability, name, Arrays.asList(lore));
	}
	
	public static ItemStack createItem(Material mat, int amt, String name, List<String> lore) {
		return createItem(mat, amt, 0, name, lore);
	}
	
	public static ItemStack createItem(Material mat, int amt, String name, String... lore) {
		return createItem(mat, amt, 0, name, lore);
	}
	
	public static ItemStack createItem(Material mat, String name, List<String> lore) {
		return createItem(mat, 1, 0, name, lore);
	}
	
	public static ItemStack createItem(Material mat, String name, String... lore) {
		return createItem(mat, 1, 0, name, lore);
	}
	
	public static ItemStack setOwner(ItemStack item, String owner) {
		if(XMaterial.fromMaterial(item.getType()) != XMaterial.CREEPER_HEAD || item.getDurability() != 3) return item;
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		meta.setOwner(owner);
		item.setItemMeta(meta);
		return item;
	}
	
	public static void removeItemFromInv(Inventory inv, ItemStack item, int amt) {
		int amount = 0;
		for(int i = 0; i < inv.getSize(); i++) {
			ItemStack get = inv.getItem(i);
			if(get == null || get.getType() == Material.AIR) continue;
			if(!get.isSimilar(item)) continue;
			amount += get.getAmount();
			inv.clear(i);
		}
		if(amount <= amt) return;
		ItemStack clone = item.clone();
		clone.setAmount(amount - amt);
		inv.addItem(clone);
	}
	
	public static String caps(String string) {
		String[] list = string.split("_");
		StringBuilder s = new StringBuilder();
		Arrays.stream(list).forEach(st -> s.append(st.substring(0, 1).toUpperCase()).append(st.substring(1).toLowerCase()).append("_"));
		return s.toString().substring(0, s.length() - 1);
	}
	
	public static String removeArgs(String[] args, int amt) {
		return amt >= args.length ? "" : String.join(" ", Arrays.copyOfRange(args, amt, args.length));
	}
	
	public static String removeArgs(String string, int amt) {
		return removeArgs(string.split(" "), amt);
	}
	
	public static boolean isInt(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch(NumberFormatException e) {
			return false;
		}
	}
	
	public static boolean isDouble(String s) {
		try {
			Double.parseDouble(s);
			return true;
		} catch(NumberFormatException e) {
			return false;
		}
	}
	
	public static String color(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}
	
	public static String strip(String s) {
		return ChatColor.stripColor(s);
	}
	
	public static List<String> color(List<String> list) {
		return list.stream().map(Util::color).collect(Collectors.toList());
	}
	
	public static <T> T setupVault(Class<T> clazz) {
		if(Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) return null;
		RegisteredServiceProvider<T> rsp = Bukkit.getServer().getServicesManager().getRegistration(clazz);
		return rsp == null ? null : rsp.getProvider();
	}
	
	public static Material getMaterial(String s) {
		try {
			s = s.toUpperCase().replace(" ", "_");
			if(s.contains(":")) s = s.split(":")[0];
			Material mat = Material.matchMaterial(s);
			return mat != null ? mat : Material.valueOf(s);
		} catch(Exception ex) {
			return Material.getMaterial(s);
		}
	}
	
	public static Enchantment getEnchantment(String s) {
		Enchantment ench = Enchantment.getByName(s.toUpperCase());
		if(ench != null) return ench;
		switch(s.toLowerCase().replace("_", "")) {
			case "power":
			case "arrowdamage":
				return Enchantment.ARROW_DAMAGE;
			case "flame":
			case "arrowfire":
				return Enchantment.ARROW_FIRE;
			case "infinite":
			case "infinity":
			case "arrowinfinite":
				return Enchantment.ARROW_INFINITE;
			case "punch":
			case "arrowknockback":
				return Enchantment.ARROW_KNOCKBACK;
			case "sharp":
			case "damage":
			case "sharpness":
			case "damageall":
				return Enchantment.DAMAGE_ALL;
			case "arthropod":
			case "arthropods":
			case "damagearthropods":
				return Enchantment.DAMAGE_ARTHROPODS;
			case "smite":
			case "damageundead":
				return Enchantment.DAMAGE_UNDEAD;
			case "mining":
			case "efficiency":
			case "digspeed":
				return Enchantment.DIG_SPEED;
			case "unbreak":
			case "unbreaking":
				return Enchantment.DURABILITY;
			case "fire":
			case "fireaspect":
				return Enchantment.FIRE_ASPECT;
			case "kb":
				return Enchantment.KNOCKBACK;
			case "fortune":
			case "lootbonusblocks":
				return Enchantment.LOOT_BONUS_BLOCKS;
			case "loot":
			case "looting":
			case "lootbonusmobs":
				return Enchantment.LOOT_BONUS_MOBS;
			case "water":
			case "waterbreathing":
				return Enchantment.OXYGEN;
			case "prot":
			case "protection":
			case "protectionenvironmental":
				return Enchantment.PROTECTION_ENVIRONMENTAL;
			case "explosive":
			case "explosions":
			case "protexplosive":
			case "protexplosives":
			case "protexplosion":
			case "protexplosions":
			case "explosiveprot":
			case "explosiveprotection":
			case "protectionexplosions":
				return Enchantment.PROTECTION_EXPLOSIONS;
			case "fall":
			case "feather":
			case "falling":
			case "featherfalling":
			case "protectionfall":
				return Enchantment.PROTECTION_FALL;
			case "fireprot":
			case "fireprotection":
			case "protectionfire":
				return Enchantment.PROTECTION_FIRE;
			case "projprot":
			case "projectileprot":
			case "projprotection":
			case "projectileprotection":
			case "arrowprotection":
			case "protectionprojectile":
				return Enchantment.PROTECTION_PROJECTILE;
			case "silk":
			case "silktouch":
				return Enchantment.SILK_TOUCH;
			case "watermine":
			case "watermining":
			case "waterworker":
				return Enchantment.WATER_WORKER;
			default:
				return null;
		}
	}
	
	public static PotionEffectType getPotionEffect(String s) {
		PotionEffectType type = PotionEffectType.getByName(s.toUpperCase());
		if(type != null) return type;
		switch(s.toLowerCase().replace("_", "")) {
			case "hearts":
				return PotionEffectType.ABSORPTION;
			case "blind":
				return PotionEffectType.BLINDNESS;
			case "nausea":
				return PotionEffectType.CONFUSION;
			case "resistence":
			case "resistance":
			case "damageresistence":
			case "damageresistance":
				return PotionEffectType.DAMAGE_RESISTANCE;
			case "haste":
			case "fastdigging":
				return PotionEffectType.FAST_DIGGING;
			case "fireresistence":
			case "fireresistance":
				return PotionEffectType.FIRE_RESISTANCE;
			case "damage":
				return PotionEffectType.HARM;
			case "health":
			case "healthboost":
				return PotionEffectType.HEALTH_BOOST;
			case "strength":
			case "increasedamage":
				return PotionEffectType.INCREASE_DAMAGE;
			case "nightvision":
			case "night":
			case "vision":
				return PotionEffectType.NIGHT_VISION;
			case "regen":
				return PotionEffectType.REGENERATION;
			case "food":
				return PotionEffectType.SATURATION;
			case "slowness":
				return PotionEffectType.SLOW;
			case "slowdigging":
			case "miningfatigue":
				return PotionEffectType.SLOW_DIGGING;
			case "water":
			case "waterbreathing":
			case "aquaaffinity":
				return PotionEffectType.WATER_BREATHING;
			case "weak":
				return PotionEffectType.WEAKNESS;
			default:
				return null;
		}
	}
	
	public static Location stringToLocation(String s) {
		if(s == null || s.isEmpty()) return null;
		String[] args = s.split(",");
		try {
			return new Location(Bukkit.getServer().getWorld(args[0].trim()), Double.parseDouble(args[1].trim()), Double.parseDouble(args[2].trim()),
					Double.parseDouble(args[3].trim()), (float) Double.parseDouble(args[4].trim()), (float) Double.parseDouble(args[5].trim()));
		} catch(NullPointerException e) {
			return new Location(Bukkit.getServer().getWorlds().stream().filter(w -> w.getEnvironment() == Environment.NORMAL).findFirst().get(),
					Double.parseDouble(args[1].trim()), Double.parseDouble(args[2].trim()), Double.parseDouble(args[3].trim()),
					(float) Double.parseDouble(args[4].trim()), (float) Double.parseDouble(args[5].trim()));
		} catch(ArrayIndexOutOfBoundsException e) {
			return new Location(Bukkit.getServer().getWorld(args[0].trim()), Double.parseDouble(args[1].trim()), Double.parseDouble(args[2].trim()),
					Double.parseDouble(args[3].trim()));
		} catch(NumberFormatException e) {
			return null;
		}
	}
	
	public static String locationToString(Location l) {
		try {
			return l.getWorld().getName() + "," + round(l.getX()) + "," + round(l.getY()) + "," + round(l.getZ()) + "," + round(l.getYaw()) + "," + round(l.getPitch());
		} catch(NullPointerException e) {
			return "";
		}
	}
	
	public static String simpleLocationToString(Location l) {
		try {
			return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
		} catch(NullPointerException e) {
			return "";
		}
	}
	
	public static double round(double num) {
		return (int) (num * 100) / 100.0;
	}
	
	public static ItemStack getItem(FileConfiguration config, String path) {
		if(config == null || !config.isConfigurationSection(path)) return null;
		if(!path.endsWith("\\.")) path += ".";
		ItemStack item = new ItemStack(Material.AIR);
		Material mat;
		if((mat = getMaterial(config.getString(path + "material"))) != null) item.setType(mat);
		else if((mat = getMaterial(config.getString(path + "type"))) != null) item.setType(mat);
		if(mat == null || mat == Material.AIR) return null;
		ItemMeta meta = item.getItemMeta() == null ? Bukkit.getItemFactory().getItemMeta(mat) : item.getItemMeta();
		int num;
		String[] smat;
		if((num = config.getInt(path + "amount")) != 0) item.setAmount(num);
		if((num = config.getInt(path + "durability")) != 0) item.setDurability((short) num);
		else if((num = config.getInt(path + "damage")) != 0) item.setDurability((short) num);
		else if((num = config.getInt(path + "data")) != 0) item.setDurability((short) num);
		else if((smat = config.getString(path + "material", "AIR").split(":")).length > 1 && isInt(smat[1]))
			item.setDurability((short) Integer.parseInt(smat[1]));
		String str;
		if(mat == XMaterial.SKELETON_SKULL.parseMaterial() && item.getDurability() == 3 && (str = config.getString(path + "owner")) != null)
			((SkullMeta) meta).setOwner(str);
		if((str = config.getString(path + "name")) != null) meta.setDisplayName(StringEscapeUtils.unescapeJava(color(str)));
		else if((str = config.getString(path + "displayname")) != null) meta.setDisplayName(StringEscapeUtils.unescapeJava(color(str)));
		else if((str = config.getString(path + "display-name")) != null) meta.setDisplayName(StringEscapeUtils.unescapeJava(color(str)));
		List<String> list;
		if(!(list = config.getStringList(path + "lore")).isEmpty())
			meta.setLore(list.stream().map(Util::color).map(StringEscapeUtils::unescapeJava).collect(Collectors.toList()));
		if(config.getBoolean(path + "glow")) {
			meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}
		if(!(list = config.getStringList(path + "enchants")).isEmpty())
			list.stream().filter(s -> getEnchantment(s.split(",")[0]) != null).forEach(s ->
					meta.addEnchant(getEnchantment(s.split(",")[0]), s.split(",").length > 1 && isInt(s.split(",")[1]) ? Integer.parseInt(s.split(",")[1]) : 1, true));
		item.setItemMeta(meta);
		return item;
	}
	
	public static ItemStack replace(ItemStack item, String old, String now) {
		return replace(item, new Files.Pair<>(old, now));
	}
	
	public static ItemStack replace(ItemStack item, String old, List<String> now, String ifEmpty) {
		if(item == null) return null;
		ItemStack copy = item.clone();
		ItemMeta meta = copy.getItemMeta();
		List<String> lore = new ArrayList<>();
		for(String get : meta.getLore()) {
			if(get.contains(old)) {
				if(now.isEmpty()) lore.add(ifEmpty);
				else lore.addAll(now);
				continue;
			}
			lore.add(get);
		}
		meta.setLore(lore);
		copy.setItemMeta(meta);
		return copy;
	}
	
	public static ItemStack replace(ItemStack item, List<Files.Pair<String, String>> pairs) {
		if(item == null) return null;
		ItemStack copy = item.clone();
		ItemMeta meta = copy.getItemMeta();
		if(meta.hasDisplayName()) pairs.forEach(pair -> meta.setDisplayName(meta.getDisplayName().replace(pair.getKey(), pair.getValue())));
		if(meta.hasLore()) meta.setLore(meta.getLore().stream().map(s -> {
			for(Files.Pair<String, String> pair : pairs) s = s.replace(pair.getKey(), pair.getValue());
			return s;
		}).collect(Collectors.toList()));
		copy.setItemMeta(meta);
		return copy;
	}
	
	@SafeVarargs
	public static ItemStack replace(ItemStack item, Files.Pair<String, String>... pairs) {
		return replace(item, Arrays.asList(pairs));
	}
	
	public static ItemStack replaceAll(ItemStack item, String old, String now) {
		if(item == null) return null;
		ItemStack copy = item.clone();
		ItemMeta meta = copy.getItemMeta();
		meta.setDisplayName(meta.getDisplayName().replaceAll(old, now));
		meta.setLore(meta.getLore().stream().map(s -> s.replaceAll(old, now)).collect(Collectors.toList()));
		copy.setItemMeta(meta);
		return copy;
	}
	
	public static ItemStack merge(ItemStack item1, ItemStack item2) {
		ItemStack item = item1.clone();
		ItemMeta meta = item.getItemMeta();
		if(item2 != null && item2.hasItemMeta()) {
			meta.setDisplayName(item2.getItemMeta().getDisplayName());
			meta.setLore(item2.getItemMeta().getLore());
			item.setItemMeta(meta);
			item.addEnchantments(item2.getEnchantments());
		}
		return item;
	}
	
	public static boolean matches(ItemStack item1, ItemStack item2, String... replace) {
		return matches(item1, item2, false, replace);
	}
	
	public static boolean matches(ItemStack item1, ItemStack item2, boolean simple, String... replace) {
		if(item1.getType() != item2.getType()) return false;
		if(item1.getDurability() != item2.getDurability()) return false;
		if(item1.hasItemMeta() ^ item2.hasItemMeta()) return false;
		if(!item1.hasItemMeta() && !item2.hasItemMeta()) return true;
		ItemMeta meta1 = item1.getItemMeta();
		ItemMeta meta2 = item2.getItemMeta();
		if(meta1.hasDisplayName() ^ meta2.hasDisplayName()) return false;
		if(meta1.hasLore() ^ meta2.hasLore()) return false;
		if(meta1.getLore().size() != meta2.getLore().size()) return false;
		if(simple) {
			if(!meta1.getDisplayName().equals(meta2.getDisplayName())) return false;
			if(!meta1.getLore().equals(meta2.getLore())) return false;
			return true;
		}
		if(!meta1.getEnchants().equals(meta2.getEnchants())) return false;
		ItemStack clone = item1.clone();
		ItemMeta cmeta = clone.getItemMeta();
		for(String rep : replace) clone = replace(clone, rep, "+.");
		if(!Pattern.compile(cmeta.getDisplayName()).matcher(meta2.getDisplayName()).matches()) return false;
		for(int i = 0; i < cmeta.getLore().size(); i++) if(!Pattern.compile(cmeta.getLore().get(i)).matcher(meta2.getLore().get(i)).matches()) return false;
		return true;
	}
	
	public static void saveItem(FileConfiguration config, ItemStack item, String path) {
		if(!path.endsWith("\\.")) path += ".";
		config.set(path + "material", item.getType().toString());
		if(item.getAmount() > 1) config.set(path + "amount", item.getAmount());
		if(item.getDurability() != 0) config.set(path + "durability", item.getDurability());
		if(item.hasItemMeta()) {
			if(item.getItemMeta().hasDisplayName()) config.set(path + "name", item.getItemMeta().getDisplayName().replaceAll("§", "&"));
			if(item.getItemMeta().hasLore())
				config.set(path + "lore", item.getItemMeta().getLore().stream().map(s -> s.replaceAll("§", "&")).collect(Collectors.toList()));
			if(item.getItemMeta().hasEnchants()) {
				List<String> enchants = new ArrayList<>();
				item.getItemMeta().getEnchants().forEach((k, v) -> enchants.add(k.getName() + "," + v));
				config.set(path + "enchants", enchants);
			}
		}
	}
	
	public static XInventory getInventory(FileConfiguration config, String path) {
		if(config == null || !config.isConfigurationSection(path)) return null;
		if(!path.endsWith("\\.")) path += ".";
		int size = 36;
		int num;
		if((num = config.getInt(path + "size")) != 0) size = num;
		else if((num = config.getInt(path + "rows")) != 0) size = num * 9;
		size = roundInvSize(size);
		String title = "&4&lInventory";
		String text;
		if((text = config.getString(path + "name")) != null) title = text;
		else if((text = config.getString(path + "title")) != null) title = text;
		Inventory inv = Bukkit.createInventory(null, size, color(title));
		if((text = config.getString(path + "fillers.slot")) != null) {
			ItemStack filler = getItem(config, path + "fillers");
			Arrays.stream(text.split(",")).forEach(key -> {
				if(isInt(key)) inv.setItem(Integer.parseInt(key), filler);
				else if(key.contains("-")) {
					String[] list = key.split("-");
					if(isInt(list[0].trim()) && isInt(list[1].trim()))
						for(int i = Integer.parseInt(list[0].trim()); i <= Integer.parseInt(list[1].trim()); i++) inv.setItem(i, filler);
				} else if(key.equalsIgnoreCase("borders") || key.equalsIgnoreCase("border")) getBorder(inv.getSize()).forEach(i -> inv.setItem(i, filler));
				else if(key.equalsIgnoreCase("all")) for(int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
			});
		}
		if(config.isConfigurationSection(path + "items"))
			for(String key : config.getConfigurationSection(path + "items").getKeys(false))
				inv.setItem(config.getInt(path + "items." + key + ".slot"), getItem(config, path + "items." + key + "."));
		return new XInventory(color(title), inv);
	}
	
	public static LivingEntity spawnMob(FileConfiguration config, String path, Location loc) {
		if(!path.endsWith("\\.")) path += ".";
		EntityType type = getEntity(config.getString(path + "type"));
		if(type == null) return null;
		LivingEntity en = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
		double dub;
		if((dub = config.getDouble(path + "health")) != 0) {
			en.setMaxHealth(dub);
			en.setHealth(dub);
		}
		String text;
		if((text = config.getString(path + "name")) != null) {
			en.setCustomName(color(text));
			en.setCustomNameVisible(true);
		}
		if(en instanceof Ageable)
			if(config.getBoolean(path + "baby"))
				((Ageable) en).setBaby();
			else
				((Ageable) en).setAdult();
		List<String> list;
		String[] list2;
		if(!(list = config.getStringList(path + "potion_effects")).isEmpty())
			for(String s : list)
				if(getPotionEffect((list2 = s.split(","))[0]) != null)
					en.addPotionEffect(new PotionEffect(getPotionEffect(list2[0]), 100000,
							list2.length > 1 && isInt(list2[1]) ? Integer.parseInt(list2[1]) - 1 : 0));
		if(!(list = config.getStringList(path + "armour_enchants")).isEmpty()) {
			ItemStack[] armour = new ItemStack[4];
			armour[0] = new ItemStack(Material.DIAMOND_HELMET);
			armour[1] = new ItemStack(Material.DIAMOND_CHESTPLATE);
			armour[2] = new ItemStack(Material.DIAMOND_LEGGINGS);
			armour[3] = new ItemStack(Material.DIAMOND_BOOTS);
			for(String s : list)
				for(ItemStack item : armour)
					if(getEnchantment((list2 = s.split(","))[0]) != null)
						item.addUnsafeEnchantment(getEnchantment(list2[0]),
								list2.length > 1 && isInt(list2[1]) ? Integer.parseInt(list2[1]) - 1 : 0);
			en.getEquipment().setArmorContents(armour);
			en.getEquipment().setHelmetDropChance(0);
			en.getEquipment().setChestplateDropChance(0);
			en.getEquipment().setLeggingsDropChance(0);
			en.getEquipment().setBootsDropChance(0);
		}
		return en;
	}
	
	public static <T> List<T> getPage(List<T> list, int page, int size) {
		if(page <= 0 || page * size > list.size()) return new ArrayList<>();
		int min = (page - 1) * size;
		int max = Math.min(list.size(), min + size);
		return list.subList(min, max);
	}
	
	public static <T> List<T> getPage(List<T> list, int page) {
		return getPage(list, page, 10);
	}
	
	public static String timeFromMillis(long time) {
		return timeFromMillis(time, "simple");
	}
	
	public static String timeFromMillis(long time, String type) {
		long days = time / 1000 / 60 / 60 / 24;
		long hours = time / 1000 / 60 / 60 - days * 24;
		long minutes = time / 1000 / 60 - days * 24 * 60 - hours * 60;
		long seconds = time / 1000 - days * 24 * 60 * 60 - hours * 60 * 60 - minutes * 60;
		StringBuilder sb = new StringBuilder();
		if(type.equalsIgnoreCase("simple")) {
			if(days >= 1) return sb.append(days).append(" day").append(days > 1 ? "s" : "").toString();
			if(hours >= 1) return sb.append(hours).append(" hour").append(hours > 1 ? "s" : "").toString();
			if(minutes >= 1) return sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").toString();
			if(seconds >= 1) return sb.append(seconds).append(" second").append(seconds > 1 ? "s" : "").toString();
		} else if(type.equalsIgnoreCase("medium")) {
			if(days >= 1) {
				sb.append(days).append(" day").append(days > 1 ? "s" : "").append(" ");
				if(hours >= 1) sb.append(hours).append(" hour").append(hours > 1 ? "s" : "");
				return sb.toString().trim();
			}
			if(hours >= 1) {
				sb.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(" ");
				if(minutes >= 1) sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
				return sb.toString().trim();
			}
			if(minutes >= 1) {
				sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(" ");
				if(seconds >= 1) sb.append(seconds).append(" second").append(seconds > 1 ? "s" : "");
				return sb.toString().trim();
			}
			if(seconds >= 1) sb.append(seconds).append(" second").append(seconds > 1 ? "s" : "");
		} else if(type.equalsIgnoreCase("tiny")) {
			if(days >= 1) {
				sb.append(days).append("d ");
				if(hours >= 1) sb.append(hours).append("h");
				return sb.toString().trim();
			}
			if(hours >= 1) {
				sb.append(hours).append("h ");
				if(minutes >= 1) sb.append(minutes).append("m");
				return sb.toString().trim();
			}
			if(minutes >= 1) {
				sb.append(minutes).append("m ");
				if(seconds >= 1) sb.append(seconds).append("s");
				return sb.toString().trim();
			}
			if(seconds >= 1) sb.append(seconds).append("s");
		} else if(type.equalsIgnoreCase("small")) {
			if(days >= 1) sb.append(days).append("d ");
			if(hours >= 1) sb.append(hours).append("h ");
			if(minutes >= 1) sb.append(minutes).append("m ");
			if(seconds >= 1) sb.append(seconds).append("s");
		} else {
			if(days >= 1) sb.append(days).append(" day").append(days > 1 ? "s" : "").append(" ");
			if(hours >= 1) sb.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(" ");
			if(minutes >= 1) sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(" ");
			if(seconds >= 1) sb.append(seconds).append(" second").append(seconds > 1 ? "s" : "");
		}
		return sb.toString().isEmpty() ? "now" : sb.toString().trim();
	}
	
	public static String timeFromSec(long time) {
		return timeFromMillis(time * 1000);
	}
	
	public static List<Integer> getBorder(int size) {
		switch(roundInvSize(size)) {
			case 54:
				return Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53);
			case 45:
				return Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44);
			case 36:
				return Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35);
			case 27:
				return Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26);
			default:
				return Collections.emptyList();
		}
	}
	
	public static int roundInvSize(int size) {
		while(size % 9 != 0) size++;
		return Math.min(54, Math.max(9, size));
	}
	
	public static int getExperience(Player player) {
		int ver = Integer.parseInt(Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].split("_")[1]);
		int exp = Math.round(getExp(ver, player.getLevel()) * player.getExp());
		int currentLevel = player.getLevel();
		while(currentLevel-- > 0) exp += getExp(ver, currentLevel);
		return Math.max(0, exp);
	}
	
	private static int getExp(int version, int level) {
		return version >= 8 ? getLevelExpNew(level) : getLevelExpOld(level);
	}
	
	public static void setExperience(Player player, int amount) {
		player.setExp(0);
		player.setLevel(0);
		player.setTotalExperience(0);
		player.giveExp(amount);
	}
	
	private static int getLevelExpOld(int level) {
		return level >= 30 ? 62 + (level - 30) * 7 : level >= 15 ? 17 + (level - 15) * 3 : 17;
	}
	
	public static int getLevelExpNew(int level) {
		return level < 16 ? level * 2 + 7 : level < 31 ? level * 5 - 38 : level * 9 - 158;
	}
	
	public static boolean isArmour(Material m) {
		return Enchantment.PROTECTION_ENVIRONMENTAL.canEnchantItem(new ItemStack(m));
	}
	
	public static boolean isWeapon(Material m) {
		return Enchantment.DAMAGE_ALL.canEnchantItem(new ItemStack(m));
	}
	
	public static boolean isTool(Material m) {
		return Enchantment.DIG_SPEED.canEnchantItem(new ItemStack(m));
	}
	
	public static String getName(EntityType e) {
		return caps(e.toString()).replaceAll("_", " ");
	}
	
	public static EntityType getEntity(String e) {
		e = e.replaceAll(" ", "_").toUpperCase();
		try {
			return EntityType.valueOf(e);
		} catch(Exception ex) {
			return null;
		}
	}
	
	public static Class<?> getOBCClass(String name) {
		try {
			return Class.forName("org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + name);
		} catch(ClassNotFoundException e) {
			return null;
		}
	}
	
	public static Class<?> getNMSClass(String name) {
		try {
			return Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + name);
		} catch(ClassNotFoundException e) {
			return null;
		}
	}
	
	public static Class<?> getNMSClass(String name, String def) {
		return getNMSClass(name) != null ? getNMSClass(name) : getNMSClass(def.split("\\.")[0]).getDeclaredClasses()[0];
	}
	
	@Getter
	@AllArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	public static class XInventory {
		String title;
		Inventory inventory;
	}
}