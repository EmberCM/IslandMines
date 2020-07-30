package us.creepermc.mines.objects;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.bukkit.material.MaterialData;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ToString
public class Upgrade {
	String id;
	double price;
	int blocks;
	MaterialData data;
	
	public String getPrettyId() {
		return id.replace("_", " ");
	}
}