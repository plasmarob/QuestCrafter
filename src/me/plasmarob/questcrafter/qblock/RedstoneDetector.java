package me.plasmarob.questcrafter.qblock;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import me.plasmarob.questcrafter.Dungeon;
import me.plasmarob.questcrafter.database.DatabaseInserter;
import me.plasmarob.questcrafter.util.Tools;

public class RedstoneDetector implements Sender, Receiver {
	private Dungeon dungeon;
	private int id = -1;
	public int getID() { return this.id; }
	public void setID(int id) { this.id= id; }
	private String name;
	private boolean enabled = false;
	private boolean defaultOnOff = true;
	private boolean isOn = true;
	private boolean inverted = false;
	private double delay = 0;	//TODO: add a delay
	
	private int maxTimes = -1; //max times it can be triggered (-1 infinite)
	private int timesRun = 0; //how many times triggered
	
	Block mainBlock;
	HashMap<Receiver, Link> links = new HashMap<Receiver, Link>();
	
	//-------------------------------------------------------------
	// Constructors
	public RedstoneDetector(Player player, Block mainBlock, String name, Dungeon dungeon) {
		this.dungeon = dungeon;
		this.mainBlock = mainBlock;
		this.name = name;
		dbInsert();
		Tools.saySuccess(player, "Detector created!");
	}
	
	/*
	public RedstoneDetector(Block mainBlock, String name, Dungeon dungeon) {	
		this.name = name;
		this.mainBlock = mainBlock;
		dbInsert();
	}
	*/
	
	public RedstoneDetector(Map<String,Object> data, Dungeon dungeon) {
		this.name = (String) data.get("name");
		this.dungeon = dungeon;
		mainBlock = Tools.blockFromXYZ((String) data.get("location"), dungeon.getWorld());
		defaultOnOff = Boolean.parseBoolean((String) data.get("default"));
		inverted = Boolean.parseBoolean((String) data.get("inverted"));
		maxTimes = (int) data.get("times");
	}
	
	public void dbInsert() {
		// Insert into DB
		new DatabaseInserter("block")
				.dungeon_id(dungeon.getDungeonId())
				.type_id("REDSTONE_DETECTOR")
				.name(name)
				//.location(mainBlock.getX(), mainBlock.getY(), mainBlock.getZ())
				.add("default", defaultOnOff)
				.add("inverted", inverted)
				.add("times", maxTimes)
				.execute();
	}
	
	public Block getMainBlock() {
		return mainBlock;
	}
	
	@Override
	public HashMap<Receiver, Link> getLinks(){
		return links;
	}
	
	@Override
	public boolean setTarget(Receiver target, Link linkType) {
		try {
			if (links.containsKey(target)) links.remove(target);
			links.put(target, linkType);
			return true;
		} catch (Exception e) { return false; }
	}

	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean bool) {
		enabled = bool;
		if (enabled) {
			mainBlock.setType(Material.AIR);
		} else {
			mainBlock.setType(Material.REDSTONE_ORE);
		}
		timesRun = 0;
	}
	public boolean isDefaultOnOff() {
		return defaultOnOff;
	}
	public void setDefaultOnOff(boolean defaultOnOff) {
		this.defaultOnOff = defaultOnOff;
	}
	public boolean isInverted() {
		return inverted;
	}
	public void setInverted(boolean inverted) {
		this.inverted = inverted;
	}
	public void setDelay(int delay) {
		this.delay = delay;
	}
	public double getDelay() {
		return delay;
	}
	
	public int getMaxTimes() {
		return maxTimes;
	}
	public void setMaxTimes(int maxTimes) {
		this.maxTimes = maxTimes;
	}
	public int getTimesTriggered() {
		return timesRun;
	}

	public void displayTargets(Player p) {
		for (Receiver r : links.keySet()) {
			p.sendMessage("  Links to " + r.name() + "("+links.get(r).NAME+")");
		}
	}
	
	public void testForRedstone(Block block) {
		if (mainBlock.equals(block)) {
			trigger();
			return;
		}
				
		for (BlockFace bf : Tools.faces())
		{
			if (mainBlock.equals(block.getRelative(bf))) {
				trigger();
				return;
			}
		}
	}
	
	@Override
	public void trigger() {
		if (enabled && isOn && (maxTimes == -1 || timesRun < maxTimes)) {
				run();
		}
	}
	@Override
	public void reset() {
		// Does nothing for now.
	}
	@Override
	public void on() {
		if (enabled)
			isOn = true;
	}
	@Override
	public void off() {
		if (enabled)
			isOn = false;
	}
	
	@Override
	public void run() {
		timesRun++;
		//send the message to the receivers
		for (Receiver r : links.keySet()) {
			links.get(r).call(r);
		}
	}

	public boolean removeLink(Receiver block) {
		if (links.containsKey(block)) {
			links.remove(block);
			return true;
		}
		return false;
	}

	public void clearLinks() {
		links.clear();
	}

	@Override
	public String type() {
		return "rsDetector";
	}

	@Override
	public String name() {
		return name;
	}

	static String prp = "" + ChatColor.LIGHT_PURPLE;
	static String r = "" + ChatColor.RESET;
	@Override
	public void show(Player p) {
		p.sendMessage(prp + "Redstone Detector \"" + name + "\":");

		String enable = "enabled";
		if (!enabled) enable = "disabled";
		p.sendMessage(prp + "  Currently " + enable + ".");

		String def = "ON";
		if (!defaultOnOff) def = "OFF";
		String on = "ON";
		if (!isOn) on = "OFF";
		p.sendMessage(prp + "  Is " + on + ","+r+" default"+prp+"s to " + def + ".");
		
		p.sendMessage(r + "  Inverted"+prp+"?: " + inverted);
		
		String maxTimeStr = "unlimited";
		if (maxTimes != -1) maxTimeStr = "" + maxTimes;
		p.sendMessage(r + "  Max"+prp+" runs: " + maxTimeStr);
		p.sendMessage(prp + "  Times run: " + timesRun);
		p.sendMessage(prp + "  Main block: " + mainBlock.getX() + " " + mainBlock.getY() + " " + mainBlock.getZ());
		
		for (Receiver r : links.keySet()) {
			p.sendMessage(prp + "  Links to " + r.name() + " ("+links.get(r).NAME+")");
			Tools.showLine(mainBlock.getWorld(), this, r);
		}
	}

	

	public void destroy() {
		mainBlock.setType(Material.AIR);
	}

	String red = "" + ChatColor.RED;
	public void edit(Player p, String key, String value) {	
		if (key.toLowerCase().equals("default")) {
			if (value.toLowerCase().equals("on"))
				defaultOnOff = true;
			else
				defaultOnOff = Boolean.parseBoolean(value); 
			p.sendMessage(prp + "  Default set to " + defaultOnOff + ".");
		} else if (key.toLowerCase().equals("inverted")) {
			inverted = Boolean.parseBoolean(value); 
			p.sendMessage(prp + "  Inverted set to " + inverted + ".");	
		} else if (key.toLowerCase().equals("delay")) {
			double count = 0;
			try { 
		        count = Double.parseDouble(value); 
		    } catch(NumberFormatException e) { 
		    	p.sendMessage(red + "invalid number \"" + value + "\".");
		        return; 
		    } catch(NullPointerException e) {
		    	p.sendMessage(red + "invalid number \"" + value + "\".");
		        return;
		    }
			delay = count;
			p.sendMessage(prp + "  Delay set to " + delay + " seconds (" + Math.round(delay*20) + " ticks)");
		}
		else if (key.toLowerCase().equals("max")) {
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
		}		
	}

	@Override
	public int getX() {
		return mainBlock.getX();
	}
	@Override
	public int getY() {
		return mainBlock.getY();
	}
	@Override
	public int getZ() {
		return mainBlock.getZ();
	}
	
	@Override
	public boolean hasBlock(Block b) {
		return b.equals(mainBlock);
	}
	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}
}
