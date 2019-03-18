package me.plasmarob.questcrafter.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import me.plasmarob.questcrafter.database.Database; // import the database class.



public class SQLite extends Database{
	
    private String dbname;
    public SQLite(){
    	super();
        dbname = plugin.getConfig().getString("SQLite.Filename", "questcrafter"); // Database filename
    }
    
    //----------------------------------------------------------------------------
    // World
    private String SQLiteCreateWorldTable = 
    		"CREATE TABLE IF NOT EXISTS world (" + 
    		"`id` INTEGER PRIMARY KEY," +
            "`uuid` varchar(255) NOT NULL," + 
            "`name` varchar(255) NOT NULL," +
            "`enabled` INTEGER DEFAULT 0," +
            "CONSTRAINT uuid_unique UNIQUE (uuid)" +
            ");";
    // Dungeon
    private String SQLiteCreateDungeonTable = 
    		"CREATE TABLE IF NOT EXISTS dungeon (" + 
    		"`id` INTEGER PRIMARY KEY," +
    		"`world_id` INTEGER NOT NULL," +
            "`name` varchar(255) NOT NULL," +
            "`min` TEXT NOT NULL," +
            "`max` TEXT NOT NULL," +
            "CONSTRAINT name_unique UNIQUE (name)" +
            ");";
            //"); CREATE UNIQUE INDEX IF NOT EXISTS world_uuid ON world(uuid);";
    //----------------------------------------------------------------------------
    // BlockType
    
    
    public String SQLiteCreateQBlockTypeTable = 
    		"CREATE TABLE IF NOT EXISTS qblockType (" + 
    		"`id` INTEGER PRIMARY KEY," +
            "`name` varchar(255) NOT NULL" +
            ");";
    public String SQLitePopulateQBlockTypeTable = 
    		"REPLACE INTO qblockType VALUES (10,'PLAYER_DETECTOR');" +
    		"REPLACE INTO qblockType VALUES (20,'STORAGE');" +
    		"REPLACE INTO qblockType VALUES (30,'TIMER');" +
    		"REPLACE INTO qblockType VALUES (40,'SPAWNER');" +
    		"REPLACE INTO qblockType VALUES (50,'TORCH');" +
    		"REPLACE INTO qblockType VALUES (60,'MUSIC');" +
    		"REPLACE INTO qblockType VALUES (70,'REDSTONE_DETECTOR');" +
    		"REPLACE INTO qblockType VALUES (80,'CHEST');" +
    		"REPLACE INTO qblockType VALUES (90,'DOOR');";   
    
    // Block
    public String SQLiteCreateQBlockTable = 
    		"CREATE TABLE IF NOT EXISTS qblock (" + 
    		"`id` INTEGER PRIMARY KEY," +
    		"`dungeon_id` INTEGER NOT NULL," +
    		"`type_id` INTEGER NOT NULL," +
            "`name` varchar(255) NOT NULL," +
    		//TODO: merge into data
            //"`location` TEXT," +
            //"`default` TEXT," +
            //"`min` TEXT," +
            //"`max` TEXT," +
            //"`times_max` INTEGER," +
            //"`times_min` INTEGER," +
            //"`blocks` TEXT," +
            
            "`data` TEXT DEFAULT NULL," +
            "FOREIGN KEY(dungeon_id) REFERENCES dungeon(id)," +
            "FOREIGN KEY(type_id) REFERENCES qblockType(id)," +
            "UNIQUE (name, dungeon_id) ON CONFLICT REPLACE" +
            ");";
    
    //----------------------------------------------------------------------------
    // Link Trigger Type
    public String SQLiteCreateTriggerTypeTable = 
    		"CREATE TABLE IF NOT EXISTS triggerType (" + 
    		"`id` INTEGER PRIMARY KEY," +
            "`name` varchar(255) NOT NULL" +
            ");";
    public String SQLitePopulateTriggerTypeTable = 
    		"REPLACE INTO triggerType VALUES (10,'TRIGGER');" +
    		"REPLACE INTO triggerType VALUES (11,'PAUSE');" +
    		"REPLACE INTO triggerType VALUES (20,'SET');" +
    		"REPLACE INTO triggerType VALUES (30,'RESET');"+   
		    "REPLACE INTO triggerType VALUES (40,'ON');" +
			"REPLACE INTO triggerType VALUES (50,'OFF');";   
	
    // Link
    public String SQLiteCreateLinkTable = 
    		"CREATE TABLE IF NOT EXISTS link (" + 
    		"`id` INTEGER PRIMARY KEY," +
    		"`sender_id` INTEGER NOT NULL," +
    		"`receiver_id` INTEGER NOT NULL," +
    		"`trigger_type_id` INTEGER NOT NULL," +
    		"`dungeon_id` INTEGER NOT NULL," +
            "FOREIGN KEY(sender_id) REFERENCES qblock(id)," +
            "FOREIGN KEY(receiver_id) REFERENCES qblock(id)," +
            "FOREIGN KEY(trigger_type_id) REFERENCES triggerType(id)" +
            "UNIQUE (sender_id, receiver_id, dungeon_id) ON CONFLICT REPLACE" +
            ");";
    
    //----------------------------------------------------------------------------
    // Other: --------------------------------------------------------------------
     
    // StorageFrame
    public String SQLiteCreateStorageFrameAnimTable = 
    		"CREATE TABLE IF NOT EXISTS storageFrameAnim (" + 
    		"`id` INTEGER PRIMARY KEY," +
    		"`storage_id` INTEGER NOT NULL," +
//            "`coreframe` TEXT," + 	// material1:data1:x,y,z;material2:data2:x,y,z;
            "`data` TEXT," +			// JSON frame data
            "FOREIGN KEY(storage_id) REFERENCES qblock(id)" +
            "UNIQUE (storage_id) ON CONFLICT REPLACE" +
            ");";  
    
    /*
    // Chest
    public String SQLiteCreateChestTable = 
    		"CREATE TABLE IF NOT EXISTS chest (" + 
    		"`id` INTEGER PRIMARY KEY," +
    		"`block_id` INTEGER NOT NULL," +
    		"`facing` varchar(255) NOT NULL," +
            "`inventory` BLOB," +
            "FOREIGN KEY(block_id) REFERENCES block(id)" +
            ");";
    
    
    //----------------------------------------------------------------------------
    // Unimplemented: ------------------------------------------------------------
    
    public String SQLiteCreateDoorTable = 
    		"CREATE TABLE IF NOT EXISTS door (" + 
    		"`id` INTEGER PRIMARY KEY," +
    		"`block_id` INTEGER NOT NULL," +
    		"`facing` varchar(255) NOT NULL," +
    		"`locked_yn` varchar(1)," +
    		"`barred` INTEGER," +
            "`blockdata` varchar(255) NOT NULL," + 	// material1,data1,mat2,dat2,m3,d3
            "FOREIGN KEY(block_id) REFERENCES block(id)" +
            ");";
    
    public String SQLiteCreatePlayerDetectorTable = 
    		"CREATE TABLE IF NOT EXISTS playerDetector (" + 
    		"`id` INTEGER PRIMARY KEY," +
    		"`block_id` INTEGER NOT NULL," +
            "`blockdata` varchar(255) NOT NULL," + 	// x1,y1,z1;x2,y2,z2;
            "FOREIGN KEY(block_id) REFERENCES block(id)" +
            ");";
    
    public String SQLiteCreateTorchTable = 
    		"CREATE TABLE IF NOT EXISTS torch (" + 
    		"`id` INTEGER PRIMARY KEY," +
    		"`block_id` INTEGER NOT NULL," +
    		"`lit_yn` varchar(1)," +
    		"`type` varchar(1)," +
    		"`timeout` INTEGER NOT NULL," +
            "`blockdata` varchar(255) NOT NULL," + 	// x1,y1,z1;x2,y2,z2;
            "FOREIGN KEY(block_id) REFERENCES block(id)" +
            ");";
    
    public String SQLiteCreateMobTable = 
    		"CREATE TABLE IF NOT EXISTS mob (" + 
    		"`id` INTEGER PRIMARY KEY," +
            "`name` varchar(255) NOT NULL," +
            "`entity_type` varchar(255) NOT NULL," +
            "`health` INTEGER NOT NULL," +
            "`inventory` BLOB," +
            "CONSTRAINT mob_name_unique UNIQUE (name)" +
            ");";
    
    public String SQLiteCreateMobSpawnTable = 
    		"CREATE TABLE IF NOT EXISTS mobSpawn (" + 
    		"`id` INTEGER PRIMARY KEY," +
    		"`mob_id` INTEGER NOT NULL," +
    		"`spawner_id` INTEGER NOT NULL," +
    		"`amount` INTEGER NOT NULL," +
            "FOREIGN KEY(mob_id) REFERENCES mob(id)," +
            "FOREIGN KEY(spawner_id) REFERENCES block(id)" +
            ");";
    
   //------------------------------------
   //For [much] later updates
    
    public String SQLiteCreatePlayerTable = 
    		"CREATE TABLE IF NOT EXISTS player (" + 
    		"`id` INTEGER PRIMARY KEY," +
            "`uuid` varchar(255) NOT NULL," + 
            "`name` varchar(255) NOT NULL," +
            "CONSTRAINT uuid_unique UNIQUE (uuid)" +
            ");";
    
    public String SQLiteCreatePlayerDungeonTable = 
    		"CREATE TABLE IF NOT EXISTS playerDungeon (" + 
    		"`id` INTEGER PRIMARY KEY," +
    		"`player_id` INTEGER NOT NULL," +
    		"`dungeon_id` INTEGER NOT NULL," +
    		"`completed_yn` varchar(1)," +
            "FOREIGN KEY(player_id) REFERENCES player(id)," +
            "FOREIGN KEY(dungeon_id) REFERENCES dungeon(id)" +
            ");";
    */
    
    // SQL creation stuff, You can leave the below stuff untouched.
    public Connection getSQLConnection() {
        File dataFolder = new File(plugin.getDataFolder(), dbname+".db");
        if (!dataFolder.exists()){
            try {
                dataFolder.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "File write error: "+dbname+".db");
                e.printStackTrace();
            }
        }
        try {
            if(connection!=null&&!connection.isClosed()){
                return connection;
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
            return connection;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE,"SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
        }
        return null;
    }

    public void load() {
        connection = getSQLConnection();
        try {
			Statement s = connection.createStatement();
            
            s.executeUpdate(SQLiteCreateWorldTable);
            s.executeUpdate(SQLiteCreateDungeonTable);
            
            s.executeUpdate(SQLiteCreateQBlockTypeTable);
            s.executeUpdate(SQLitePopulateQBlockTypeTable);
            s.executeUpdate(SQLiteCreateQBlockTable);
            
            s.executeUpdate(SQLiteCreateTriggerTypeTable);
            s.executeUpdate(SQLitePopulateTriggerTypeTable);
            s.executeUpdate(SQLiteCreateLinkTable);

            s.executeUpdate(SQLiteCreateStorageFrameAnimTable);
            /*
            s.executeUpdate(SQLiteCreateChestTable);
           
            */
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        initialize();
    }
}
