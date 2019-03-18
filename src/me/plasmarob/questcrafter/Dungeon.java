package me.plasmarob.questcrafter;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

import me.plasmarob.questcrafter.database.DatabaseInserter;
import me.plasmarob.questcrafter.database.DatabaseMethods;
import me.plasmarob.questcrafter.qblock.Detector;
import me.plasmarob.questcrafter.qblock.Link;
import me.plasmarob.questcrafter.qblock.Receiver;
import me.plasmarob.questcrafter.qblock.RedstoneDetector;
import me.plasmarob.questcrafter.qblock.Sender;
import me.plasmarob.questcrafter.qblock.Storage;
import me.plasmarob.questcrafter.util.Tools;

public class Dungeon {
	// TODO: add relevant dungeon-level commands, including resize and delete
	// Global static data ///////////////////////
	private static File dungeonFolder = null;
	public static File getDungeonFolder() { return dungeonFolder; }
	
	private static ConcurrentHashMap<String, Dungeon> dungeons = new ConcurrentHashMap<String, Dungeon>();
	public static ConcurrentHashMap<String, Dungeon> getDungeons() { return dungeons; };
	//TODO: config file
	//public static ConcurrentHashMap<String, FileConfiguration> dungeonConfigs = new ConcurrentHashMap<String, FileConfiguration>();
	private static ConcurrentHashMap<Player, String> selectedDungeons = new ConcurrentHashMap<Player, String>();
	public static ConcurrentHashMap<Player, String> getSelectedDungeons() { return selectedDungeons; };
	private static ConcurrentHashMap<Player, Dungeon> activePlayerDungeons = new ConcurrentHashMap<Player, Dungeon>();
	public static ConcurrentHashMap<Player, Dungeon> getActivePlayerDungeons() { return activePlayerDungeons; };
	public static void removePlayersFromDungeon(Dungeon dungeon) {
		Iterator<Map.Entry<Player, Dungeon>> iterator = activePlayerDungeons.entrySet().iterator();
		while(iterator.hasNext()) {
			Map.Entry<Player, Dungeon> entry = iterator.next();
			if(entry.getValue().equals(dungeon)) {
				iterator.remove();
			}
		}
	}
	
	private ConcurrentHashMap<String, Detector> detectors = new ConcurrentHashMap<String, Detector>();
	public ConcurrentHashMap<String, Detector> getDetectors() { return detectors; }
	private ConcurrentHashMap<String, Storage> storages = new ConcurrentHashMap<String, Storage>();
	public ConcurrentHashMap<String, Detector> getStorages() { return detectors; }
	private ConcurrentHashMap<String, RedstoneDetector> rsDetectors = new ConcurrentHashMap<String, RedstoneDetector>();
	public ConcurrentHashMap<String, RedstoneDetector> getRSDetectors() { return rsDetectors; }
	
	private World world;
	public World getWorld() { return world; }
	
	private int dungeon_id;
	public int getDungeonId() { return dungeon_id; }
	public void setDungeonId(int dungeon_id) { this.dungeon_id = dungeon_id; }
	
	private List<Player> players = new ArrayList<Player>();
	public List<Player> getPlayers() { return players; }
	
	private String name;
	private Location min;
	private Location max;
	
	private boolean enabled = false;
	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean bool) {
		enabled = bool;
		updatePlayersIfEnabled();
		for (String s : detectors.keySet()) 
			detectors.get(s).setEnabled(bool);	
		for (String s : rsDetectors.keySet()) 
			rsDetectors.get(s).setEnabled(bool);	
		
		for (String s : storages.keySet()) 
			storages.get(s).setEnabled(bool);		
		//updateEnabled(enabled);
		if(!enabled) removePlayersFromDungeon(this);
	}
	
	// Create from WE selection
	public Dungeon(String name, World world, Location min, Location max) {
		this.name = name;
		this.world = world;
		this.min = min;
		this.max = max;
		
		int[] corners = getCorners();
		dungeon_id = DatabaseMethods.insertDungeon(world, name, corners);
		dungeons.put(name, this);
	}
	
	// Create from DB
	public Dungeon(Map<String,Object> data) {
		this.dungeon_id = (int) data.get("id");
		this.name = (String) data.get("name");
		this.world = Bukkit.getWorld(UUID.fromString((String) data.get("uuid")));
		this.min = Tools.weLocationFromString((String) data.get("min"), world);
		this.max = Tools.weLocationFromString((String) data.get("max"), world);
		
		dungeons.put(name, this);
	
		// Make list for linking up
		List<Sender> senderList = new ArrayList<Sender>();
		List<Receiver> receiverList = new ArrayList<Receiver>();
		
		List<Map<String, Object>> detectorList = DatabaseMethods.getBlocks("PLAYER_DETECTOR", dungeon_id);
		for (Map<String,Object> m : detectorList) {
			Detector d =  new Detector(m, this);
			detectors.put((String)m.get("name"), d);
			senderList.add(d);
		}
		
		List<Map<String, Object>> rsDetectorList = DatabaseMethods.getBlocks("REDSTONE_DETECTOR", dungeon_id);
		for (Map<String,Object> m : rsDetectorList) {
			RedstoneDetector rs =  new RedstoneDetector(m, this);
			rsDetectors.put((String)m.get("name"), rs);
			senderList.add(rs);
		}
		
		//TODO: insert storages on load with their anim frames
		List<Map<String, Object>> storageList = DatabaseMethods.getBlocks("STORAGE", dungeon_id);
		for (Map<String,Object> m : storageList) {
			int storage_id = (int) m.get("id");
			List<Map<String, Object>> anim = DatabaseMethods.getAnimation(storage_id);
			String json="";
			for (Map<String,Object> a : anim) {
				 json = (String)a.get("data");
			}
			Storage s =  new Storage(m, json, this);
			storages.put((String)m.get("name"), s);
			receiverList.add(s);
		}
		
		// Loop through senders and call DB query for links
	    for (Sender sender : senderList) {
		   List<Map<String, Object>> linkList = DatabaseMethods.getLinks(dungeon_id, sender.getID());
		   // Loop through rows returned for IDs
		   for (Map<String,Object> link : linkList) {
			   int rec_id = (Integer)link.get("receiver_id");
			   Link type = Link.get((Integer)link.get("trigger_type_id"));
			   // Loop through receivers and find a match
			   for (Receiver receiver : receiverList) {
				   if (receiver.getID() == rec_id) {
					   sender.setTarget(receiver, type);
					   break;
				   }
			   }
		   }
	    }
	}
	
	// Static Dungeon load & save
	// -----------------------
	public static void loadDungeons() {
		//Tools.say("Loading Dungeons");
		List<Map<String, Object>> rows = DatabaseMethods.getDungeons();
		for (Map<String,Object> m : rows) {
			new Dungeon(m);
			//Tools.say("Dungeon Added.");
		}
	}
	
	//private void updateEnabled(boolean bool) {
	//}
	
	public static File findDungeonDir(String dungeonName) {
		File[] listOfFiles = dungeonFolder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
    		if (listOfFiles[i].isDirectory() && listOfFiles[i].getName().equals(dungeonName)) 
    			return listOfFiles[i];
    	}		
		return null;
	}
	
	@SuppressWarnings("unused")
	public boolean tryDeleteDungeon(Player player, String myName) {
		File file = null;
		// Delete independent files
		for (String s : detectors.keySet()) 
			tryDelete(player, s);	
		for (String s : rsDetectors.keySet()) 
			tryDelete(player, s);	
		//for (String s : spawners.keySet()) 
		//	tryDelete(player, s);
		for (String s : storages.keySet()) 
			tryDelete(player, s);	
		
		DatabaseMethods.deleteDungeon(this.dungeon_id);
		
		/*
		if (file != null && file.exists()) {
			try {
				FileUtils.deleteDirectory(file);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			player.sendMessage(ChatColor.LIGHT_PURPLE + "Dungeon deleted.");
			return true;
		}
		*/
		return true;
	}
	
	public boolean tryAddDetector(Player player, String name) {
		if (nameUsed(name)) {
			player.sendMessage(ChatColor.RED + "A dungeon block with this name already exists.");
			return false;
		}
		Block target = player.getTargetBlock(null, 20);
		if (target.getType() == Material.COAL_ORE) {
			if (Tools.blockWithin(target, min, max) && target.getWorld() == world) {
				detectors.put(name, new Detector(player, target, name, this));
				player.sendMessage(ChatColor.LIGHT_PURPLE + "Detector " + name + " created!");
				return true;
			} else
				player.sendMessage(ChatColor.RED + "This block is not within dungeon boundaries.");
		} else 
			player.sendMessage(ChatColor.RED + "Error: Enter this command while facing " + Material.COAL_ORE.toString());
		return false;
	}
	
	public boolean tryAddRSDetector(Player player, String name) {
		if (nameUsed(name)) {
			player.sendMessage(ChatColor.RED + "A dungeon block with this name already exists.");
			return false;
		}
		Block target = player.getTargetBlock(null, 20);
		if (target.getType() == Material.REDSTONE_ORE) {
			if (Tools.blockWithin(target, min, max) && target.getWorld() == world) {
				rsDetectors.put(name, new RedstoneDetector(player, target, name, this));
				player.sendMessage(ChatColor.LIGHT_PURPLE + "RS Detector " + name + " created!");
				return true;
			} else
				player.sendMessage(ChatColor.RED + "This block is not within dungeon boundaries.");
		} else 
			player.sendMessage(ChatColor.RED + "Error: Enter this command while facing " + Material.REDSTONE_ORE.toString());
		return false;
	}
	
	
	
	@SuppressWarnings("unused")
	public boolean tryAddStorage(Player player, String name) {
		if (nameUsed(name)) {
			player.sendMessage(red + "A dungeon block with this name already exists.");
			return false;
		}

		WorldEditPlugin we = QuestCrafter.getWE();
		try {
			Region region = we.getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));
			
			if (region instanceof CuboidRegion && 
		    		region.getHeight()*region.getLength()*region.getWidth() < 3000 ) {
				if (false)  { //TODO: check this
					player.sendMessage(red + "Selection is not within dungeon boundaries.");
					return false;
				}
				Block target = player.getTargetBlock(null, 20);
				if (target.getType() == Material.EMERALD_ORE) {
					if (Tools.blockWithin(target, min, max) && target.getWorld() == world) {
						storages.put(name, new Storage(player, target, name, this));
						player.sendMessage(prp + "Storage " + name + " created!");
						return true;
					} else
						player.sendMessage(red + "This block is not within dungeon boundaries.");
				} else 
					player.sendMessage(red + "Error: Enter this command while facing " + Material.EMERALD_ORE.toString());
		    } else {
		    	if (region instanceof CuboidRegion)
			    	player.sendMessage(red + "Too many blocks");
			    else
			    	player.sendMessage(red + "Invalid WorldEdit selection.  Must be cuboid.");
		    }
		} catch (IncompleteRegionException e) {
			e.printStackTrace();
		}
		
		
		
		//WorldEdit.getInstance().getSessionManager().get(bp);
		/*
		Selection sel = QuestCrafter.getWE().getSelection(player);
		if (sel instanceof CuboidSelection && 
	    		sel.getHeight()*sel.getLength()*sel.getWidth() < 3000 ) {
			if (false)  { //TODO: check this
				player.sendMessage(red + "Selection is not within dungeon boundaries.");
				return false;
			}
			Block target = player.getTargetBlock(null, 20);
			if (target.getType() == Material.EMERALD_ORE) {
				if (Tools.blockWithin(target, min, max) && target.getWorld() == world) {
					storages.put(name, new Storage(player, target, name, this));
					player.sendMessage(prp + "Storage " + name + " created!");
					return true;
				} else
					player.sendMessage(red + "This block is not within dungeon boundaries.");
			} else 
				player.sendMessage(red + "Error: Enter this command while facing " + Material.EMERALD_ORE.toString());
	    } else {
	    	if (sel instanceof CuboidSelection)
		    	player.sendMessage(red + "Too many blocks");
		    else
		    	player.sendMessage(red + "Invalid WorldEdit selection.  Must be cuboid.");
	    }
	    */
		return false;
	}
	
	public boolean tryAddStorageFrame(Player player, String[] args) {
		if(storages.containsKey(args[1])) {
			if(args.length>=4) {
				try {
					int ticks = Integer.parseInt(args[3]);
					if(args.length>=5) {
						int index = Integer.parseInt(args[4]);
						if(index < 0 || index > storages.get(args[1]).getAnimLength()) {
							player.sendMessage(red + "Invalid index.");
							return false;
						}
						storages.get(args[1]).addFrame(ticks, index);
					} else 
						storages.get(args[1]).addFrame(ticks);
					player.sendMessage(prp + "Frame added.");
					return true;
				} catch (Exception e) {
					// stack trace as a string
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					Bukkit.getConsoleSender().sendMessage(sw.toString());
					player.sendMessage(red + "Invalid command.");
				}
			} else {
				storages.get(args[0]).addFrame();
				return true;
			}
		} else
			player.sendMessage(red + "Storage block named " + args[1] + " not found.");
		return false;
	}
	public boolean tryPlayStorageFrameAnim(Player player, String[] args) {
		if(storages.containsKey(args[1])) {
			if(args.length>=3) {
				try {
					storages.get(args[1]).animate(player);
					return true;
				} catch (Exception e) {
					// stack trace as a string
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					Bukkit.getConsoleSender().sendMessage(sw.toString());
					player.sendMessage(red + "Invalid command.");
				}
			}
		} else
			player.sendMessage(red + "Storage block named " + args[0] + " not found.");
		return false;
	}
	public boolean tryShowStorageFrame(Player player, String[] args) {
		if(storages.containsKey(args[1])) {
			if(args.length>=4) {
				try {
					int index = Integer.parseInt(args[3]);
					if(index < 0 || index >= storages.get(args[1]).getAnimLength()) {
						player.sendMessage(red + "Invalid index.");
						return false;
					}
					storages.get(args[1]).showFrame(player, index);
					return true;
				} catch (Exception e) {
					// stack trace as a string
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					Bukkit.getConsoleSender().sendMessage(sw.toString());
					player.sendMessage(red + "Invalid command.");
				}
			}
		} else
			player.sendMessage(red + "Storage block named " + args[0] + " not found.");
		return false;
	}
	public boolean tryUpdateStorageFrame(Player player, String[] args) {
		if(storages.containsKey(args[1])) {
			if(args.length>=5) {
				try {
					int index = Integer.parseInt(args[4]);
					int ticks = Integer.parseInt(args[3]);
					if(index < 0 || index >= storages.get(args[1]).getAnimLength()) {
						player.sendMessage(red + "Invalid index.");
						return false;
					}
					storages.get(args[1]).replaceFrame(player, index, ticks);
					return true;
				} catch (Exception e) {
					// stack trace as a string
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					Bukkit.getConsoleSender().sendMessage(sw.toString());
					player.sendMessage(red + "Command error.");
				}
			}
		} else
			player.sendMessage(red + "Storage block named " + args[0] + " not found.");
		return false;
	}
	public boolean tryDeleteStorageFrame(Player player, String[] args) {
		if(storages.containsKey(args[1])) {
			if(args.length>=4) {
				try {
					int index = Integer.parseInt(args[3]);
					if(index < 0 || index >= storages.get(args[1]).getAnimLength()) {
						player.sendMessage(red + "Invalid index.");
						return false;
					}
					storages.get(args[1]).removeFrame(player, index);
					return true;
				} catch (Exception e) {
					// stack trace as a string
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					Bukkit.getConsoleSender().sendMessage(sw.toString());
					player.sendMessage(red + "Command error.");
				}
			}
		} else
			player.sendMessage(red + "Storage block named " + args[0] + " not found.");
		return false;
	}
	
	
	public boolean tryLink(Player player, String b1, String b2, Link type) {
		Sender sender = getSender(b1);
		Receiver receiver = getReceiver(b2);
		if (sender != null && receiver != null) {
			if (sender.setTarget(receiver, type)) {
				int sid = sender.getID();
				int rid = receiver.getID();
				new DatabaseInserter("link")
					.add("sender_id", sid)
					.add("receiver_id", rid)
					.add("trigger_type_id", type.ID)
					.add("dungeon_id", dungeon_id)
					.execute();
				player.sendMessage(ChatColor.GREEN + type.NAME + " link successful.");
				return true;
			}
		} else if (receiver == null)
			player.sendMessage(ChatColor.RED + b2 + " does not exist.");
		else
			player.sendMessage(ChatColor.RED + b1 + " does not exist.");
		return false;
	}
	
	
	public void updatePlayersIfEnabled() {
		if(enabled) {
			players.clear();
			players = world.getPlayers();
			Player p;
			for (int i = 0; i < players.size(); i++) {
				p = players.get(i);
				if (p.getLocation().getX() < min.getBlockX() ||
					p.getLocation().getY() < min.getBlockY() ||
					p.getLocation().getZ() < min.getBlockZ() ||
					p.getLocation().getX() > max.getBlockX() ||
					p.getLocation().getY() > max.getBlockY() ||
					p.getLocation().getZ() > max.getBlockZ())
				{
					players.remove(p);
					i--;
				}
			}
			for (Player player : players)
				activePlayerDungeons.put(player, this);
		}
	}
	//Actually check for players around.
	public void updateIfEnabled() {
		if(enabled) {
			/*
			for (String d : autoDoors.keySet()) 
				autoDoors.get(d).update();	
			for (String t : torchBlocks.keySet()) 
				torchBlocks.get(t).testPlayers(players);
			for (String s : spawners.keySet()) 
				spawners.get(s).updateMobs();
			*/
		}
	}
	
	public void updatePlayerMoved() {
		if(enabled) {
			for (String d : detectors.keySet()) 
				detectors.get(d).testPlayers(players);
		}
	}
	public void testForRedstone(Block block) {
		if(enabled) {
			for (String key : rsDetectors.keySet()) 
				rsDetectors.get(key).testForRedstone(block);
		}
	}
	
	
	// Show Command
	public void show(Player player, String[] args) {
		if(args.length < 2) return;
		String name = args[1];
		if (detectors.containsKey(name)) {
			detectors.get(name).show(player);
			//showLinksFrom(player, detectors.get(name));
		}
		if (storages.containsKey(name)) {
			storages.get(name).show(player);
			showLinksFrom(player, storages.get(name));
		}
	}
	
	public void show(Player player) {
		player.sendMessage(ChatColor.LIGHT_PURPLE + "Min: " + min.getBlockX() + " " + min.getBlockY() + " " + min.getBlockZ());
		player.sendMessage(ChatColor.LIGHT_PURPLE + "Max: " + max.getBlockX() + " " + max.getBlockY() + " " + max.getBlockZ());
		listBlocks(player);
		
		Tools.outline(player, min, max, Material.GOLD_BLOCK.createBlockData(), Material.EMERALD_BLOCK.createBlockData(), 10);	
	}
	public void showLinksFrom(Player player, Receiver rec) {
		for (String s : detectors.keySet()) {
			if (detectors.get(s).getLinks().containsKey(rec)) {
				player.sendMessage(ChatColor.LIGHT_PURPLE + "  Link from " + s + " ("+ detectors.get(s).getLinks().get(rec) +")" );
				Tools.showLine(world, detectors.get(s), rec);
			}
		}
	}
	private void listBlocks(Player player) {
		if (detectors.size() > 0) {
			player.sendMessage(ChatColor.LIGHT_PURPLE + "Detectors:");
			String names = "";
			for (String name : detectors.keySet()) {
				names += name + " ";
			}
			player.sendMessage("  " + names);
		}
		if (rsDetectors.size() > 0) {
			player.sendMessage(ChatColor.LIGHT_PURPLE + "Redstone Detectors:");
			String names = "";
			for (String name : rsDetectors.keySet()) {
				names += name + " ";
			}
			player.sendMessage("  " + names);
		}
		if (storages.size() > 0) {
			player.sendMessage(ChatColor.LIGHT_PURPLE + "Storages:");
			String names = "";
			for (String name : storages.keySet()) {
				names += name + " ";
			}
			player.sendMessage("  " + names);
		}
	}
	
	public void tryEdit(Player player, String name, String key, String value) {
		if (nameUsed(name)) {
			if (detectors.containsKey(name))
				detectors.get(name).edit(player, key, value);
			if (rsDetectors.containsKey(name))
				rsDetectors.get(name).edit(player, key, value);
			if (storages.containsKey(name))
				storages.get(name).edit(player, key, value);
		}
	}

	public boolean nameUsed(String name) {
		return detectors.containsKey(name) || rsDetectors.containsKey(name) ||
			   storages.containsKey(name);
		/*
		return (chestBlocks.containsKey(name) || detectors.containsKey(name) || 
				 || spawners.containsKey(name) ||
				musics.containsKey(name) || autoDoors.containsKey(name) ||
				rsDetectors.containsKey(name) || timerBlocks.containsKey(name) ||
				torchBlocks.containsKey(name)
				);
				*/
	}
	
	//##################################################################
	// Linking Code
	
	public Sender getSender (String s) {
		if (detectors.containsKey(s))
			return detectors.get(s);
		else if (rsDetectors.containsKey(s))
			return rsDetectors.get(s);
		/*
		else if (spawners.containsKey(s))
			return spawners.get(s);	
		else if (timerBlocks.containsKey(s))
			return timerBlocks.get(s);
		else if (torchBlocks.containsKey(s))
			return torchBlocks.get(s);
			*/
		return null;			
	}
	
	public Receiver getReceiver (String r) {
		
		if (storages.containsKey(r))
			return storages.get(r);		
		/*
		else if (spawners.containsKey(r))
			return spawners.get(r);
		else if (musics.containsKey(r))
			return musics.get(r);		
		else if (autoDoors.containsKey(r))
			return autoDoors.get(r);	
		else if (timerBlocks.containsKey(r))
			return timerBlocks.get(r);
		else if (torchBlocks.containsKey(r))
			return torchBlocks.get(r);
		else if (chestBlocks.containsKey(r))
			return chestBlocks.get(r);
			*/
		return null;			
	}
	
	public void unlink(Player player, String name) {
		Sender sender = getSender(name);
		if (sender != null) {
			sender.clearLinks();
			DatabaseMethods.removeAllLinks(dungeon_id, sender.getID());
		}
		
		Receiver rec = getReceiver(name);
		if (rec != null) {
			for (String s : detectors.keySet()) 
				detectors.get(s).removeLink(rec);
			for (String s : rsDetectors.keySet()) 
				rsDetectors.get(s).removeLink(rec);
			/*
			for (String s : spawners.keySet()) 
				spawners.get(s).removeLink(rec);
			for (String s : torchBlocks.keySet()) 
				torchBlocks.get(s).removeLink(rec);
			for (String s : timerBlocks.keySet()) 
				timerBlocks.get(s).removeLink(rec);
				*/
		}
		//if (sender != null || rec != null)
		//	player.sendMessage(ChatColor.LIGHT_PURPLE + "All links to and from this block deleted.");
	}
	public void unlink(Player player, String sender, String target) {
		Sender send = getSender(sender);
		Receiver rec = getReceiver(target);
		DatabaseMethods.removeLink(dungeon_id, send.getID(), rec.getID());
		if (rec != null && send != null) {
			send.removeLink(rec);
			player.sendMessage(ChatColor.LIGHT_PURPLE + "This link removed.");
		}
	}
	
	public void tryDelete(Player player, String name) {
		unlink(player, name);
		if (detectors.containsKey(name)) {
			detectors.get(name).destroy();
			detectors.get(name).clearLinks();
			DatabaseMethods.removeAllLinks(dungeon_id, detectors.get(name).getID());
			DatabaseMethods.deleteBlock(dungeon_id, detectors.get(name).getID());
			detectors.remove(name);
			//player.sendMessage(ChatColor.LIGHT_PURPLE + "Detector " + name + " and its links deleted.");
		} else if (rsDetectors.containsKey(name)) {
			rsDetectors.get(name).destroy();
			rsDetectors.get(name).clearLinks();
			DatabaseMethods.removeAllLinks(dungeon_id, rsDetectors.get(name).getID());
			DatabaseMethods.deleteBlock(dungeon_id, rsDetectors.get(name).getID());
			rsDetectors.remove(name);
			//player.sendMessage(ChatColor.LIGHT_PURPLE + "Detector " + name + " and its links deleted.");
		} else if (storages.containsKey(name)) {
			storages.remove(name);
			unlink(player, name);
			storages.get(name).removeAllFrames();
			DatabaseMethods.deleteBlock(dungeon_id, storages.get(name).getID());
			//player.sendMessage(ChatColor.LIGHT_PURPLE + "Storage block " + name + " and its links deleted.");
		
		}
	}
	
	public int[] getCorners() {
		int[] corners = {(int) min.getX(),(int) min.getY(),(int) min.getZ(),
						(int) max.getX(),(int) max.getY(),(int) max.getZ()};
		return corners;
	}
	// shorthand for communicating with command sender
	void log(String s) {
		Bukkit.getConsoleSender().sendMessage(s);
	}
	static String prp = "" + ChatColor.LIGHT_PURPLE;
	static String r = "" + ChatColor.RESET;
	static String red = "" + ChatColor.RED;
}
