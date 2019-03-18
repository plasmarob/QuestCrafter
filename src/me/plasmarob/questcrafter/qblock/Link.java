package me.plasmarob.questcrafter.qblock;

public enum Link {
	TRIGGER ("TRIGGER", 10), // PLAY
//	PAUSE ("PAUSE", 11), 
//	SET ("SET", 20), 
	RESET ("RESET", 30), // STOP
	ON ("ON", 40), 
	OFF ("OFF", 50);
	
	public final String NAME;
	public final int ID;
	
	Link(String name, int id) {
		this.NAME = name;
		this.ID = id;
	}
	
	public static boolean valid(String str) {
		for (Link l : Link.values()) 
			if (str.toUpperCase().equals(l.NAME)) return true;
		return false;
	}
	
	public static Link get(String str) {
		for (Link l : Link.values()) 
			if (str.toUpperCase().equals(l.NAME)) return l;
		return null;
	}
	
	public static Link get(int i) {
		for (Link l : Link.values()) 
			if (l.ID == i) return l;
		return null;
	}
	
	public void call(Receiver r) {
		if (NAME.equals("TRIGGER"))
			r.trigger();
//		else if (NAME.equals("PAUSE"))
//			r.pause();
//		else if (NAME.equals("RESET"))
//			r.reset();
		else if (NAME.equals("ON"))
			r.on();
		else if (NAME.equals("OFF"))
			r.off();
	}
	
}
