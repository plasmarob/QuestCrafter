package me.plasmarob.questcrafter.util;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class BlockChangeHelper {
	
	public static void changeByBlock(World w) {
		
	}
	
	@SuppressWarnings("deprecation")
	public static void fakeChangeByBlock(Player p, List<Location> loc, Material mat) {
		//World w = p.getWorld();
		for (Location l : loc) {
			p.sendBlockChange(l, mat, (byte)0);
		}
		
		
	}
}
