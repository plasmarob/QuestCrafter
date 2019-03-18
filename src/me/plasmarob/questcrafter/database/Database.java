package me.plasmarob.questcrafter.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import me.plasmarob.questcrafter.database.Error; // YOU MUST IMPORT THE CLASS ERROR, AND ERRORS!!!
import me.plasmarob.questcrafter.database.Errors;
import me.plasmarob.questcrafter.QuestCrafter;

public abstract class Database {
	QuestCrafter plugin;
    Connection connection;
    // The name of the table we created back in SQLite class.
    public String table = "world";
    public int tokens = 0;
    public Database(){
        plugin = JavaPlugin.getPlugin(QuestCrafter.class);
    }

    public abstract Connection getSQLConnection();

    public abstract void load();

    // attempts to get results
    public void initialize(){
        connection = getSQLConnection();
        try{
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + table + " WHERE uuid = ''");
            ResultSet rs = ps.executeQuery();
            close(ps,rs);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to retreive connection", ex);
        }
    }
    
    /**
	 * Queries the Database, for queries which return results.
	 *
	 * @param query Query to run
	 * @return Result set of ran query
	 */
	public List<Map<String, Object>> readQuery(String query) {
		Connection conn = null;
    	PreparedStatement ps = null; 	
		try {
            conn = getSQLConnection();
            ps = conn.prepareStatement(query); 
            ResultSet rs = ps.executeQuery();
			return resultSetAsList(rs);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return null;     
	}
	
	/**
	 * Queries the Databases, for queries which modify data.
	 *
	 * @param query Query to run
	 */
	public boolean updateQuery(String query) {
		Connection conn = null;
    	PreparedStatement ps = null;
    	
    	try {
    		conn = getSQLConnection();
            ps = conn.prepareStatement(query); 		  
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
                return false;
            }
        }
        return true;    
	}
    
	/**
	 * Queries the Databases, for queries which modify data.
	 *
	 * @param query Query to run
	 */
	public int insertQuery(String query) {
		Connection conn = null;
    	PreparedStatement ps = null;
    	
    	try {
    		conn = getSQLConnection();
            ps = conn.prepareStatement(query); 		  
            ps.executeUpdate();
            ps.close();
            
            ps = conn.prepareStatement("select last_insert_rowid() as id;"); 
            ResultSet rs = ps.executeQuery();
            return Integer.parseInt(rs.getString("id"));
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
                return 0;
            }
        }
    	return 0; 
	}
	
	/**
	 * Queries the Databases, for queries which modify data.
	 *
	 * @param query Query to run
	 */
	public int insertBLOBQuery(String query, byte[] blob) {
		Connection conn = null;
    	PreparedStatement ps = null;
    	
    	try {
    		conn = getSQLConnection();
            ps = conn.prepareStatement(query); 		
            ps.setBytes(1, blob);
            ps.executeUpdate();
            ps.close();
            
            ps = conn.prepareStatement("select last_insert_rowid() as id;"); 
            ResultSet rs = ps.executeQuery();
            return Integer.parseInt(rs.getString("id"));
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
                return 0;
            }
        }
    	return 0; 
	}
	
    public void close(PreparedStatement ps,ResultSet rs){
        try {
            if (ps != null)
                ps.close();
            if (rs != null)
                rs.close();
        } catch (SQLException ex) {
            Error.close(plugin, ex);
        }
    }
    
    public List<Map<String, Object>> resultSetAsList(ResultSet rs) throws SQLException{
    	ResultSetMetaData md = rs.getMetaData();
    	int columns = md.getColumnCount();
    	List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    	while (rs.next()){
    		Map<String, Object> row = new HashMap<String, Object>(columns);
    		for(int i=1; i<=columns; ++i){           
    			row.put(md.getColumnName(i),rs.getObject(i));
    		}
    		list.add(row);
    	}
		return list;
    }
}
