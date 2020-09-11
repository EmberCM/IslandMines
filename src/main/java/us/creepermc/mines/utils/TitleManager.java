package us.creepermc.mines.utils;

import org.bukkit.entity.Player;

public class TitleManager {
	public static void sendTitle(Player player, Integer fadeIn, Integer stay, Integer fadeOut, String title, String subtitle) {
		try {
			Object entity = Util.getOBCClass("entity.CraftPlayer").cast(player);
			Object handle = entity.getClass().getMethod("getHandle").invoke(entity);
			Object connection = handle.getClass().getField("playerConnection").get(handle);
			Class<?> enumClass = Util.getNMSClass("EnumTitleAction", "PacketPlayOutTitle.EnumTitleAction");
			Object cbc = Util.getNMSClass("IChatBaseComponent").getDeclaredClasses().length != 0 ? Util.getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getDeclaredMethod("a", String.class).invoke(null, "") : null;
			Object packet = Util.getNMSClass("PacketPlayOutTitle")
					.getConstructor(enumClass, Util.getNMSClass("IChatBaseComponent"), int.class, int.class, int.class)
					.newInstance(enumClass.getDeclaredMethod("a", String.class).invoke(null, "TIMES"), cbc, fadeIn, stay, fadeOut);
			connection.getClass().getMethod("sendPacket", Util.getNMSClass("Packet")).invoke(connection, packet);
			sendPacket(player, title, "TITLE", connection, enumClass);
			sendPacket(player, subtitle, "SUBTITLE", connection, enumClass);
		} catch(Exception ex) {
			try {
				player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class).invoke(player, Util.color(title), Util.color(subtitle), fadeIn, stay, fadeOut);
			} catch(Exception ex2) {
				ex2.printStackTrace();
			}
		}
	}
	
	private static void sendPacket(Player player, String text, String type, Object connection, Class<?> enumClass) throws Exception {
		text = "{\"text\": \"" + Util.color(text.replaceAll("%player%", player.getDisplayName())) + "\"}";
		Object json = Util.getNMSClass("ChatSerializer", "IChatBaseComponent.ChatSerializer").getMethod("a", String.class).invoke(null, text);
		Object titlePacket = Util.getNMSClass("PacketPlayOutTitle").getConstructor(enumClass, Util.getNMSClass("IChatBaseComponent"))
				.newInstance(enumClass.getDeclaredMethod("a", String.class).invoke(null, type), json);
		connection.getClass().getMethod("sendPacket", Util.getNMSClass("Packet")).invoke(connection, titlePacket);
	}
}