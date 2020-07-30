package us.creepermc.mines.templates;

import lombok.Getter;
import org.bukkit.event.Listener;
import us.creepermc.mines.Core;

@Getter
public abstract class XListener extends XManager implements Listener {
	public XListener(Core core) {
		super(core);
	}
}