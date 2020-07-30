package us.creepermc.mines.managers;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SuperiorSkyblockHook {
	public static boolean isAllowed(Player player, Location location) {
		Island island = SuperiorSkyblockAPI.getIslandAt(location);
		return island != null && island.isMember(SuperiorSkyblockAPI.getPlayer(player));
	}
}