package us.creepermc.mines.managers;

import net.brcdev.shopgui.ShopGuiPlusApi;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ShopGUIPlusHook {
	public static double getWorth(Player player, ItemStack item) {
		try {
			return Math.max(ShopGuiPlusApi.getItemStackPriceSell(player, item), 0);
		} catch(Exception ex) {
			return 0;
		}
	}
}