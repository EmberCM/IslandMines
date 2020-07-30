package us.creepermc.mines.objects;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Mine {
	String id;
	int size;
	int height;
	long lifeSpan;
	int automaticReset;
	int resetDelay;
	List<String> signText;
	ItemStack item;
	List<Upgrade> upgrades;
	
	public String getPrettyId() {
		return id.replace("_", " ");
	}
}