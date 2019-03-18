package me.plasmarob.questcrafter.qblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import me.plasmarob.questcrafter.Dungeon;
import me.plasmarob.questcrafter.QuestCrafter;
import me.plasmarob.questcrafter.database.DatabaseInserter;
import me.plasmarob.questcrafter.util.Tools;

public class Storage implements Receiver {
	// Dungeon
	private Dungeon dungeon;
	private boolean enabled = false;
	public boolean isEnabled() { return enabled; }
	// Object immutable
	private int id = -1;
	public int getID() { return this.id; }
	public void setID(int id) { this.id = id; }
	private String name;
	public String name() { return name; }
	public String type() { return "detector"; }
	public Block mainBlock;
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
    // Unique
	private Location min;
	private Location max;
	private FrameAnimator frameAnim;
	private String mode = "once";
	private boolean playAll = true;
	private boolean reverse = false;
	// Live
	//private boolean isAnimating = false;
	private boolean isOn = true;
	private int timesRun = 0; // times triggered
	public int getTimesRun() { return timesRun; }
	
	//-------------------------------------------------------------
	// Constructors
	public Storage(Player player, Block block, String name, Dungeon dungeon) {
		this.dungeon = dungeon;
		this.mainBlock = block;
		this.name = name;
		
		//TODO
		/*
		Selection sel = QuestCrafter.getWE().getSelection(player);
		min = sel.getMinimumPoint();
		max = sel.getMaximumPoint();
		//min = sel.getNativeMinimumPoint();
        //max = sel.getNativeMaximumPoint();
        List<BlockState> bsList = new ArrayList<BlockState>();
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
        	for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
        		for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
        			bsList.add(player.getWorld().getBlockAt(x, y, z).getState());	
                }
            }
        }  
        */
        frameAnim = new FrameAnimator(min, max);
        dbInsert();
        //Tools.saySuccess(player, "Storage created!");
	}
	
	public Storage(Map<String,Object> data, String animJSON, Dungeon dungeon) {
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
			if(json.containsKey("mode")) mode = (String) json.get("mode");
			this.min = Tools.weLocationFromString((String) json.get("min"),dungeon.getWorld());
			this.max = Tools.weLocationFromString((String) json.get("max"),dungeon.getWorld());
			frameAnim = new FrameAnimator(min, max, animJSON);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void dbInsert() {
		JSONObject json = new JSONObject();
		json.put("location", Tools.xyzAsString(mainBlock.getX(), mainBlock.getY(), mainBlock.getZ()));
		json.put("defaultOnOff", defaultOnOff);
		json.put("min", Tools.xyzAsString((int)min.getX(), (int)min.getY(), (int)min.getZ()));
		json.put("max", Tools.xyzAsString((int)max.getX(), (int)max.getY(), (int)max.getZ()));
		json.put("times_min", minTimes);
		json.put("times_max", maxTimes);
		json.put("mode", mode);
		
		id = new DatabaseInserter("qblock")
						.dungeon_id(dungeon.getDungeonId())
						.type_id("STORAGE")
						.name(name)
						.add("data", json.toString())
						.execute();
		new DatabaseInserter("storageFrameAnim")
				.add("storage_id", id)
				.add("data", frameAnim.getAsJSON().toString())
				.execute();
		//TODO enhancement: add special block data to anim JSON
	}
	
    //-------------------------------------------------------------
  	// Execution
    @Override
	public void setEnabled(boolean bool) {
		enabled = bool;
		if(enabled) {
			mainBlock.setType(Material.AIR);
		} else {
			off();
			reset();
			mainBlock.setType(Material.EMERALD_ORE);
		}
		timesRun = 0;
		isOn = defaultOnOff;
	}
    
    @Override
	public void run() {
	
	}
    @Override
	public void trigger()  {	
		if(enabled) {
			if(!playAll) {
				frameAnim.advance();
			} else {
				if(!isOn) {
					on();
				} else {
					off();
				}
			}
		}
	}
    @Override
	public void reset() {
		if(!enabled) return; // dungeon running, not isOn
		//timesRun = 0;
		frameAnim.showFrame(0);
		isOn = defaultOnOff;
	}
    @Override
	public void on() { 
    	if (enabled) {
    		frameAnim.play();
    		isOn = true; 
    	}
    }
    @Override
    public void off() {
    	frameAnim.pause();
		isOn = false;
	}

	public void pause() {
	}

	@Override
	public boolean hasBlock(Block b) {
		if (b.equals(mainBlock))
			return true;
		for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
        	for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
        		for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
        			if (b.equals(mainBlock.getWorld().getBlockAt(x, y, z)))
        				return true;
                }
            }
        }
		return false;
	}
	public void destroy() {
		mainBlock.setType(Material.AIR);
	}

	//-------------------------------------------------------------
	// Links
	
	//-------------------------------------------------------------
	// Config

	@Override
	public void show(Player p) {
		p.sendMessage(prp + "Storage Block \"" + name + "\":");

		String enable = "enabled";
		if (!enabled) enable = "disabled";
		p.sendMessage(prp + "  Currently " + enable + ".");
		
		String def = "ON";
		if (!defaultOnOff) def = "OFF";
		String on = "ON";
		if (!isOn) on = "OFF";
		p.sendMessage(prp + "  Is " + on + ","+r+" default"+prp+"s to " + def + ".");
		p.sendMessage(prp + "  Block: " + mainBlock.getX() + " " + mainBlock.getY() + " " + mainBlock.getZ());
		p.sendMessage(prp + "  Min XYZ: " + min.getX() + " / " + min.getY() + " / " + min.getZ() + "");
		p.sendMessage(prp + "  Max XYZ: " + max.getX() + " / " + max.getY() + " / " + max.getZ() + "");
		
		String framelist = prp + frameAnim.size() + " (0"; // (0) or (0-9)
		if(frameAnim.size()>1) framelist += "-"+(frameAnim.size()-1)+")"; else framelist += ")";
		p.sendMessage(prp + "  Frames: " + framelist);
		int total = frameAnim.getTotalTicks();
		double seconds = total / 20;
		p.sendMessage(prp + "  Animate time: " + total + " ticks (" + seconds + " sec)");
	}
	
	public void showframe(Player p, int fnum) {
		p.sendMessage(prp + "Storage Block \"" + name + "\" Frame "+fnum+":");
		if(fnum < 0 || fnum >= frameAnim.size()) {
			return;
		}
		p.sendMessage(prp + "Ticks: " + frameAnim.getTicks(fnum));
		frameAnim.showFrame(fnum);
	}
	
	@Override
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
		} else if (key.toLowerCase().equals("mode")) {
			value=value.toLowerCase();
			if(!value.equals("once") && !value.equals("loop") && !value.equals("bounce")) {
				p.sendMessage(red + "invalid mode \"" + value + "\".");
		        return;
			} else {
				if(!enabled) {
					mode = value;
					frameAnim.updateMode();
					p.sendMessage(prp + "  Mode set to " + value + ".");
				}
				else
					p.sendMessage("Cannot change mode while dungeon is enabled.");
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
	
	// Create
	public void addFrame() {
		addFrame(20);
	}
	public void addFrame(int time) {
		addFrame(time,frameAnim.size());
	}
    public void addFrame(int ticks, int at_frame) {
    	if (ticks < 1) ticks = 1;
    	if (ticks > 600) ticks = 600; // 30 sec
    	if (at_frame < 0) at_frame = 0;
    	//Bukkit.getConsoleSender().sendMessage("frameAnim.insertFrame("+at_frame+","+ticks+");");
    	frameAnim.insertFrame(at_frame,ticks);
    	dbInsert();
	}
    
    // Read
    public void showFrame(Player p, int index) {
		if(!enabled) {
			frameAnim.showFrame(index);
			p.sendMessage(prp + "Showing frame "+index+".");
		}
		else
			p.sendMessage("Cannot show frame while dungeon is enabled.");
	}
    
    public int getAnimLength() {
    	return frameAnim.size();
    }
    public void animate(Player p) {
    	if(!enabled) {
    		frameAnim.restartPlay();
		}
		else
			p.sendMessage("Cannot manually play animation while dungeon is enabled.");
    }
	
    // Update
    public void replaceFrame(Player p, int index, int ticks) {
    	if(!enabled) {
    		frameAnim.updateFrame(index, ticks);
			p.sendMessage(prp + "Updating frame "+index+".");
			dbInsert();
		}
		else
			p.sendMessage("Cannot update frame while dungeon is enabled.");
	}
    
    // Delete
    public void removeFrame(Player p, int index) {
    	if(!enabled) {
    		frameAnim.deleteFrame(index);
			p.sendMessage(prp + "Removing frame "+index+".");
			dbInsert();
		}
		else
			p.sendMessage("Cannot remove frame while dungeon is enabled.");
    }
    public void removeAllFrames() {
    	//DatabaseMethods.deleteAllStorageFrames(id);
    	//frames.clear();
    }
    
    
    /**
     * Helpers
     * - FrameAnimator - single parent controller
     * - Frame
     * - FullFrame - extends Frame with all blocks
     * - BlockDelta - diff parser
     * - BlockInfo - data unit
	 */
	@SuppressWarnings("unused")
	private class FrameAnimator {
		
		Location min;
		Location max;
		FullFrame coreFrame; // core frame object with all blocks
		ArrayList<Frame> frameList = new ArrayList<Frame>();
		public int size() { return frameList.size(); }
		public int getTicks(int frame) { return frameList.get(frame).getTicks(); }
		public void setTicks(int frame, int ticks) { frameList.get(frame).setTicks(ticks); }
		public int getTotalTicks() {
			int t = 0;
			for(Frame f : frameList) { t += f.getTicks(); }
			return t;
		}
		
		int currentFrame = 0;
		public int getCurrentFrame() { return currentFrame; }
		int lastRealFrame = 0; // if we want some frames to be fake... TODO
		
		//-------------------------------------------------------------
		// Constructors
		public FrameAnimator(Location min, Location max) {
			this.min = min;
			this.max = max;
			coreFrame = new FullFrame(); // new based on current blocks, using min and max
			frameList.add(0, coreFrame);
		}
		public FrameAnimator(Location min, Location max, String jsonStr) {
			this.min = min;
			this.max = max;
			try {
				JSONParser parser = new JSONParser();
				JSONObject json = (JSONObject) parser.parse(jsonStr);
				if(json.containsKey("frames")) {
					JSONArray frames = (JSONArray) json.get("frames");
					@SuppressWarnings("rawtypes")
					Iterator it = frames.iterator();
					JSONObject f0 = (JSONObject) it.next();
					//log(f0.toString());
					coreFrame = new FullFrame(f0);
					frameList.add(0,coreFrame);
					while(it.hasNext()) {
						JSONObject f = (JSONObject) it.next();
						//log(",");
						frameList.add(new Frame(f));
						//log(f.toString());
					}
				}	
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		
		public void start() {
			
		}
		// TODO: pause & play again, LOOP and BOUNCE
		public void play() {
			onceThread();
		}
		public void restartPlay() {
			showFrame(0);
			onceThread();
		}
		private boolean cancel=false;
		public void pause() {
			Plugin plugin = JavaPlugin.getPlugin(QuestCrafter.class);
			BukkitScheduler scheduler = plugin.getServer().getScheduler();
			if(animationTaskNumber>=0) {
				scheduler.cancelTask(animationTaskNumber);
				animationTaskNumber=-1;
			}
		}
		/**
		 * Creates the animation thread and runs it
		 */
		
		private int animationTaskNumber=-1;
		public void onceThread() {
			if(animationTaskNumber>=0) return; //don't allow multiple runs;
			
			Plugin plugin = JavaPlugin.getPlugin(QuestCrafter.class);
			BukkitScheduler scheduler = plugin.getServer().getScheduler();
			
			animationTaskNumber = scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
				int counter = frameList.get(getCurrentFrame()).getTicks();
				int lastFrame = frameList.size()-1;
				@Override
	            public void run() {
					counter--;
					if(counter<=0) {
						
						if(currentFrame+1>lastFrame) {
							scheduler.cancelTask(animationTaskNumber);
							animationTaskNumber=-1;
						} else {
							boolean b = frameList.get(currentFrame).playNext();
							counter = frameList.get(currentFrame+1).getTicks();
						}
						currentFrame += 1;
					}
	            }
	        }, 1L, 1L);
		}
		
		public void advance() {
			if(mode.equals("once")) {
				if(currentFrame < frameList.size()-1) {
					frameList.get(currentFrame).playNext();
					currentFrame++;
				}
			} else if(mode.equals("loop")) {	// loop back to start
				if(currentFrame < frameList.size()) {
					frameList.get(currentFrame).playNext();
					currentFrame++;
					if(currentFrame >= frameList.size()) currentFrame=0;
				}
			} else if(mode.equals("bounce")) { // reverse direction
				if(!reverse) {
					if(currentFrame >= frameList.size()-1) {
						reverse=true;
						frameList.get(currentFrame).playPrevious();
						currentFrame--;
					} else {
						frameList.get(currentFrame).playNext();
						currentFrame++;
					}
				} else if (reverse) {
					if(currentFrame <= 0) {
						reverse=false;
						frameList.get(currentFrame).playNext();
						currentFrame++;
					} else {
						frameList.get(currentFrame).playPrevious();
						currentFrame--;
					}
				}
			}
		}
	
		//-------------------------------------------------------------
		public void insertFrame(int pos) {
			insertFrame(pos, 20);
		}
		public void insertFrame(int pos, int ticks) {
			//Bukkit.getConsoleSender().sendMessage("" + pos + "--" + ticks);
			if(pos == 0) { // handle updating first frame
				// ALL of these must be initialized before meddling with blocks
				FullFrame oldCoreframe = (FullFrame) frameList.get(0);
				FullFrame newCoreFrame = new FullFrame();
				newCoreFrame.setTicks(ticks);
				BlockDelta delta = new BlockDelta(newCoreFrame, oldCoreframe);
				
				Frame replaceOldFrame = new Frame(oldCoreframe); //downgrade frame
				
				replaceOldFrame.setPrevious(delta.getBackward()); // old frame has moved forward
				newCoreFrame.setNext(delta.getForward());
				frameList.add(0, newCoreFrame);
				frameList.set(1, replaceOldFrame);
				dbInsert();
			} else if (pos > 0) { // handle middle and end
				if (pos > frameList.size()) pos = frameList.size();
				// ALL of these must be initialized before meddling with blocks
				FullFrame tempNewFullFrame = new FullFrame();
				tempNewFullFrame.setTicks(ticks);
				FullFrame tempPrevFrame = new FullFrame(pos-1);
				FullFrame tempNextFrame = new FullFrame(pos);
				BlockDelta delta1 = new BlockDelta(tempPrevFrame, tempNewFullFrame);
				BlockDelta delta2 = new BlockDelta(tempNewFullFrame, tempNextFrame);
				// Make changes
				frameList.get(pos-1).setNext(delta1.getForward());
				tempNewFullFrame.setPrevious(delta1.getBackward());
				if(pos < frameList.size()) { // if not last (if middle)
					tempNewFullFrame.setNext(delta2.getForward());
					frameList.get(pos).setPrevious(delta2.getBackward());
				}
				// we don't want to store the WHOLE frame! just a basic diff one
				Frame newFrame = new Frame(tempNewFullFrame);
				frameList.add(pos, newFrame);
				dbInsert();
			} else { // error - neg or > all
			}
		}
		
		
		public void logBlockDelta(HashMap<BlockInfo,HashSet<Location>> data) {
			for(BlockInfo bi : data.keySet()) {
				for(Location loc : data.get(bi)) {			
				}
			}
		}
		public void log(String s) {
			Bukkit.getConsoleSender().sendMessage(s);
		}
		
		public void showFrame(int index) {
			//Bukkit.getConsoleSender().sendMessage(""+index);
			Frame f=frameList.get(index);
			//Bukkit.getConsoleSender().sendMessage(f.getAsJSON().toString());
			
			FullFrame chosenFrame = new FullFrame(index); //FullFrame constructor heavy lifting
			List<BlockInfo> list = chosenFrame.getBlockList();
			int i = 0;
			BlockInfo bi;
			World w = dungeon.getWorld();
	        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
	        	for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
	        		for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
	        			bi=list.get(i);
	        			//TODO: 
//						w.getBlockAt(x, y, z).setTypeIdAndData(bi.id(), bi.getData(), false);
//						w.getBlockAt(x, y, z).setType();
						i++;
	        }}}
	        currentFrame=index;
		}
		public void showFrameFake(int index) {
			FullFrame chosenFrame = new FullFrame(index);
			List<BlockInfo> list = chosenFrame.getBlockList();
			BlockInfo bi;
			World w = dungeon.getWorld();
			for ( Player p : dungeon.getPlayers()) {
				int i = 0;
		        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
		        	for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
		        		for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
		        			bi=list.get(i);
		        			//TODO:
//		        			Material mat = Material.getMaterial(bi.id());
//							p.sendBlockChange(new Location(w,x,y,z), Material.getMaterial(bi.id()), bi.getData());
							i++;
		        }}}	
			}
		}

		public void updateFrame(int index, int ticks) {
			if(ticks<1) ticks=1;
			if(ticks>600)ticks=600;
			if(index==0) {
				FullFrame newFullFrame = new FullFrame(); //current state
				FullFrame nextFrame = new FullFrame(index+1);
				BlockDelta delta = new BlockDelta(newFullFrame, nextFrame);
				
				newFullFrame.setNext(delta.getForward());
				frameList.get(index+1).setPrevious(delta.getBackward());
				newFullFrame.setTicks(ticks);
				frameList.set(index, newFullFrame);
				updateMode();
			} else if (index < frameList.size()-1) {
				FullFrame newFullFrame = new FullFrame(); //current state
				FullFrame previousFrame = new FullFrame(index-1);
				FullFrame nextFrame = new FullFrame(index+1);
				BlockDelta delta1 = new BlockDelta(previousFrame, newFullFrame);
				BlockDelta delta2 = new BlockDelta(newFullFrame, nextFrame);
				Frame newFrame = new Frame(newFullFrame);
				
				newFrame.setPrevious(delta1.getBackward());
				frameList.get(index-1).setNext(delta1.getForward());
				newFrame.setNext(delta2.getForward());
				frameList.get(index+1).setPrevious(delta2.getBackward());
				newFrame.setTicks(ticks);
				frameList.set(index, newFrame);
			} else if (index == frameList.size()-1) {
				FullFrame newFullFrame = new FullFrame(); //current state
				FullFrame previousFrame = new FullFrame(index-1);
				BlockDelta delta = new BlockDelta(previousFrame, newFullFrame);
				Frame newFrame = new Frame(newFullFrame);
				
				newFrame.setPrevious(delta.getBackward());
				frameList.get(index-1).setNext(delta.getForward());
				newFrame.setTicks(ticks);
				frameList.set(index, newFrame);
				updateMode();
			}
			dbInsert();
		}
		
		// Update loop to point at each other
		public void updateMode() {
			if(mode.equals("once") || mode.equals("bounce")) {
				frameList.get(0).setPrevious(null);
				frameList.get(frameList.size()-1).setNext(null);
			} else if (mode.equals("loop")) {
				FullFrame firstFrame = (FullFrame) frameList.get(0);
				FullFrame lastFrame = new FullFrame(frameList.size()-1);
				BlockDelta delta = new BlockDelta(lastFrame, firstFrame);
				frameList.get(frameList.size()-1).setNext(delta.getForward());
				frameList.get(0).setPrevious(delta.getBackward());
			}
		}
		
		public void deleteFrame(int index) {
			if(index==frameList.size()-1) { // last index
				frameList.get(index-1).setNext(null);
				frameList.remove(index);
				updateMode();
			} else if (index==0) {
				// Upgrade index 1 to full frame
				FullFrame nextFrame = new FullFrame(1);
				nextFrame.setNext(frameList.get(1).getNext()); // (previous will be null)
				frameList.set(1, nextFrame);
				frameList.remove(0);
				updateMode();
			} else if (index > 0 && index < frameList.size()-1) {
				// point prev and next at each other
				FullFrame previousFrame = new FullFrame(index-1);
				FullFrame nextFrame = new FullFrame(index+1);
				BlockDelta delta = new BlockDelta(previousFrame, nextFrame);
				frameList.get(index-1).setNext(delta.getForward());
				frameList.get(index+1).setPrevious(delta.getBackward());
				//previousFrame.setNext(delta.getForward());
				//nextFrame.setPrevious(delta.getBackward());
				frameList.remove(index);
			}
			dbInsert();
		}
		
		public ArrayList<BlockInfo> cloneBIList(List<BlockInfo> list) {
			ArrayList<BlockInfo> clone = new ArrayList<BlockInfo>(list.size());
		    for (BlockInfo b : list) {
		    	clone.add(b.clone());
		    }
		    return clone;
		}	
		@SuppressWarnings("unchecked")
		public JSONObject getAsJSON() {
			JSONObject json = new JSONObject();
			String minStr = Tools.xyzAsString(min.getBlockX(),min.getBlockY(),min.getBlockZ());
			String maxStr = Tools.xyzAsString(max.getBlockX(),max.getBlockY(),max.getBlockZ());
			json.put("min", minStr);
			json.put("max", maxStr);
			JSONArray frames = new JSONArray();
			for(Frame f : frameList) {
				frames.add(f.getAsJSON());
			}
			json.put("frames", frames);
			return json;
		}
		
		//###############################################################################
		// Helper Internal Classes
		// Frame->FullFrame, BlockDelta, BlockInfo
		private class Frame {
			private HashMap<BlockInfo,HashSet<Location>> nextFrameDiff = null;
			public HashMap<BlockInfo,HashSet<Location>> getNext() { return nextFrameDiff; }
			public void setNext(HashMap<BlockInfo,HashSet<Location>> diff) { nextFrameDiff = diff; }
			private HashMap<BlockInfo,HashSet<Location>> previousFrameDiff = null;
			public HashMap<BlockInfo,HashSet<Location>> getPrevious() { return previousFrameDiff; }
			public void setPrevious(HashMap<BlockInfo,HashSet<Location>> diff) { previousFrameDiff = diff; }
			protected int ticks = 20;
			public int getTicks() { return ticks; }
			public void setTicks(int ticks) { this.ticks = ticks; }
			protected boolean fake = false;
			public boolean getFake() { return fake; }
			public void getFake(boolean fake) { this.fake = fake; }
			
			private Frame() {
			}
			// Used to strip blocks from a FullFrame
			public Frame(Frame oldFrame) {
				this.nextFrameDiff = oldFrame.nextFrameDiff;
				this.previousFrameDiff = oldFrame.previousFrameDiff;
				this.ticks = oldFrame.ticks;
				this.fake = oldFrame.fake;
			}
			@SuppressWarnings({ "rawtypes", "unchecked" })
			public Frame(JSONObject o) {
				if(o.containsKey("ticks")) this.ticks = ((int)((long)o.get("ticks")));
				if(o.containsKey("fake")) this.fake = (boolean)o.get("fake");	
				if(o.containsKey("forward")) {					
					Map f = (Map)o.get("forward");
					if (f == null) {
						nextFrameDiff = null;
					} else {
						nextFrameDiff = new HashMap<BlockInfo,HashSet<Location>>();
						Iterator<Map.Entry> it = f.entrySet().iterator();
						HashSet<Location> locSet;
					    while(it.hasNext()) {
					    	Map.Entry pair = it.next();
					    	BlockInfo bi = new BlockInfo((String)pair.getKey());
					    	String locStr = (String) pair.getValue();
					    	locSet = Tools.locationSetFromString(dungeon.getWorld(),locStr);
					    	nextFrameDiff.put(bi, locSet);
					    }
					}
					
				}
				if(o.containsKey("backward")) {					
					Map b = (Map)o.get("backward");
					if (b == null) {
						previousFrameDiff = null;
					} else {
						previousFrameDiff = new HashMap<BlockInfo,HashSet<Location>>();
						Iterator<Map.Entry> it = b.entrySet().iterator();
						HashSet<Location> locSet;
					    while(it.hasNext()) {
					    	Map.Entry pair = it.next();
					    	BlockInfo bi = new BlockInfo((String)pair.getKey());
					    	String locStr = (String) pair.getValue();
					    	locSet = Tools.locationSetFromString(dungeon.getWorld(),locStr);
					    	previousFrameDiff.put(bi, locSet);
					    }
					}
				}
			}
			
			/**
			 * ALL of this code was written to make these two methods
			 * as efficient as possible
			 */
			public boolean playNext() {
				if(nextFrameDiff == null) return false;
				for(BlockInfo b : nextFrameDiff.keySet()) {
					for(Location loc : nextFrameDiff.get(b)) {
						// TODO
//						loc.getBlock().setTypeIdAndData(b.id(), b.getData(), false);
					}
				}
				return true;
			}
			public boolean playPrevious() {
				if(previousFrameDiff == null) return false;
				for(BlockInfo b : previousFrameDiff.keySet()) {
					for(Location loc : previousFrameDiff.get(b)) {
						// TODO
//						loc.getBlock().setTypeIdAndData(b.id(), b.getData(), false);
					}
				}
				return true;
			}
			
			@SuppressWarnings("unchecked")
			public JSONObject getAsJSON() {
				JSONObject json = new JSONObject();
				json.put("ticks", ticks);
				json.put("fake", fake);
				if (nextFrameDiff == null) {
					json.put("forward", null);
				} else {
					JSONObject forward = new JSONObject();
					for (Map.Entry<BlockInfo, HashSet<Location>> entry : nextFrameDiff.entrySet()) {
						BlockInfo bi = entry.getKey();
						HashSet<Location> locs = entry.getValue();
						String locStr = Tools.locationSetAsString(locs);
						forward.put(bi.toString(), locStr);
					}
					json.put("forward", forward);
				}
				if (previousFrameDiff == null) {
					json.put("backward", null);
				} else {
					JSONObject backward = new JSONObject();
					for (Map.Entry<BlockInfo, HashSet<Location>> entry : previousFrameDiff.entrySet()) {
						BlockInfo bi = entry.getKey();
						HashSet<Location> locs = entry.getValue();
						String locStr = Tools.locationSetAsString(locs);
						backward.put(bi.toString(), locStr);
					}
					json.put("backward", backward);
				}
				return json;
			}
		}
		
		/**
		 * FullFrame
		 * - Object holding every block for index 0 
         * - Used to update delta frames
		 */
		private class FullFrame extends Frame {
			// Flattened 3D arrays of blocks
			ArrayList<BlockInfo> biList = new ArrayList<BlockInfo>();
			ArrayList<BlockInfo> getBlockList() { return biList; }
			//TODO: also store BlockData in 1.13 or on request from Brad
			// AHHHHH BlockState sucks KILL IT WITH FIRE
			
			public FullFrame() {	
				BlockState bs;
		        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
		        	for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
		        		for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
		        			bs = dungeon.getWorld().getBlockAt(x, y, z).getState();
		        			biList.add(new BlockInfo(bs));	
		        }}}
			}
			
			public FullFrame(JSONObject o) {
				super(o);
				if(o.containsKey("blocks")) {
					String blkStr = (String)o.get("blocks");
					String[] bA = blkStr.split(";");
					biList = new ArrayList<BlockInfo>();
					for(int i=0; i< bA.length; i++) {
						biList.add(new BlockInfo(bA[i]));
					}
				}
			}
			
			// Creates a FullFrame based on the partial one at index
			public FullFrame(int index) {
				FullFrame start = (FullFrame) frameList.get(0);
				this.biList = cloneBIList(start.getBlockList());
				int x,y,z,idx;
				int mx=min.getBlockX();
				int my=min.getBlockY();
				int mz=min.getBlockZ();
				int dx=max.getBlockX()-min.getBlockX()+1;
				int dy=max.getBlockY()-min.getBlockY()+1;
				int dz=max.getBlockZ()-min.getBlockZ()+1;
				for(int i=0; i < index; i++) {
					Frame frame = frameList.get(i);
					// location is index in biList as follows:
					// 000,001,002,...00X,010,011,012...01X...0XX...100...
					// biList [ (x*dy*dz) + (y*dz) + z; ]
					// For each block type
					HashMap<BlockInfo, HashSet<Location>> map = frame.getNext();
					if(frame.getNext() != null)
						for(BlockInfo bi : map.keySet()) {
							for(Location l : map.get(bi)) {
								x = l.getBlockX()-mx;
								y = l.getBlockY()-my;
								z = l.getBlockZ()-mz;
								idx = (x*dy*dz) + (y*dz) + z;
								if(!bi.equals(biList.get(idx))) {
									biList.set(idx, bi);
								}
							}
						}
				}
			}
			@SuppressWarnings("unchecked")
			@Override
			public JSONObject getAsJSON() {
				JSONObject json = super.getAsJSON();
				StringBuilder sb = new StringBuilder();
				sb.append(biList.get(0).toString());
				for(int i=1; i < biList.size(); i++) {
					sb.append(";"+biList.get(i).toString());
				}
				json.put("blocks", sb.toString());
				return json;
			}
		}
		
		/**
		 * BlockDelta
		 * - compares 2 3D arrays and builds a pair of maps of unique blocks with locations to change
		 * - this detemines both at once for use in populating frames
		 */
		private class BlockDelta {
			private HashMap<BlockInfo,HashSet<Location>> backdiff = new HashMap<BlockInfo,HashSet<Location>>();
			private HashMap<BlockInfo,HashSet<Location>> forwarddiff = new HashMap<BlockInfo,HashSet<Location>>();
			public HashMap<BlockInfo,HashSet<Location>> getBackward() { return backdiff; }
			public HashMap<BlockInfo,HashSet<Location>> getForward() { return forwarddiff; }
			
			public BlockDelta(FullFrame frame1, FullFrame frame2) {
				List<BlockInfo> list1 = frame1.getBlockList();
				List<BlockInfo> list2 = frame2.getBlockList();
				if(list1.size()==list2.size()) { // should always be the same
					int i = 0;
			        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
			        	for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
			        		for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
			        			BlockInfo bs1 = list1.get(i);
			        			BlockInfo bs2 = list2.get(i);
								if(!bs1.equals(bs2)) {	//look for mismatches in 3D
									if(!backdiff.containsKey(bs1)) {
										// Add new list of locations for this new type of block
										HashSet<Location> locs1 = new HashSet<Location>();
										locs1.add(new Location(dungeon.getWorld(), x, y, z));
										backdiff.put(bs1, locs1);  
									} else {
										HashSet<Location> locs1 = backdiff.get(bs1);
										locs1.add(new Location(dungeon.getWorld(), x, y, z));
									}
									if(!forwarddiff.containsKey(bs2)) {
										// Add new list of locations for this new type of block
										HashSet<Location> locs2 = new HashSet<Location>();
										locs2.add(new Location(dungeon.getWorld(), x, y, z));
										forwarddiff.put(bs2, locs2);
									} else {
										HashSet<Location> locs2 = forwarddiff.get(bs2);
										locs2.add(new Location(dungeon.getWorld(), x, y, z));
									}
								}
			        			i++;
			        }}}
				}
			}
		}
		
		
		
		/**
		 * BlockInfo
		 * - Substitute for BlockState that doesn't suck
		 * - My class for holding onto what matters. Spigot sux
		 * TODO: extend to furnaces, beds, signs, etc
		 */
		private final class BlockInfo implements Cloneable {
			private int id;
			private byte data;
			public BlockInfo(int id, byte data) {
				this.id=id;
				this.data=data;
			}
			public BlockInfo(String s) {
				String[] str = s.split(":");
				this.id=Integer.parseInt(str[0]);
				this.data=(byte) Integer.parseInt(str[1]);
			}
			@SuppressWarnings("deprecation")
			public BlockInfo(BlockState bs) {
				this.id=bs.getType().getId();
				this.data=bs.getData().getData();
			}

			public int id() { return id; }
			public byte getData() { return data; }
			public String toString() {
				return "" + id + ":" + data;
			}
			public String getDataAsString() {
				return null; //TODO when we implement signs, etc, add this
			}
			
			@Override
			protected BlockInfo clone() {
				return new BlockInfo(id,data);
			}
			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
			    if (!(o instanceof BlockInfo)) return false;
			    BlockInfo bi = (BlockInfo)o;
			    return (this.id==bi.id && this.data==bi.data);
			}
			@Override
			public int hashCode() {
				// Start with a non-zero constant. Prime is preferred
			    int result = 17;
			    result = 37*result+id;
			    result = 37*result+(int)data;
				return result;
			}
		}
	}
}
