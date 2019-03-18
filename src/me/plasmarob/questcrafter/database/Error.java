package me.plasmarob.questcrafter.database;

import java.util.logging.Level;

import me.plasmarob.questcrafter.QuestCrafter;

public class Error {
    public static void execute(QuestCrafter plugin, Exception ex){
        plugin.getLogger().log(Level.SEVERE, "Couldn't execute MySQL statement: ", ex);
    }
    public static void close(QuestCrafter plugin, Exception ex){
        plugin.getLogger().log(Level.SEVERE, "Failed to close MySQL connection: ", ex);
    }
}
