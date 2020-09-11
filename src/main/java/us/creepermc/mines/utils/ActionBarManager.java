package us.creepermc.mines.utils;

import org.bukkit.entity.Player;

public class ActionBarManager {
	public static void sendActionText(Player player, String message) {
		try {
			Object entity = Util.getOBCClass("entity.CraftPlayer").cast(player);
			Object handle = entity.getClass().getMethod("getHandle").invoke(entity);
			Object connection = handle.getClass().getField("playerConnection").get(handle);
			Object chatText = Util.getNMSClass("ChatComponentText").getConstructor(String.class).newInstance(message);
			Object packet;
			try {
				packet = Util.getNMSClass("PacketPlayOutChat").getConstructor(Util.getNMSClass("IChatBaseComponent"), byte.class).newInstance(chatText, (byte) 2);
			} catch(Exception ex2) {
				packet = Util.getNMSClass("PacketPlayOutChat").getConstructor(Util.getNMSClass("IChatBaseComponent"), Util.getNMSClass("ChatMessageType")).newInstance(chatText, Enum.valueOf((Class<? extends Enum>) Util.getNMSClass("ChatMessageType"), "GAME_INFO"));
			}
			connection.getClass().getMethod("sendPacket", Util.getNMSClass("Packet")).invoke(connection, packet);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
}