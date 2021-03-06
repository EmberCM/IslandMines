package us.creepermc.mines.managers;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import org.bukkit.Location;
import us.creepermc.mines.Core;
import us.creepermc.mines.objects.PlayerMine;
import us.creepermc.mines.utils.Util;

public class HolographicDisplaysHook {
	private static Hologram createHologram(Core core, PlayerMine mine) {
		Hologram hologram = HologramsAPI.createHologram(core, mine.getCenterLocation());
		String cooldown = Util.timeFromMillis(mine.getCooldown(), "medium");
		String life = Util.timeFromMillis(mine.getLifeLeft(), "medium");
		mine.getMine().getHologramText().forEach(line -> hologram.appendTextLine(line.replace("{time}", cooldown).replace("{life}", life)));
		return hologram;
	}
	
	public static void updateHologram(Core core, PlayerMine mine) {
		Hologram hologram = getHologram(core, mine);
		if(hologram == null) hologram = createHologram(core, mine);
		int resetDelay = mine.getMine().getAutomaticReset();
		long difference = System.currentTimeMillis() - mine.getTimePlaced();
		while(difference >= resetDelay) difference -= resetDelay;
		String cooldown = Util.timeFromMillis(resetDelay - difference, "medium");
		String life = Util.timeFromMillis(mine.getLifeLeft(), "medium");
		for(int i = 0; i < mine.getMine().getHologramText().size(); i++)
			((TextLine) hologram.getLine(i)).setText(mine.getMine().getHologramText().get(i).replace("{time}", cooldown).replace("{life}", life));
	}
	
	public static void deleteHologram(Core core, PlayerMine mine) {
		Hologram hologram = getHologram(core, mine);
		if(hologram == null) return;
		hologram.delete();
	}
	
	private static Hologram getHologram(Core core, PlayerMine mine) {
		Location center = mine.getCenterLocation();
		return HologramsAPI.getHolograms(core).stream()
				.filter(hologram -> hologram.getLocation().getWorld().equals(center.getWorld()) && hologram.getLocation().distance(center) <= 2)
				.findFirst().orElse(null);
	}
}