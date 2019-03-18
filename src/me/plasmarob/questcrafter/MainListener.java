package me.plasmarob.questcrafter;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerMoveEvent;


public class MainListener implements Listener {

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		// If the player is in an active dungeon
		if(Dungeon.getActivePlayerDungeons().containsKey(event.getPlayer())) {
			// Tell the dungeon a player moved
			Dungeon.getActivePlayerDungeons().get(event.getPlayer()).updatePlayerMoved();
		}
	}
	
	@EventHandler
	public void onRedstoneChange(BlockRedstoneEvent event) {
		//redstone sense 	
		int prev = event.getOldCurrent();
		int next = event.getNewCurrent();
		if (prev == 0 && next > 0) {	//detect siging signal
			for (String dungeonStr : Dungeon.getDungeons().keySet()) {
				Dungeon.getActivePlayerDungeons().get(dungeonStr).testForRedstone(event.getBlock());
			}
		}
	}
		
	// shorthand for communicating with command sender
	void log(String s) {
		Bukkit.getConsoleSender().sendMessage(s);
	}
}