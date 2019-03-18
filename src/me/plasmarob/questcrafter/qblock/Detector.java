package me.plasmarob.questcrafter.qblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import me.plasmarob.questcrafter.Dungeon;
import me.plasmarob.questcrafter.QuestCrafter;
import me.plasmarob.questcrafter.database.DatabaseInserter;
import me.plasmarob.questcrafter.database.DatabaseMethods;
import me.plasmarob.questcrafter.util.Tools;

public class Detector implements Sender {
	// Dungeon
	private Dungeon dungeon;
	private boolean enabled = false;
	public boolean isEnabled() { return enabled; }
	// Object immutable
	private int id = -1;
	public int getID() { return this.id; }
	public void setID(int id) { this.id= id; }
	private String name;
	public String name() { return name; }
	public String type() { return "detector"; }
	private Block mainBlock;
	public Block getMainBlock() { return mainBlock; }
	public int getX() { return mainBlock.getX(); }
	public int getY() { return mainBlock.getY(); }
	public int getZ() { return mainBlock.getZ(); }
	// Config
	private boolean defaultOnOff = true;
	public boolean isDefaultOnOff() { return defaultOnOff; }
	public void setDefaultOnOff(boolean defaultOnOff) { this.defaultOnOff = defaultOnOff; dbInsert(); }
	private int maxTimes = Integer.MAX_VALUE; //max times it can be triggered
	public int getMaxTimes() { return maxTimes; }
	public void setMaxTimes(int maxTimes) { this.maxTimes = maxTimes; dbInsert(); }
	private int minTimes = Integer.MIN_VALUE; //min times until it can be triggered
	public int setMinTimes() { return minTimes; }
	public void setMinTimes(int minTimes) { this.minTimes = minTimes; dbInsert(); }
	private int delay = 0;
	public double getDelay() { return delay; }
	public void setDelay(int delay) { this.delay = delay; dbInsert(); }
	// Sender
	private HashMap<Receiver, Link> links = new HashMap<Receiver, Link>();
	// Unique
	private List<Block> blockList = new ArrayList<Block>();
	public List<Block> getBlocks() { return blockList; }
	private boolean entermode = true; //1=enter, 0=leave
	public boolean getMode() { return entermode; }
	// Live
	private boolean isOn = true;
	private int timesRun = 0; // times triggered
	public int getTimesRun() { return timesRun; }
	private boolean isTriggering = false; // prevent stacked delayed executions
	private BukkitRunnable runnable;
	private boolean playerFound = false; // prevent stream of executions
	
	//-------------------------------------------------------------
	// Constructors
	public Detector(Player player, Block mainBlock, String name, Dungeon dungeon) {
		this.dungeon = dungeon;
		this.mainBlock = mainBlock;
		this.name = name;
		
		blockList.add(mainBlock);
		boolean foundMore = true;
		Block tmpB;
		while (blockList.size() < 100 && foundMore) {
			foundMore = false;
			List<Block> tempList = new ArrayList<Block>();
			for (Block b : blockList) {
				for (BlockFace bf : Tools.faces()) {
					tmpB = b.getRelative(bf);
					if (tmpB.getType() == Material.COAL_ORE && !tempList.contains(tmpB) && !blockList.contains(tmpB)) {
						tempList.add(tmpB);
						foundMore = true;
					}
				}
			}
			blockList.addAll(tempList);
		}
		if (blockList.size() >= 100)
			player.sendMessage("Too many potential detector blocks found. Truncating.");
		dbInsert();
		Tools.saySuccess(player, "Detector created!");
	}
	
	public Detector(Map<String,Object> data, Dungeon dungeon) {
		id = (int) data.get("id");
		this.name = (String) data.get("name");
		this.dungeon = dungeon;
		
		try {
			String jsonStr = (String) data.get("data");
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(jsonStr);
			mainBlock = Tools.blockFromXYZ((String) json.get("location"), dungeon.getWorld());
			if(json.containsKey("defaultOnOff")) defaultOnOff = (boolean) json.get("defaultOnOff");
			if(json.containsKey("times_min")) minTimes = ((int)((long) json.get("times_min")));
			if(json.containsKey("times_max")) maxTimes = ((int)((long) json.get("times_max")));
			if(json.containsKey("blocks")) {
				String blockString = (String) json.get("blocks");
				String[] blocks = blockString.split(";");
				for (String blk : blocks) {
					blockList.add(Tools.blockFromXYZ(blk, dungeon.getWorld()));
				}
			}
			if(json.containsKey("entermode")) entermode = (boolean) json.get("entermode");
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void dbInsert() {
		// Insert into DB
		StringJoiner blocks = new StringJoiner(";");
		for (Block b : blockList) {
			blocks.add(Tools.xyzAsString(b.getX(), b.getY(), b.getZ()));
		}
		JSONObject json = new JSONObject();
		json.put("location", Tools.xyzAsString(mainBlock.getX(), mainBlock.getY(), mainBlock.getZ()));
		json.put("defaultOnOff", defaultOnOff);
		json.put("times_max", maxTimes);
		json.put("times_min", minTimes);
		json.put("blocks", blocks.toString());
		json.put("entermode", entermode);
		
		id = new DatabaseInserter("qblock")
				.dungeon_id(dungeon.getDungeonId())
				.type_id("PLAYER_DETECTOR")
				.name(name)
				.add("data", json.toString())
				.execute();
	}
	
	//-------------------------------------------------------------
	// Execution
	@Override
	public void setEnabled(boolean bool) {
		enabled = bool;
		if (enabled) {
			for (Block b : blockList)
				b.setType(Material.AIR);
		} else {
			for (Block b : blockList)
				b.setType(Material.COAL_ORE);
		}
		timesRun = 0;
		isOn = defaultOnOff;
		isTriggering = false;
		playerFound=false;
	}
	
	public void run() {
		timesRun++;
		if(timesRun >= minTimes) {
			isTriggering=false;	
			
			//send the message to the receivers
			for (Receiver r : links.keySet()) {
				links.get(r).call(r);
			}
		}
	}
	@Override
	public void trigger() {
		if(enabled && isOn && (timesRun < maxTimes) && (timesRun+1 >= minTimes)) {
			if(delay<0) delay=0;
			if(delay>1200) delay=1200;
			if (delay==0 && !isTriggering) {
				run(); // do a delay thread instead when there is one
			} else {
				runnable=new BukkitRunnable() {
					@Override
		            public void run() {
						Detector.this.run();
					}
				};
				runnable.runTaskLater(JavaPlugin.getPlugin(QuestCrafter.class), delay);
				isTriggering=true;
			}
		}
	}
	@Override
	public void reset() {
		if(!enabled) return; // dungeon running, not isOn
		isOn = defaultOnOff;
		timesRun = 0;
		if(runnable != null)
			runnable.cancel();
	}
	@Override
	public void on() { if(enabled) isOn = true; }
	@Override
	public void off() { 
		if(enabled) {
			isOn = false; 
			if(runnable != null)
				runnable.cancel();
		} 
	}
	
	@Override
	public boolean hasBlock(Block b) { return blockList.contains(b); }
	public void destroy() {
		for (Block b : blockList)
			b.setType(Material.AIR);
	}
	
	//-------------------------------------------------------------
	// Links
	public boolean setTarget(Receiver target, Link linkType) {
		try {
			if (links.containsKey(target)) links.remove(target);
			links.put(target, linkType);
			return true;
		} catch (Exception e) { return false; }
	}
	public HashMap<Receiver, Link> getLinks(){ return links; }
	public boolean removeLink(Receiver receiver) {
		if (links.containsKey(receiver)) {
			links.remove(receiver);
			return true;
		}
		return false;
	}
	public void clearLinks() {
		links.clear();
		DatabaseMethods.removeAllLinks(dungeon.getDungeonId(), id);
	}
	public void displayTargets(Player p) {
		for (Receiver r : links.keySet()) {
			p.sendMessage("  Links to " + r.name() + "("+links.get(r).NAME+")");
		}
	}
	
	//-------------------------------------------------------------
	// Config
	
	@Override
	public void show(Player p) {
		p.sendMessage(prp + "Detector \"" + name + "\":");

		String enable = "enabled";
		if (!enabled) enable = "disabled";
		p.sendMessage(prp + "  Currently " + enable + ".");
		
		String def = "ON";
		if (!defaultOnOff) def = "OFF";
		String on = "ON";
		if (!isOn) on = "OFF";
		p.sendMessage(prp + "  Is " + on + ","+r+" default"+prp+"s to " + def + ".");
			
		String maxTimeStr = "unlimited";
		if (maxTimes < Integer.MAX_VALUE) maxTimeStr = "" + maxTimes;
		p.sendMessage(r + "  Max"+prp+" runs: " + maxTimeStr);
		String minTimeStr = "unlimited";
		if (minTimes > Integer.MIN_VALUE) minTimeStr = "" + minTimes;
		p.sendMessage(r + "  Min"+prp+" runs: " + minTimeStr);
		
		p.sendMessage(prp + "  Times run: " + timesRun);
		p.sendMessage(prp + "  Main block: " + mainBlock.getX() + " " + mainBlock.getY() + " " + mainBlock.getZ());
		
		for (Receiver r : links.keySet()) {
			p.sendMessage(prp + "  Links to " + r.name() + " ("+links.get(r).NAME+")");
			Tools.showLine(mainBlock.getWorld(), this, r);
		}
	}
	
	public void edit(Player p, String key, String value) {	
		if (key.toLowerCase().equals("default")) {
			if (value.toLowerCase().equals("on"))
				defaultOnOff = true;
			else
				defaultOnOff = Boolean.parseBoolean(value); 
			p.sendMessage(prp + "  Default set to " + defaultOnOff + ".");
		} else if (key.toLowerCase().equals("delay")) {
			int inputdelay = 0;
			try { 
				inputdelay = Integer.parseInt(value); 
		    } catch(NumberFormatException e) { 
		    	p.sendMessage(red + "invalid number \"" + value + "\".");
		        return; 
		    } catch(NullPointerException e) {
		    	p.sendMessage(red + "invalid number \"" + value + "\".");
		        return;
		    }
			delay = inputdelay;
			double seconds = delay/20;
			p.sendMessage(prp + "  Delay set to " + seconds + " seconds (" + delay + " ticks)");
		} else if (key.toLowerCase().equals("max")) {
			int count = 0;
			try { 
		        count = Integer.parseInt(value); 
		    } catch(NumberFormatException e) { 
		    	p.sendMessage(red + "invalid number \"" + value + "\".");
		        return; 
		    } catch(NullPointerException e) {
		    	p.sendMessage(red + "invalid number \"" + value + "\".");
		        return;
		    }
			maxTimes = count;
			p.sendMessage(prp + "  Max set to " + count + " triggers.");
		} else if (key.toLowerCase().equals("min")) {
			int count = 0;
			try { 
		        count = Integer.parseInt(value); 
		    } catch(NumberFormatException e) { 
		    	p.sendMessage(red + "invalid number \"" + value + "\".");
		        return; 
		    } catch(NullPointerException e) {
		    	p.sendMessage(red + "invalid number \"" + value + "\".");
		        return;
		    }
			minTimes = count;
			p.sendMessage(prp + "  Min set to " + (count-1) + "pre-triggers needed.");
		} else if (key.toLowerCase().equals("mode")) {
			value=value.toLowerCase();
			if(!value.equals("enter") && !value.equals("leave")) {
				p.sendMessage(red + "invalid mode \"" + value + "\".");
		        return;
			} else {
				entermode = !value.equals("leave"); // true=enter, false=leave, default to enter
				p.sendMessage(prp + "  Mode set to " + value + ".");
			}
		}
		dbInsert();
	}
	
	static String prp = "" + ChatColor.LIGHT_PURPLE;
	static String r = "" + ChatColor.RESET;
	static String red = "" + ChatColor.RED;
	
	//####################################################################################
	//####################################################################################
	//####################################################################################
	// Primary
	
	// run block on enter/leave
	public void testPlayers(List<Player> players) {
		// isWaiting :
		// we accidentally confused isWaiting, which indicated delaying and isTriggering, which prevents it running over and over when a player is in it
		// renamed isWaiting to PlayerFound
		boolean found = false; //check if player is found and see if they entered or left
		if(enabled && isOn && !isTriggering && 
				(timesRun < maxTimes)) {
			for (Player p : players) {
				if (blockList.contains(p.getLocation().getBlock()) || 
						blockList.contains(p.getEyeLocation().getBlock())) {
					found = true;
					break;
				}
			}	
		}
		if(entermode) { // on player enter
			if(!playerFound && found) {
				playerFound=found;
				run();
			}
		} else { // on player leave
			if(playerFound && !found) {
				playerFound=found;
				run();
			}
		}
		playerFound=found;
	}
	
}
