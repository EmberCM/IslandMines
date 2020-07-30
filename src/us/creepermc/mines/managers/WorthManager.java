package us.creepermc.mines.managers;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import us.creepermc.mines.Core;
import us.creepermc.mines.templates.XManager;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorthManager extends XManager {
	boolean usingEssentials;
	boolean usingShopGUIPlus;
	
	public WorthManager(Core core) {
		super(core);
	}
	
	@Override
	public void initialize() {
		deinitialize();
		
		usingEssentials = getCore().getServer().getPluginManager().isPluginEnabled("Essentials");
		usingShopGUIPlus = getCore().getServer().getPluginManager().isPluginEnabled("ShopGUIPlus");
	}
	
	@Override
	public void deinitialize() {
		usingEssentials = false;
		usingShopGUIPlus = false;
	}
	
	public double getWorth(Player player, MaterialData data) {
		if(usingShopGUIPlus) return ShopGUIPlusHook.getWorth(player, new ItemStack(data.getItemType(), 1, data.getData()));
		if(usingEssentials) return EssentialsHook.getWorth(getCore(), new ItemStack(data.getItemType(), 1, data.getData()));
		return 0;
	}
}