package me.plasmarob.questcrafter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class MainTabCompleter implements TabCompleter {

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String commandLabel, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;
			List<String> autoCompleteList = new ArrayList<String>();
			//alias for /lc add|edit
			if (commandLabel.equals("qca") || commandLabel.equals("qce")) {
				String[] argsOld = args.clone();
				args = new String[argsOld.length+1];
				args[0] = (commandLabel.equals("qca")) ? "add" : "edit";
				for (int i=0; i < argsOld.length; i++) {
					args[i+1] = argsOld[i];
				}
			}
			
			// core commands
			if (args.length==1) {
				final String[] COMMANDS = { "addworld", "removeworld", "removeframe", "showframe", "replaceframe", "add","edit","info","create","delete","deleteDungeon", "disable", 
						                  "enable", "disable",  "expand", "insertMob", "link", "list", "listMobs", "mobinfo", 
						                  "playsound", "save", "saveMob", "select", "setWorld", "show", "spawn", "unlink" };
				autoCompleteList = new ArrayList<String>(COMMANDS.length);
				StringUtil.copyPartialMatches(args[0], Arrays.asList(COMMANDS), autoCompleteList);
			}
			// block add/edit
			if (args[0].equals("add") || args[0].equals("edit")) {
				final String[] BLOCKS = { "Chest","Detector","Door", "Frame", "Music","RSDetector","Spawner","Storage", "Timer", "Torch", "TorchBlock" };
				autoCompleteList = new ArrayList<String>(BLOCKS.length);
				if (args.length > 1) {
					StringUtil.copyPartialMatches(args[1], Arrays.asList(BLOCKS), autoCompleteList);
				} 
			}
			// dungeon names
			if (args[0].equals("select")) {
				final Set<String> DUNGEONS = Dungeon.getDungeons().keySet();
				autoCompleteList = new ArrayList<String>(DUNGEONS.size());
				
				if (args.length > 1) {
					StringUtil.copyPartialMatches(args[1], DUNGEONS, autoCompleteList);
				} 
			}
			// show all blocks
			if (args[0].equals("show") && Dungeon.getSelectedDungeons().get(player) != null) {
				Dungeon d = Dungeon.getDungeons().get(Dungeon.getSelectedDungeons().get(player));
				Set<String> BLOCKS = new HashSet<String>();
				BLOCKS.addAll(d.getDetectors().keySet());
				BLOCKS.addAll(d.getStorages().keySet());
				//BLOCKS.addAll(d.getChestBlocks().keySet());
				autoCompleteList = new ArrayList<String>(BLOCKS.size());
				
				if (args.length > 1) {
					StringUtil.copyPartialMatches(args[1], BLOCKS, autoCompleteList);
				} 
			}
			if (args[0].equals("link") && Dungeon.getSelectedDungeons().get(player) != null) {
				Dungeon d = Dungeon.getDungeons().get(Dungeon.getSelectedDungeons().get(player));
				Set<String> BLOCKS = new HashSet<String>();
				if (args.length < 3) { // SENDERS
					BLOCKS.addAll(d.getDetectors().keySet());
					//BLOCKS.addAll(d.getRSDetectors().keySet());
					autoCompleteList = new ArrayList<String>(BLOCKS.size());
					if (args.length > 1) { //2
						StringUtil.copyPartialMatches(args[1], BLOCKS, autoCompleteList);
					} else {
						autoCompleteList.addAll(BLOCKS);
					}
				} else if (args.length==3) { // RECEIVERS
					BLOCKS.addAll(d.getDetectors().keySet());
					BLOCKS.addAll(d.getStorages().keySet());
					//BLOCKS.addAll(d.getChestBlocks().keySet());
					autoCompleteList = new ArrayList<String>(BLOCKS.size());
					StringUtil.copyPartialMatches(args[2], BLOCKS, autoCompleteList);
				}
			}
			// block names
			if (args.length==3 && args[1].equals("frame")) {
				String dungeon = Dungeon.getSelectedDungeons().get(player);
				final Set<String> STORAGES = Dungeon.getDungeons().get(dungeon).getStorages().keySet();
				autoCompleteList = new ArrayList<String>(STORAGES.size());
				StringUtil.copyPartialMatches(args[2], STORAGES, autoCompleteList);
			}
			/*
			 * TODO:
			 * - mob name
			 * - world
			 */
			
			Collections.sort(autoCompleteList);
			return autoCompleteList;
		}
		return null;
	}

}
