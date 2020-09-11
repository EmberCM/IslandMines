package us.creepermc.mines.templates;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import us.creepermc.mines.Core;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class XManager {
	Core core;
	
	public void initialize() {
	}
	
	public void deinitialize() {
	}
}