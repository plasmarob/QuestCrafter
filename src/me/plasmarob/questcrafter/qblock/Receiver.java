package me.plasmarob.questcrafter.qblock;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public interface Receiver {
	public int getID();
	public void setID(int id);
	
	//public void set(); // probably going byebye
	
	public void trigger(); // run if ON && count < max
	public void pause(); // pause if ON
	public void reset(); // count=0, set ON/OFF to default
	public void on(); // set to ON
	public void off(); // set to OFF
	// block needs MIN and MAX
	
	// Dungeon enabled, not to be confused with ON/OFF
	public boolean isEnabled();
	public void setEnabled(boolean bool);
	
	//TODO: alter this to standardize the interface. 
	// allows running via external delay
	public void run();
	
	public String type();
	public String name();
	public int getX();
	public int getY();
	public int getZ();
	
	public void show(Player p);
	public void edit(Player p, String key, String value);
	public boolean hasBlock(Block b);
	
	public void dbInsert();
}
