package me.plasmarob.questcrafter.qblock;

import java.util.HashMap;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public interface Sender {
	public int getID();
	public void setID(int id);
	
	//public HashMap<Receiver, String> getTargets();
	public HashMap<Receiver, Link> getLinks();
	public void clearLinks();
	public boolean removeLink(Receiver receiver);
	
	public boolean setTarget(Receiver receiver, Link type);
	
	public String type();
	public String name();
	
	public boolean isEnabled();
	public void setEnabled(boolean bool);
	
	void run();
	
	void trigger();
	public void reset();
	public void on();
	public void off();
	
	public void show(Player p);
	public void edit(Player p, String key, String value);
	public boolean hasBlock(Block b);
	public int getX();
	public int getY();
	public int getZ();
	
	void dbInsert();
}
