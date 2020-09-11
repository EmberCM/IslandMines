package us.creepermc.mines.templates;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import us.creepermc.mines.Core;

import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class XCommand extends XManager implements CommandExecutor, TabExecutor {
	String command;
	
	public XCommand(Core core, String command) {
		super(core);
		this.command = command;
	}
	
	public XCommand(Core core) {
		super(core);
		String name = getClass().getSimpleName().toLowerCase();
		this.command = name.substring(0, name.length() - 3);
	}
	
	public abstract void execute(CommandSender sender, String[] args);
	
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return null;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!command.getName().equals(this.command)) return false;
		execute(sender, args);
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if(!command.getName().equals(this.command)) return null;
		return tabComplete(sender, args);
	}
}