package me.plasmarob.questcrafter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
// TODO import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

import me.plasmarob.questcrafter.database.DatabaseMethods;
import me.plasmarob.questcrafter.qblock.Link;

public class MainCommandExecutor implements CommandExecutor {
	
	private static ConcurrentHashMap<String, Dungeon> dungeons = Dungeon.getDungeons();
	private static ConcurrentHashMap<Player, String> selectedDungeons = Dungeon.getSelectedDungeons();
	Player player;
	String red = "" + ChatColor.RED;
	String grn = "" + ChatColor.GREEN;
	String purp = "" + ChatColor.LIGHT_PURPLE;
	String b = "" + ChatColor.DARK_BLUE;
	
	public MainCommandExecutor()  {
	}
	
	public static String[] expandArgs(String[] args) {
		
		return args;
	}
	//private static ConcurrentHashMap<Player, String> expansions = ImmutableMap.of(key1, value1, key2, value2);
	private static final Map<String, String> expansions;
    static {
    	HashMap<String, String> exp = new HashMap<String, String>();
    	exp.put("qca", "add");
    	exp.put("qce", "edit");
    	exp.put("qci", "info");
    	exp.put("qcn", "new");
    	exp.put("qcsf", "storageframe");
        expansions = Collections.unmodifiableMap(exp);
    }
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		if (sender instanceof Player) {
           player = (Player) sender;
        } else {
           sender.sendMessage("You must be a player!");
           return false;
        }
		// ------------------------------------------------
		// Expand shorthand commands
		if (expansions.containsKey(commandLabel)) {
			String[] argsOld = args.clone();
			args = new String[argsOld.length+1];
			args[0] = expansions.get(commandLabel);
			for (int i=0; i < argsOld.length; i++) {
				args[i+1] = argsOld[i];
			}
		}
		
		//TODO: add currently selected dungeon here
		
		if(args.length < 1) return false; // subcommands only (1 or more args) beyond this point
		String subcommand = args[0].toLowerCase();
		int last_index = args.length - 1;
		// ------------------------------------------------
		// Permissions checks
		if (!player.hasPermission("legendcraft.commands") && !player.isOp()) {	
			say(red+"You do not have permission to perform that command.");
			return false;
		}
		if (!DatabaseMethods.containsWorld(player.getWorld().getUID())) {
			if (!subcommand.equals("addworld") && !subcommand.equals("removeworld")) {
				say(red+"You cannot perform that command in this world.");
				say(red+"try using '/qc addworld' first.");
				return false;
			}
		}
		// ------------------------------------------------
		// Command List
		// ------------------------------------------------
		/**
		 * Add & Remove World
		 * - sets active worlds
		 * --- /qc addworld
		 * --- /qc removeworld
		 */
		if(subcommand.equals("addworld")) {
			World world;
			if (last_index >= 1) {
				if (Bukkit.getWorld(args[1]) != null) {
					world = Bukkit.getWorld(args[1]);
				} else {
					say(red+"World not found with this name.");
					return false;
				}
			} else {
				world = player.getWorld();
				DatabaseMethods.addWorld(world.getUID(), world.getName());
				say(grn+"Plugin enabled in world '" + world.getName() + "'.");
				return true;
			}	
		}
		if(subcommand.equals("removeworld")) {
			World world;
			if (last_index >= 1) {
				if (Bukkit.getWorld(args[1]) != null) {
					world = Bukkit.getWorld(args[1]);
				} else {
					say(red+"World not found with this name.");
					return false;
				}
			} else {
				world = player.getWorld();
				DatabaseMethods.disableWorld(world.getUID());
				say(grn+"Plugin disabled in world " + world.getName() + ".");
				return true;
			}	
		}
		/**
		 * ADD [qblock]
		 */
		if (subcommand.equals("add")) {
			if (last_index >= 2) {
				if (selectedDungeon(player)) {
					String addType = args[1].toLowerCase();
					String dungeonStr = selectedDungeons.get(player);
					/**
					 * Add Detector
					 * - attempts to create a trigger via looked-at coal_ore
					 */
					if (addType.equals("detector") || addType.equals("det")) {
						if (!dungeons.get(dungeonStr).tryAddDetector(player, args[2]))
							say(red + "Detector creation failed.");
						else 
							return true;
					}
					/**
					 * Add Storage
					 * - attempts to create 3D animation storage via looked-at emerald_ore
					 */
					if (addType.equals("storage") || addType.equals("sto")) {
						if (!dungeons.get(dungeonStr).tryAddStorage(player, args[2]))
							say(red + "Storage creation failed.");
						else 
							return true;
					}
				} else 
					say(red + "No dungeon selected. Select one using\n /qc select <dungeon>");
			} else 
				say(red+"Usage: /qc add <blocktype> <name> [...]");
		}
		
		if (subcommand.equals("storageframe")) {	// qcsf abbrev
			String dungeonStr;
			if (selectedDungeon(player)) {
				dungeonStr = selectedDungeons.get(player);
			} else {
				say(red + "No dungeon selected. Select one using\n /qc select <dungeon>");
				return false;
			}
			
			if (last_index >= 2) {
				String sfCommand = args[2].toLowerCase();
				if(sfCommand.equals("add")) {
					dungeons.get(dungeonStr).tryAddStorageFrame(player, args);
				} else if(sfCommand.equals("play")) {
					dungeons.get(dungeonStr).tryPlayStorageFrameAnim(player, args);
				} else if (last_index >= 3) {
					if(sfCommand.equals("show")) {
						dungeons.get(dungeonStr).tryShowStorageFrame(player, args);
					} else if(sfCommand.equals("delete")) {
						dungeons.get(dungeonStr).tryDeleteStorageFrame(player, args);
					} else if (last_index >= 4) {
						if(sfCommand.equals("update")) {
							dungeons.get(dungeonStr).tryUpdateStorageFrame(player, args);
						} else 
							say(red+"Usage: /qcsf <storage> "+sfCommand+" [...]");
					} else 
						say(red+"Usage: /qcsf <storage> "+sfCommand+" [...]");
				} else 
					say(red+"Usage: /qcsf <storage> "+sfCommand+" [...]");
			} else 
				say(red+"Usage: /qc qcsf <storage> <ADD|SHOW|EDIT|DELETE|UPDATE> [...]");
		}
		
		
		
		/**
		 * Edit (universal)
		 * - attempts to edit a block
		 * --- /lc edit <block> <key> <value>
		 */
		if (last_index >=3 && args[0].toLowerCase().equals("edit")) {
			if (selectedDungeon(player)) {
				String dungeonStr = selectedDungeons.get(player);
				dungeons.get(dungeonStr).tryEdit(player, args[1], args[2], args[3]);
			}
		}
		else if (args[0].toLowerCase().equals("edit"))
			say(red + "Usage: /lc edit <block> <key> <value>\nTo see valid options, use /lc show <block>");
		
		/**
		 * New Dungeon
		 * - Creates and selects* a new blank dungeon from selection.
		 * --- /qc new <dungeon>
		 */
		if(subcommand.equals("new")) {
			if(last_index >= 1) {
				if (!dungeons.containsKey(args[1])) {
					
					//SessionOwner so;
					//SessionManager sm = new SessionManager(WorldEdit.getInstance());
					//BukkitImplAdapter.adapt(player);
					
					
					// TODO
					//BukkitAdapter ba;
					//LocalSession ls = WorldEdit.getInstance().getSessionManager().get(player);
					//Selection sel = QuestCrafter.getWE().getSelection(player);
					WorldEditPlugin we = QuestCrafter.getWE();
					try {
						Region region = we.getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));
						if (region instanceof CuboidRegion) {
					        Vector mv = region.getMinimumPoint();
					        Vector Mv = region.getMaximumPoint();
					        Location min = new Location(player.getWorld(),mv.getX(),mv.getY(),mv.getZ());
					        Location max = new Location(player.getWorld(),Mv.getX(),Mv.getY(),Mv.getZ());
					        new Dungeon(args[1], player.getWorld(), min, max);
					        selectedDungeons.put(player, args[1]);
					        say(grn+"Dungeon created and selected");
					        return true;
					    } else
					        say(red+"Invalid WorldEdit Selection");
					} catch (IncompleteRegionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					/*
					if (sel instanceof CuboidSelection) {
				        Location min = sel.getMinimumPoint();
				        Location max = sel.getMaximumPoint();
				        new Dungeon(args[1], sel.getWorld(), min, max);
				        selectedDungeons.put(player, args[1]);
				        say(grn+"Dungeon created and selected");
				        return true;
				    } else
				        say(red+"Invalid WorldEdit Selection");
				        */
				} else
					say(red+"Dungeon with this name already exists");
			} else 
				say(red+"Usage: /qc new <dungeon>");
			return false;
		}
		/**
		 * Delete Dungeon
		 * - Deletes named dungeon.
		 * - TODO: Not Yet Implemented
		 */
		if (subcommand.equals("deletedungeon")) {	
			if (last_index >= 1) {
				if (dungeons.containsKey(args[1])) {
					Dungeon deletee = dungeons.get(args[1]);
					if (deletee.isEnabled()) {
						say(red+"Can't delete enabled dungeon.");
						return false;
					}
					if (selectedDungeons.containsKey(player) && selectedDungeons.get(player).equals(args[1])) {
						selectedDungeons.remove(player);
					}
					//boolean attempt = deletee.tryDeleteDungeon(player, args[1], Dungeon.findDungeonDir(args[1]));
					boolean attempt = deletee.tryDeleteDungeon(player, args[1]);
					if (attempt) {
						dungeons.remove(args[1]);
						say(red+"Dungeon "+args[1]+" deleted.");
					} else
						say(red+"Error deleting dungeon. If this persists, remove yml file while server is not running.");
				} else
					say(red+"Dungeon with this name not found.");
			} else if (subcommand.equals("deletedungeon"))
				say(red+"Usage: /qc deleteDungeon <dungeon>");
		}
		
		/**
		 * Dungeon Enable&Disable
		 * - Changes whether dungeon is enabled
		 * - sets or unsets code Blocks, resetting them.
		 * --- /qc enable <dungeon>
		 * --- /qc disable <dungeon>
		 */
		if (subcommand.equals("enable")) {
			if (last_index >= 1) {
				if (dungeons.containsKey(args[1])) {
					dungeons.get(args[1]).setEnabled(true);
					say(purp + "Dungeon " + args[1] + " enabled.");
					return true;
				} else
					say(red + "Dungeon with this name not found.");
			} else {
				if (selectedDungeons.containsKey(player)) {
					dungeons.get(selectedDungeons.get(player)).setEnabled(true);
					say(purp + "Dungeon " + selectedDungeons.get(player) + " enabled.");
					return true;
				} else {
					say(red + "No dungeon selected.");
					say(red + "Usage: /qc enable [dungeon]");
				}
			}
			return false;
		}
		if (subcommand.equals("disable")) {
			if (last_index >= 1) {
				if (dungeons.containsKey(args[1])) {
					dungeons.get(args[1]).setEnabled(false);
					say(purp + "Dungeon " + args[1] + " disabled.");
					return true;
				} else
					say(red + "Dungeon with this name not found.");
			} else {
				if (selectedDungeons.containsKey(player)) {
					dungeons.get(selectedDungeons.get(player)).setEnabled(false);
					say(purp + "Dungeon " + selectedDungeons.get(player) + " disabled.");
					return true;
				} else {
					say(red + "No dungeon selected.");
					say(red + "Usage: /qc disable [dungeon]");
				}
			}
			return false;
		}

		/**
		 * Show
		 * - shows information about a block
		 * --- /lc show <name>
		 */
		if(subcommand.equals("show") || subcommand.equals("info")) {
			if (selectedDungeon(player)) {
				if (last_index >= 1) {
					String dungeonStr = selectedDungeons.get(player);
					dungeons.get(dungeonStr).show(player, args);
				} else {
					dungeons.get(selectedDungeons.get(player)).show(player);
					say(ChatColor.GRAY + "/qc show <qblock> for a specific qblock.");
				}
			}
		}
		
		/**
		 * Link Blocks
		 * - attempts to link blocks, where A triggers B
		 * --- /lc link <block_from> <block_to> [TRIGGER|set|reset|on|off]
		 */
		if (last_index >= 2 && args[0].toLowerCase().equals("link")) {
			String type = "trigger";
			if (last_index >= 3 && 
						(args[3].toLowerCase().equals("reset") ||
						 args[3].toLowerCase().equals("on") || 
						 args[3].toLowerCase().equals("off") ))
				type = args[3].toLowerCase();
			if (selectedDungeon(player)) {
				String dungeonStr = selectedDungeons.get(player);
				if (!Link.valid(type) || !dungeons.get(dungeonStr).tryLink(player, args[1], args[2], Link.get(type.toUpperCase())))
					say(red + "Linking failed.");
			}
		}
		else if (args[0].toLowerCase().equals("link"))
			say(red + "Usage: /lc link <sender> <receiver> [trigger|set|reset|on|off]");
		
		/**
		 * Select
		 * - selects available dungeon
		 * --- /lc select <dungeon>
		 */
		if(subcommand.equals("select")) {
			if(last_index >= 1) {
				if (!dungeons.containsKey(args[1])) {
					say(red + "Dungeon with this name not found.");
					say(purp + "Existing Dungeons:");
					String dList = "";
					for (String s : dungeons.keySet())
						dList = dList + "  " + s;
					say(purp + dList);
				}
				else {
					selectedDungeons.put(player, args[1]);
					say(purp + args[1] + " selected.");
				}
			} else
				say(red + "Usage: /qc select <dungeon>");
		}	
		
		/*
		 * In principle how you'd clone all of a block's data
		 * 
		 * 
		 * 
		 * 
		 * Player p = (Player) s;
                Location loc = p.getTargetBlock((HashSet<Byte>) null, 0).getLocation();
                Location loc2 = p.getLocation();
                Block b = p.getWorld().getBlockAt(loc);
                Block b2 = p.getWorld().getBlockAt(p.getLocation());
                b2.setTypeIdAndData(b.getTypeId(), b.getData(), false);
                CraftWorld cw = (CraftWorld)p.getWorld();
                TileEntity tileEntity = cw.getTileEntityAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                TileEntity tileEntity2 = cw.getTileEntityAt(loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ());
                WorldServer we = cw.getHandle();
                we.setTileEntity(tileEntity2.getPosition(), tileEntity);
		 * 
		 * 
		 * 
		Player p = (Player) s;
		Location loc = p.getTargetBlock((HashSet<Byte>) null, 0).getLocation();
		Location loc2 = p.getLocation();
		Block b = p.getWorld().getBlockAt(loc);
		Block b2 = p.getWorld().getBlockAt(p.getLocation());
		b2.setTypeIdAndData(b.getTypeId(), b.getData(), true);
		CraftWorld cw = (CraftWorld) p.getWorld();
		TileEntity tileEntity = cw.getTileEntityAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		TileEntity tileEntity2 = cw.getTileEntityAt(loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ());
		NBTTagCompound ntc = new NBTTagCompound();
		NBTTagCompound ntc2 = new NBTTagCompound();
		tileEntity.b(ntc);
		ntc2 = (NBTTagCompound) ntc.clone();
		ntc2.setInt("x", loc2.getBlockX());
		ntc2.setInt("y", loc2.getBlockY());
		ntc2.setInt("z", loc2.getBlockZ());
		tileEntity2.a(ntc2);
		tileEntity2.update();
		*/
		
		/**
		 * List
		 * - Lists available dungeons for selection
		 * --- /lc list
		 */
		if(last_index >= 0 && args[0].toLowerCase().equals("list")) {
			say("QuestCrafter Dungeons:");
			for (String s : dungeons.keySet())
				say(" " + s);
		}
		
		return false;
	}
	
	// shorthand for checking player has selected a dungeon.
	private boolean selectedDungeon(Player p) {
		if (!selectedDungeons.containsKey(p)) {
			say(red + "No dungeon selected. Select one using\n /qc select <dungeon>");
			return false;
		}
		return true;
	}
	// shorthand for communicating with command sender
	void say(String s) {
		player.sendMessage(s);
	}
	static String prp = "" + ChatColor.LIGHT_PURPLE;
	static String r = "" + ChatColor.RESET;
}
