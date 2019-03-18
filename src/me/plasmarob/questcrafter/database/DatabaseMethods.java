package me.plasmarob.questcrafter.database;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;

import me.plasmarob.questcrafter.util.Tools;

public class DatabaseMethods {
	// Singleton
	private static DatabaseMethods instance;
	private DatabaseMethods() {}
	public static DatabaseMethods getInstance() {
		if(instance == null) {
			instance = new DatabaseMethods();
			//db = new SQLite();
		}
		return instance;
	}
	
	private static Database db = new SQLite();
	
	public static boolean addWorld(UUID uuid, String name) {
		try {
			// Check if world exists, if it does, enabled it, else add it
			if(getWorldId(uuid) > 0) {
				db.updateQuery("UPDATE world SET enabled=1 WHERE uuid = '"+ uuid.toString() + "';");
			} else {
				db.updateQuery("INSERT INTO world (uuid,name,enabled) VALUES('"+ uuid.toString() + "','" + name + "',1);");
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	public static boolean disableWorld(UUID uuid) {
		try {
			db.updateQuery("UPDATE world SET enabled=0 WHERE uuid = '"+ uuid.toString() + "';");
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	// Not currently used. Code would need to blow away all dungeons in a world, with a confirm portion.
	public static boolean deleteWorld(UUID uuid) {
		try {
			db.updateQuery("DELETE FROM world WHERE uuid = '"+ uuid.toString() + "';");
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	public static int getWorldId(UUID uuid) {
		List<Map<String, Object>> results = db.readQuery("SELECT id from world WHERE uuid = '"+ uuid.toString() + "';");
		for (Map<String,Object> m : results) {
				return (Integer) m.get("id");
		}
		//db.updateQuery("DELETE FROM world WHERE uuid = '"+ uuid.toString() + "';");
		return -1;
	}
	public static int getIdByName(String table, String name) {
		List<Map<String, Object>> results = db.readQuery("SELECT id from '" + table + "' WHERE name = '"+ name + "';");
		for (Map<String,Object> m : results) {
				return (Integer) m.get("id");
		}
		//db.updateQuery("DELETE FROM world WHERE uuid = '"+ uuid.toString() + "';");
		return -1;
	}
	public static boolean containsWorld(UUID uuid) {	
		List<Map<String, Object>> results = db.readQuery("SELECT uuid FROM world where uuid = '" + uuid.toString() + "'");
		for (Map<String,Object> m : results) {
			if (m.get("uuid").equals(uuid.toString()))
				return true;
		}
		return false;		
		/*
		Connection conn = db.getSQLConnection();
        PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("SELECT uuid,name FROM world where uuid = ?");
			ps.setString(1, uuid.toString());                                                                                                                                                                                                                           					  
	        ResultSet rs = plugin.getDatabase().readQuery(conn, ps);
	        while(rs.next()){
	        	if (UUID.fromString(rs.getString("uuid")).equals(uuid))
	            	return true;
	        }
	        return false;
		} catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
		} finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }		
        return false;
        */
	}
	
	public static int insertDungeon(World world, String name, int[] corners) {	
		int id = db.insertQuery("INSERT INTO dungeon (id,world_id,name,min,max) " +
						"VALUES(NULL,'"+ 
						Integer.toString(getWorldId(world.getUID())) + "','" + 
						name + "'," +
						"'" + Tools.xyzAsString(corners[0],corners[1],corners[2]) + "'," +
						"'" + Tools.xyzAsString(corners[3],corners[4],corners[5]) + "');");
		return id;
	}
	public static List<Map<String, Object>> getDungeons() {	
		List<Map<String, Object>> results = db.readQuery("SELECT d.*,w.uuid FROM dungeon AS d JOIN world AS w ON w.id=d.world_id ");
		return results;
	}
	public static boolean deleteDungeon(int id) {
		db.updateQuery("DELETE FROM dungeon WHERE id='"+id+"'");
		return true;
	}
	
	public static List<Map<String, Object>> getBlocks(String type_name, int dungeon_id) {	
		List<Map<String, Object>> results = db.readQuery(
				  "SELECT b.* FROM qblock AS b "
				+ "JOIN qblockType AS bt ON bt.id=b.type_id "
				+ "WHERE bt.name = '" + type_name + "' and b.dungeon_id = " + dungeon_id);
		return results;
	}
	public static List<Map<String, Object>> getAnimation(int storage_id) {	
		List<Map<String, Object>> results = db.readQuery("SELECT * FROM storageFrameAnim WHERE storage_id = '" + storage_id + "' limit 1");
		return results;
	}
	
	public static boolean deleteBlock(int dungeon, int id) {	
		db.updateQuery("DELETE FROM qblock WHERE id = '"+id+"' AND dungeon_id = '"+dungeon+"'");
		return true;
	}
	
	/*
	public static List<Map<String, Object>> getStorageFrames(int block_id) {	
		List<Map<String, Object>> results = db.readQuery("SELECT sf.* FROM storageFrame AS sf WHERE sf.block_id = '" + block_id + "' ORDER BY frame_id");
		return results;
	}
	public static boolean pushStorageFrames(int block_id, int from_frame) {	
		db.updateQuery("UPDATE storageFrame SET frame_id = frame_id+1 " +
	                 "WHERE block_id = '"+block_id+"' AND frame_id >= '"+from_frame+"'");
		return true;
	}
	public static boolean pullStorageFrames(int block_id, int from_frame) {	
		db.updateQuery("UPDATE storageFrame SET frame_id = frame_id-1 " +
	                 "WHERE block_id = '"+block_id+"' AND frame_id > '"+from_frame+"'");
		return true;
	}
	public static boolean updateStorageFrame(int block_id, int frame, int ticks, String blocks, String jsondata) {	
		db.updateQuery("UPDATE storageFrame SET time = '"+ticks+"', blocks = '"+blocks+"', data = '"+jsondata+"'," +
	                 "WHERE block_id = '"+block_id+"' AND frame_id = '"+frame+"'");
		return true;
	}
	public static boolean deleteStorageFrame(int block_id, int frame_id) {	
		db.updateQuery("DELETE FROM storageFrame WHERE block_id = '"+block_id+"' AND frame_id = '"+frame_id+"'");
		return true;
	}
	public static boolean deleteAllStorageFrames(int block_id) {	
		db.updateQuery("DELETE FROM storageFrame WHERE block_id = '"+block_id+"'");
		return true;
	}
	*/
	
	public static List<Map<String, Object>> getLinks(int dungeon, int sender) {	
		List<Map<String, Object>> results = db.readQuery("SELECT * FROM link WHERE dungeon_id='"+dungeon+"' AND sender_id='"+sender+"'");
		return results;
	}
	public static void removeAllLinks(int dungeon, int sender) {
		db.updateQuery("DELETE FROM link WHERE dungeon_id='"+dungeon+"' AND sender_id='"+sender+"'");
		return;
	}
	public static void removeLink(int dungeon, int sender, int receiver) {
		db.updateQuery("DELETE FROM link WHERE dungeon_id='"+dungeon+"' AND sender_id='"+sender+"' AND receiver_id='"+receiver+"'");
		return;
	}
	
	public static List<Map<String, Object>> getQBlocksIdJoined(String joinedTable) {	
		List<Map<String, Object>> results = db.readQuery("SELECT qblock.*,"+joinedTable+".* FROM `block` JOIN '" + joinedTable + "' ON "+joinedTable+".block_id=block.id");
		return results;
	}
	
	public static int getQBlockType(String name) {	
		List<Map<String, Object>> results = db.readQuery("SELECT id FROM qblockType WHERE name = '" + name + "'");
		return (int) results.get(0).get("id");
	}
	public static boolean containsBlock(String name) {	
		List<Map<String, Object>> results = db.readQuery("SELECT name FROM world where name = '" + name + "'");
		for (Map<String,Object> m : results) {
			if (m.get("name").equals(name))
				return true;
		}
		return false;		
	}
	public static void insertBlock(World world, String name, int[] corners) {	
		/*
		"`id` INTEGER PRIMARY KEY," +
    		"`dungeon_id` INTEGER NOT NULL," +
    		"`type_id` INTEGER NOT NULL," +
            "`name` varchar(255) NOT NULL," +
            "`location` TEXT," +
            "`default` INTEGER," +
            "`inverted` INTEGER," +
            "`min` TEXT," +
            "`max` TEXT," +
            "`times` INTEGER," 
		*/
		db.updateQuery("INSERT INTO qblock (id, dungeon_id, type_id, name, min, max) " +
						"VALUES(NULL,'"+ 
						Integer.toString(getWorldId(world.getUID())) + "','" + 
						name + "'," +
						Tools.xyzAsString(corners[0],corners[1],corners[2]) + "," +
						Tools.xyzAsString(corners[3],corners[4],corners[5]) + ",);");
		return;
	}
	
	
	
	
}
