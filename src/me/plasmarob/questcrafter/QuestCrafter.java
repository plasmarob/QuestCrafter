package me.plasmarob.questcrafter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import de.slikey.effectlib.EffectLib;
import de.slikey.effectlib.EffectManager;
import me.plasmarob.questcrafter.database.Database;
import me.plasmarob.questcrafter.database.DatabaseMethods;
//import me.plasmarob.questcrafter.database.DatabaseMethods;
import me.plasmarob.questcrafter.database.SQLite;

public class QuestCrafter extends JavaPlugin {
	
	// YAML file config objects
	File mainConfigFile;
	// WorldEdit dependency
	private static WorldEditPlugin worldEditPlugin = null;
	public static WorldEditPlugin getWE() { return worldEditPlugin; }
	private static Database db;
	public static Database getDatabase() { return db; }
	// Effects Library
    private static EffectManager effectManager; 
    public static EffectManager getEffectManager() { return effectManager; }
    
    public static ThreadManager manager;
    public static ThreadManager5 manager5;
    public final MainListener listener = new MainListener(); // Player Listener
	
	@Override
	public void onEnable()
	{
		// Create files & DB
		firstRun();
		
		//-------------------
  		// Bring in WorldEdit
  		worldEditPlugin = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
  	    if(worldEditPlugin == null){
  	        getLogger().severe("Error starting QuestCrafter! WorldEdit isn't found."); 
  	        Bukkit.getConsoleSender().sendMessage("Do not attempt to use LegendCraft in this state.");  
  	        Bukkit.getPluginManager().disablePlugin(this);
  	    }
	    
  	    manager = new ThreadManager(); // Create the thread that updates every tick
		getServer().getScheduler().scheduleSyncRepeatingTask(this, manager, 0, 1);
		manager5 = new ThreadManager5(); // Create the thread that updates every 5 sec
		getServer().getScheduler().scheduleSyncRepeatingTask(this, manager5, 0, 100);
		// Register the main listener
		Bukkit.getServer().getPluginManager().registerEvents(listener, this);
		
		// Create EffectManager for special effects library
		effectManager = new EffectManager(EffectLib.instance());
		
		List<String> commands = Arrays.asList("heal","qc","qca","qce","qci","qcn","qcsf","questcrafter");
	    for (String cmd : commands) {
	    	this.getCommand(cmd).setExecutor(new MainCommandExecutor());
	    	this.getCommand(cmd).setTabCompleter(new MainTabCompleter());
	    }
	    
		//---------------
		// Database Setup
	    DatabaseMethods.getInstance();	// init method singleton
		db = new SQLite();
		db.load();
		
		Dungeon.loadDungeons(); // Load Dungeons, its blocks, and their data
		
		// Success
		getLogger().info("Loaded!");
	}

	/**
	 * firstRun()
	 * Create files if they don't exist
	 */
	private void firstRun() {
		
		if(!getDataFolder().exists())
			getDataFolder().mkdir();	
		mainConfigFile = new File(getDataFolder(), "config.yml");
	    if(!mainConfigFile.exists())
	    	copyYamlsToFile(getResource("config.yml"), mainConfigFile);
	    
	    
		
	    /*
	    File dungeonFolder = new File(getDataFolder(), "dungeons");
	    if(!dungeonFolder.exists()) {
	    	try { 
		    	dungeonFolder.mkdir(); 
		    	getLogger().info("dungeons folder created.");
		    } catch(Exception e) { e.printStackTrace(); }
	    }
	    Dungeon.setDungeonFolder(dungeonFolder);
	    */
	    
	    /*
	    File mobsFolder = new File(getDataFolder(), "mobs");
	    if(!mobsFolder.exists()) {
	    	try { 
	    		mobsFolder.mkdir(); 		    	
		    	getLogger().info("mobs folder created.");
		    } catch(Exception e) { e.printStackTrace(); }
	    }
	    MobTemplate.setMobFolder(mobsFolder);
	    */
	}
	
	// Copy to file
	public static void copyYamlsToFile(InputStream in, File file) {
	    try {
	        OutputStream out = new FileOutputStream(file);
	        if (in != null) {
	        	int len;
	        	byte[] buf = new byte[1024];
		        while((len=in.read(buf))>0){
		            out.write(buf,0,len);
		        }
		        in.close();
	        }
	        out.close();
	    } catch (Exception e) {e.printStackTrace();}
	}
}