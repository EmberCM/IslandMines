package us.creepermc.mines.utils;

import org.bukkit.inventory.ItemStack;

public class NBTEditor {
	public static ItemStack set(ItemStack item, String key, Object value, Class<?> type) {
		try {
			if(item == null) return null;
			Object copy = Util.getOBCClass("inventory.CraftItemStack").getDeclaredMethod("asNMSCopy", ItemStack.class).invoke(null, item);
			boolean has = (boolean) copy.getClass().getDeclaredMethod("hasTag").invoke(copy);
			Object tag = copy.getClass().getDeclaredMethod("getTag").invoke(copy);
			Object cmpd = has ? tag : Util.getNMSClass("NBTTagCompound").getConstructor().newInstance();
			cmpd.getClass().getDeclaredMethod("set" + Util.caps(type.getSimpleName()), String.class, type).invoke(cmpd, key, value);
			copy.getClass().getDeclaredMethod("setTag", Util.getNMSClass("NBTTagCompound")).invoke(copy, cmpd);
			return (ItemStack) Util.getOBCClass("inventory.CraftItemStack").getDeclaredMethod("asBukkitCopy", Util.getNMSClass("ItemStack")).invoke(null, copy);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static ItemStack set(ItemStack item, String key, Object value) {
		return set(item, key, value, String.class);
	}
	
	public static <T> T get(ItemStack item, String key, Class<T> type) {
		try {
			if(item == null) return type == String.class ? (T) "" : null;
			Object copy = Util.getOBCClass("inventory.CraftItemStack").getDeclaredMethod("asNMSCopy", ItemStack.class).invoke(null, item);
			if(!((boolean) copy.getClass().getDeclaredMethod("hasTag").invoke(copy))) return type == String.class ? (T) "" : null;
			Object tag = copy.getClass().getDeclaredMethod("getTag").invoke(copy);
			if(!((boolean) tag.getClass().getDeclaredMethod("hasKey", String.class).invoke(tag, key))) return type == String.class ? (T) "" : null;
			return (T) tag.getClass().getMethod("get" + Util.caps(type.getSimpleName()), String.class).invoke(tag, key);
		} catch(Exception e) {
			e.printStackTrace();
			return type == String.class ? (T) "" : null;
		}
	}
	
	public static String get(ItemStack item, String key) {
		return get(item, key, String.class);
	}
}