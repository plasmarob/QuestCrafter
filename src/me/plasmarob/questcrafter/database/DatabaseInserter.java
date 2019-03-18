package me.plasmarob.questcrafter.database;

import java.util.StringJoiner;

import me.plasmarob.questcrafter.QuestCrafter;

public class DatabaseInserter {
	
	StringBuilder query = new StringBuilder("REPLACE INTO ");
	StringJoiner columns = new StringJoiner(",");
	StringJoiner values = new StringJoiner(",");
	
	public DatabaseInserter(String table) {
		query.append(table);
	}
	
	public DatabaseInserter id() {
		columns.add("id");
		values.add("NULL");
		return this;
	}
	
	public DatabaseInserter dungeon_id(int id) {
		columns.add("dungeon_id");
		values.add(Integer.toString(id));
		return this;
	}
	
	public DatabaseInserter type_id(int type_id) {
		columns.add("type_id");
		values.add(Integer.toString(type_id));
		return this;
	}
	public DatabaseInserter type_id(String type_name) {
		columns.add("type_id");
		values.add(Integer.toString(DatabaseMethods.getQBlockType(type_name)));
		return this;
	}
	
	public DatabaseInserter name(String n) {
		columns.add("name");
		values.add("'" + n + "'");
		return this;
	}
	
	/*
	public DatabaseInserter location(int x, int y, int z) {
		columns.add("location");
		values.add("'" + Tools.locationAsString(x, y, z) + "'");
		return this;
	}
	*/
	
	public DatabaseInserter add(String s, Object o) {
		columns.add("`"+s+"`");
		if (o instanceof String)
			values.add("'" + (String)o + "'");
		else if (o instanceof Integer)
			values.add(Integer.toString((Integer)o));
		else if (o instanceof Boolean)
			values.add("'" + Boolean.toString((Boolean)o) + "'");
		else
			throw new IllegalArgumentException();
		return this;
	}
	
	public DatabaseInserter addBlobName(String s) {
		columns.add("`"+s+"`");
		values.add("?");
		return this;
	}

	public int execute() {	
		query.append("(" + columns + ") VALUES (" + values + ");");
		//Tools.say(query.toString())
		int id = QuestCrafter.getDatabase().insertQuery(query.toString());
		return id;	
	}
	
	public int executeBlob(byte[] blob) {	
		
		query.append("(" + columns + ") VALUES (" + values + ");");
		//Tools.say(query.toString())
		QuestCrafter.getDatabase().insertBLOBQuery(query.toString(),blob);
		return 0;	//TODO : return last rownum id
	}
}
