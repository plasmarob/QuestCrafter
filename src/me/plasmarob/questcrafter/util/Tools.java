package me.plasmarob.questcrafter.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.plasmarob.questcrafter.QuestCrafter;
import me.plasmarob.questcrafter.qblock.Receiver;
import me.plasmarob.questcrafter.qblock.Sender;
//import me.plasmarob.questcrafter.qblock.Receiver;
//import me.plasmarob.questcrafter.qblock.Sender;
import net.md_5.bungee.api.ChatColor;

public class Tools {
	
	private static List<BlockFace> faces;
	public static List<BlockFace> faces()
	{
		if (faces == null) {
			faces = new ArrayList<BlockFace>();
			faces.add(BlockFace.UP);
			faces.add(BlockFace.DOWN);
			faces.add(BlockFace.NORTH);
			faces.add(BlockFace.SOUTH);
			faces.add(BlockFace.EAST);
			faces.add(BlockFace.WEST);
		}
		return faces;
	}
	
	private static List<Material> clearblocks;
	public static boolean canSeeThrough(Material mat) {
		if (clearblocks == null) {
			clearblocks = new ArrayList<Material>();
			clearblocks.add(Material.TORCH);
			clearblocks.add(Material.AIR);
			clearblocks.add(Material.REDSTONE);
			clearblocks.add(Material.REDSTONE_WIRE);
			clearblocks.add(Material.SPRUCE_FENCE);
			clearblocks.add(Material.SPRUCE_FENCE_GATE);
			clearblocks.add(Material.BIRCH_FENCE);
			clearblocks.add(Material.BIRCH_FENCE_GATE);
			clearblocks.add(Material.JUNGLE_FENCE);
			clearblocks.add(Material.JUNGLE_FENCE_GATE);
			clearblocks.add(Material.ACACIA_FENCE);
			clearblocks.add(Material.ACACIA_FENCE_GATE);
			clearblocks.add(Material.DARK_OAK_FENCE);
			clearblocks.add(Material.DARK_OAK_FENCE_GATE);
			clearblocks.add(Material.BREWING_STAND);
			clearblocks.add(Material.SIGN);
			clearblocks.add(Material.WALL_SIGN);
			clearblocks.add(Material.STONE_BUTTON);
			clearblocks.add(Material.ACACIA_BUTTON);
			clearblocks.add(Material.BIRCH_BUTTON);
			clearblocks.add(Material.DARK_OAK_BUTTON);
			clearblocks.add(Material.JUNGLE_BUTTON);
			clearblocks.add(Material.OAK_BUTTON);
			clearblocks.add(Material.SPRUCE_BUTTON);
			clearblocks.add(Material.TRIPWIRE);
			clearblocks.add(Material.TRIPWIRE_HOOK);
			clearblocks.add(Material.COBWEB);
		}
		return clearblocks.contains(mat);
	}
	
	public static void showLine(World world, Sender send, Receiver rec) {
		new LineConnectEffect(QuestCrafter.getEffectManager(), 
				new Location(world, send.getX(), send.getY(), send.getZ()), 
				new Location(world, rec.getX(), rec.getY(), rec.getZ()), 
				Color.fromRGB(0, 255, 0), Color.fromRGB(0, 0, 255)).start();
	}
	
	public static void showLine(World world, Block send, Block rec) {
		new LineConnectEffect(QuestCrafter.getEffectManager(), 
				new Location(world, send.getX(), send.getY(), send.getZ()), 
				new Location(world, rec.getX(), rec.getY(), rec.getZ()), 
				Color.fromRGB(0, 255, 0), Color.fromRGB(0, 0, 255)).start();
	}
	public static void showLine(World world, Block send, Block rec, int r, int g, int b) {
		new LineConnectEffect(QuestCrafter.getEffectManager(), 
				new Location(world, send.getX(), send.getY(), send.getZ()), 
				new Location(world, rec.getX(), rec.getY(), rec.getZ()), 
				Color.fromRGB(r, g, b), Color.fromRGB(r, g, b)).start();
	}
	
	public static void outline(Player player, Location min, Location max, BlockData frame, BlockData corners, int seconds) {
		int mx = min.getBlockX();
		int my = min.getBlockY();
		int mz = min.getBlockZ();
		int MX = max.getBlockX();
		int MY = max.getBlockY();
		int MZ = max.getBlockZ();
		
		// Fake outline of dungeon
		for (int x = min.getBlockX()+1; x < max.getBlockX(); x++) {
			player.sendBlockChange(new Location(player.getWorld(), x, my, mz), frame);
			player.sendBlockChange(new Location(player.getWorld(), x, MY, mz), frame);
			player.sendBlockChange(new Location(player.getWorld(), x, my, MZ), frame);
			player.sendBlockChange(new Location(player.getWorld(), x, MY, MZ), frame);
		}
		for (int y = min.getBlockY()+1; y < max.getBlockY(); y++) {
			player.sendBlockChange(new Location(player.getWorld(), mx, y, mz), frame);
			player.sendBlockChange(new Location(player.getWorld(), MX, y, mz), frame);
			player.sendBlockChange(new Location(player.getWorld(), mx, y, MZ), frame);
			player.sendBlockChange(new Location(player.getWorld(), MX, y, MZ), frame);
		}
		for (int z = min.getBlockZ()+1; z < max.getBlockZ(); z++) {
			player.sendBlockChange(new Location(player.getWorld(), mx, my, z), frame);
			player.sendBlockChange(new Location(player.getWorld(), MX, my, z), frame);
			player.sendBlockChange(new Location(player.getWorld(), mx, MY, z), frame);
			player.sendBlockChange(new Location(player.getWorld(), MX, MY, z), frame);
		}
		player.sendBlockChange(new Location(player.getWorld(), mx, my, mz), corners);
		player.sendBlockChange(new Location(player.getWorld(), mx, my, MZ), corners);
		player.sendBlockChange(new Location(player.getWorld(), mx, MY, mz), corners);
		player.sendBlockChange(new Location(player.getWorld(), mx, MY, MZ), corners);
		player.sendBlockChange(new Location(player.getWorld(), MX, my, mz), corners);
		player.sendBlockChange(new Location(player.getWorld(), MX, my, MZ), corners);
		player.sendBlockChange(new Location(player.getWorld(), MX, MY, mz), corners);
		player.sendBlockChange(new Location(player.getWorld(), MX, MY, MZ), corners);	
		
		if(seconds > 0 && seconds < 60) {
			new BukkitRunnable() {
	            @Override
	            public void run() {
	            	for (int x = min.getBlockX()+1; x < max.getBlockX(); x++) {
	            		Location loc1 = new Location(player.getWorld(), x, my, mz);
	            		Location loc2 = new Location(player.getWorld(), x, my, MZ);
	            		Location loc3 = new Location(player.getWorld(), x, MY, mz);
	            		Location loc4 = new Location(player.getWorld(), x, MY, MZ);
	        			player.sendBlockChange(loc1, loc1.getBlock().getBlockData());
	        			player.sendBlockChange(loc2, loc2.getBlock().getBlockData());
	        			player.sendBlockChange(loc3, loc3.getBlock().getBlockData());
	        			player.sendBlockChange(loc4, loc4.getBlock().getBlockData());
	        		}
	        		for (int y = min.getBlockY()+1; y < max.getBlockY(); y++) {
	        			Location loc1 = new Location(player.getWorld(), mx, y, mz);
	            		Location loc2 = new Location(player.getWorld(), mx, y, MZ);
	            		Location loc3 = new Location(player.getWorld(), MX, y, mz);
	            		Location loc4 = new Location(player.getWorld(), MX, y, MZ);
	        			player.sendBlockChange(loc1, loc1.getBlock().getBlockData());
	        			player.sendBlockChange(loc2, loc2.getBlock().getBlockData());
	        			player.sendBlockChange(loc3, loc3.getBlock().getBlockData());
	        			player.sendBlockChange(loc4, loc4.getBlock().getBlockData());
	        		}
	        		for (int z = min.getBlockZ()+1; z < max.getBlockZ(); z++) {
	        			Location loc1 = new Location(player.getWorld(), mx, my, z);
	            		Location loc2 = new Location(player.getWorld(), mx, MY, z);
	            		Location loc3 = new Location(player.getWorld(), MX, my, z);
	            		Location loc4 = new Location(player.getWorld(), MX, MY, z);
	        			player.sendBlockChange(loc1, loc1.getBlock().getBlockData());
	        			player.sendBlockChange(loc2, loc2.getBlock().getBlockData());
	        			player.sendBlockChange(loc3, loc3.getBlock().getBlockData());
	        			player.sendBlockChange(loc4, loc4.getBlock().getBlockData());
	        		}
	        		Location loc1 = new Location(player.getWorld(), mx, my, mz);
	        		Location loc2 = new Location(player.getWorld(), mx, my, MZ);
	        		Location loc3 = new Location(player.getWorld(), mx, MY, mz);
	        		Location loc4 = new Location(player.getWorld(), mx, MY, MZ);
	        		Location loc5 = new Location(player.getWorld(), MX, my, mz);
	        		Location loc6 = new Location(player.getWorld(), MX, my, MZ);
	        		Location loc7 = new Location(player.getWorld(), MX, MY, mz);
	        		Location loc8 = new Location(player.getWorld(), MX, MY, MZ);
	        		player.sendBlockChange(loc1, loc1.getBlock().getBlockData());
	        		player.sendBlockChange(loc2, loc2.getBlock().getBlockData());
	        		player.sendBlockChange(loc3, loc3.getBlock().getBlockData());
	        		player.sendBlockChange(loc4, loc4.getBlock().getBlockData());
	        		player.sendBlockChange(loc5, loc5.getBlock().getBlockData());
	        		player.sendBlockChange(loc6, loc6.getBlock().getBlockData());
	        		player.sendBlockChange(loc7, loc7.getBlock().getBlockData());
	        		player.sendBlockChange(loc8, loc8.getBlock().getBlockData());
	            }
	        }.runTaskLater(JavaPlugin.getPlugin(QuestCrafter.class), 20*seconds);
		}
	}
	
	/*
	public static void outline(Player player, Location min, Location max, Material frame, Material corners, int seconds) {
		// Fake outline of dungeon
		for (int x = min.getBlockX()+1; x < max.getBlockX(); x++) {
			player.sendBlockChange(new Location(player.getWorld(), x, min.getBlockY(),min.getBlockZ()), frame, (byte)0);
			player.sendBlockChange(new Location(player.getWorld(), x, max.getBlockY(),min.getBlockZ()), frame, (byte)0);
			player.sendBlockChange(new Location(player.getWorld(), x, min.getBlockY(),max.getBlockZ()), frame, (byte)0);
			player.sendBlockChange(new Location(player.getWorld(), x, max.getBlockY(),max.getBlockZ()), frame, (byte)0);
		}
		for (int y = min.getBlockY()+1; y < max.getBlockY(); y++) {
			player.sendBlockChange(new Location(player.getWorld(), min.getBlockX(), y, min.getBlockZ()), frame, (byte)0);
			player.sendBlockChange(new Location(player.getWorld(), max.getBlockX(), y, min.getBlockZ()), frame, (byte)0);
			player.sendBlockChange(new Location(player.getWorld(), min.getBlockX(), y, max.getBlockZ()), frame, (byte)0);
			player.sendBlockChange(new Location(player.getWorld(), max.getBlockX(), y, max.getBlockZ()), frame, (byte)0);
		}
		for (int z = min.getBlockZ()+1; z < max.getBlockZ(); z++) {
			player.sendBlockChange(new Location(player.getWorld(), min.getBlockX(), min.getBlockY(), z), frame, (byte)0);
			player.sendBlockChange(new Location(player.getWorld(), max.getBlockX(), min.getBlockY(), z), frame, (byte)0);
			player.sendBlockChange(new Location(player.getWorld(), min.getBlockX(), max.getBlockY(), z), frame, (byte)0);
			player.sendBlockChange(new Location(player.getWorld(), max.getBlockX(), max.getBlockY(), z), frame, (byte)0);
		}
		player.sendBlockChange(new Location(player.getWorld(), min.getBlockX(), min.getBlockY(),min.getBlockZ()), corners, (byte)0);
		player.sendBlockChange(new Location(player.getWorld(), min.getBlockX(), min.getBlockY(),max.getBlockZ()), corners, (byte)0);
		player.sendBlockChange(new Location(player.getWorld(), min.getBlockX(), max.getBlockY(),min.getBlockZ()), corners, (byte)0);
		player.sendBlockChange(new Location(player.getWorld(), min.getBlockX(), max.getBlockY(),max.getBlockZ()), corners, (byte)0);
		player.sendBlockChange(new Location(player.getWorld(), max.getBlockX(), min.getBlockY(),min.getBlockZ()), corners, (byte)0);
		player.sendBlockChange(new Location(player.getWorld(), max.getBlockX(), min.getBlockY(),max.getBlockZ()), corners, (byte)0);
		player.sendBlockChange(new Location(player.getWorld(), max.getBlockX(), max.getBlockY(),min.getBlockZ()), corners, (byte)0);
		player.sendBlockChange(new Location(player.getWorld(), max.getBlockX(), max.getBlockY(),max.getBlockZ()), corners, (byte)0);	
		
		if(seconds > 0 && seconds < 60) {
			new BukkitRunnable() {
	            @Override
	            public void run() {
	            	for (int x = min.getBlockX()+1; x < max.getBlockX(); x++) {
	            		Location loc1 = new Location(player.getWorld(), x, min.getBlockY(),min.getBlockZ());
	            		Location loc2 = new Location(player.getWorld(), x, min.getBlockY(),max.getBlockZ());
	            		Location loc3 = new Location(player.getWorld(), x, max.getBlockY(),min.getBlockZ());
	            		Location loc4 = new Location(player.getWorld(), x, max.getBlockY(),max.getBlockZ());
	        			player.sendBlockChange(loc1, loc1.getBlock().getType(), loc1.getBlock().getData());
	        			player.sendBlockChange(loc2, loc2.getBlock().getType(), loc2.getBlock().getData());
	        			player.sendBlockChange(loc3, loc3.getBlock().getType(), loc3.getBlock().getData());
	        			player.sendBlockChange(loc4, loc4.getBlock().getType(), loc4.getBlock().getData());
	        		}
	        		for (int y = min.getBlockY()+1; y < max.getBlockY(); y++) {
	        			Location loc1 = new Location(player.getWorld(), min.getBlockX(), y, min.getBlockZ());
	            		Location loc2 = new Location(player.getWorld(), min.getBlockX(), y, max.getBlockZ());
	            		Location loc3 = new Location(player.getWorld(), max.getBlockX(), y, min.getBlockZ());
	            		Location loc4 = new Location(player.getWorld(), max.getBlockX(), y, max.getBlockZ());
	        			player.sendBlockChange(loc1, loc1.getBlock().getType(), loc1.getBlock().getData());
	        			player.sendBlockChange(loc2, loc2.getBlock().getType(), loc2.getBlock().getData());
	        			player.sendBlockChange(loc3, loc3.getBlock().getType(), loc3.getBlock().getData());
	        			player.sendBlockChange(loc4, loc4.getBlock().getType(), loc4.getBlock().getData());
	        		}
	        		for (int z = min.getBlockZ()+1; z < max.getBlockZ(); z++) {
	        			Location loc1 = new Location(player.getWorld(), min.getBlockX(), min.getBlockY(), z);
	            		Location loc2 = new Location(player.getWorld(), min.getBlockX(), max.getBlockY(), z);
	            		Location loc3 = new Location(player.getWorld(), max.getBlockX(), min.getBlockY(), z);
	            		Location loc4 = new Location(player.getWorld(), max.getBlockX(), max.getBlockY(), z);
	        			player.sendBlockChange(loc1, loc1.getBlock().getType(), loc1.getBlock().getData());
	        			player.sendBlockChange(loc2, loc2.getBlock().getType(), loc2.getBlock().getData());
	        			player.sendBlockChange(loc3, loc3.getBlock().getType(), loc3.getBlock().getData());
	        			player.sendBlockChange(loc4, loc4.getBlock().getType(), loc4.getBlock().getData());
	        		}
	        		Location loc1 = new Location(player.getWorld(), min.getBlockX(), min.getBlockY(), min.getBlockZ());
	        		Location loc2 = new Location(player.getWorld(), min.getBlockX(), min.getBlockY(), max.getBlockZ());
	        		Location loc3 = new Location(player.getWorld(), min.getBlockX(), max.getBlockY(), min.getBlockZ());
	        		Location loc4 = new Location(player.getWorld(), min.getBlockX(), max.getBlockY(), max.getBlockZ());
	        		Location loc5 = new Location(player.getWorld(), max.getBlockX(), min.getBlockY(), min.getBlockZ());
	        		Location loc6 = new Location(player.getWorld(), max.getBlockX(), min.getBlockY(), max.getBlockZ());
	        		Location loc7 = new Location(player.getWorld(), max.getBlockX(), max.getBlockY(), min.getBlockZ());
	        		Location loc8 = new Location(player.getWorld(), max.getBlockX(), max.getBlockY(), max.getBlockZ());
	        		player.sendBlockChange(loc1, loc1.getBlock().getType(), loc1.getBlock().getData());
	        		player.sendBlockChange(loc2, loc2.getBlock().getType(), loc2.getBlock().getData());
	        		player.sendBlockChange(loc3, loc3.getBlock().getType(), loc3.getBlock().getData());
	        		player.sendBlockChange(loc4, loc4.getBlock().getType(), loc4.getBlock().getData());
	        		player.sendBlockChange(loc5, loc5.getBlock().getType(), loc5.getBlock().getData());
	        		player.sendBlockChange(loc6, loc6.getBlock().getType(), loc6.getBlock().getData());
	        		player.sendBlockChange(loc7, loc7.getBlock().getType(), loc7.getBlock().getData());
	        		player.sendBlockChange(loc8, loc8.getBlock().getType(), loc8.getBlock().getData());	
	            }
	        }.runTaskLater(JavaPlugin.getPlugin(QuestCrafter.class), 20*seconds);
		}
	}
	*/
	
	//Efficient transformation of locations into ,,; separated string
	public static String locationSetAsString(Set<Location> locs) {
		if(locs.size()==0) return "";
		StringBuilder sb = new StringBuilder();
		Iterator<Location> it = locs.iterator();
		Location l;
		
		l = it.next();
		sb.append(l.getBlockX());
		sb.append(",");
		sb.append(l.getBlockY());
		sb.append(",");
		sb.append(l.getBlockZ());
		while(it.hasNext()) {
			l = it.next();
			sb.append(";"+l.getBlockX());
			sb.append(","+l.getBlockY());
			sb.append(","+l.getBlockZ());
		}
		return sb.toString();
	}
	public static HashSet<Location> locationSetFromString(World w, String str) {
		String[] locA = str.split(";");
		HashSet<Location> locs = new HashSet<Location>();
		String[] sA;
		for(int i = 0; i < locA.length; i++) {
			sA = xyzFromString(locA[i]);
			locs.add(new Location(w,Integer.parseInt(sA[0]),
					                Integer.parseInt(sA[1]),
					                Integer.parseInt(sA[2])));
		}
		return locs;
	}
	public static String locationAsString(Location loc) {
		return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
	}
	public static String xyzAsString(int x, int y, int z) {
		return Integer.toString(x) + "," + Integer.toString(y) + "," + Integer.toString(z);
	}	
	public static String[] xyzFromString(String string) {
		String[] xyz = string.split(",");
		return xyz;
	}
	
	public static boolean blockWithin(Block target, Location min, Location max) {
		return (target.getX() >= min.getX() && target.getX() <= max.getX() || 
				target.getY() >= min.getY() || target.getY() <= max.getY() || 
				target.getZ() >= min.getZ() || target.getZ() <= max.getZ());
	}
	
	public static Location weLocationFromString(String string, World world) {
		String[] xyz = string.split(",");
		return new Location(world,Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
	}	
	
	public static Block blockFromXYZ(String string, World world) {
		String[] xyz = xyzFromString(string);
		return world.getBlockAt(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
	}
	
	public static void saySuccess(Player p, String s) {
		p.sendMessage(ChatColor.LIGHT_PURPLE + s);
	}
}
