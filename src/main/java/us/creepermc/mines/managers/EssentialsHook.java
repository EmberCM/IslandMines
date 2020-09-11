package us.creepermc.mines.managers;

import com.earth2me.essentials.Essentials;
import org.bukkit.inventory.ItemStack;
import us.creepermc.mines.Core;

public class EssentialsHook {
	public static double getWorth(Core core, ItemStack item) {
		Essentials essentials = (Essentials) core.getServer().getPluginManager().getPlugin("Essentials");
		try {
			// Essentials 2.16+
			return essentials.getWorth().getPrice(essentials, item).doubleValue();
		} catch(NullPointerException | NoSuchMethodError ex) {
			// Essentials 2.15-
			try {
				Object worthO = essentials.getClass().getMethod("getWorth").invoke(essentials);
				Object price = worthO.getClass().getMethod("getPrice", ItemStack.class).invoke(worthO, item);
				return (double) price.getClass().getMethod("doubleValue").invoke(price);
			} catch(Exception ex2) {
				ex2.printStackTrace();
				return 0;
			}
		}
	}
}