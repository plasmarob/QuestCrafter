package me.plasmarob.questcrafter;

/**
 * manager, which manages moves each tick
 * 
 * @author      Robert Thorne <plasmarob@gmail.com>
 * @version     0.0.1                
 * @since       2018-07-21
 */
public class ThreadManager5 implements Runnable {
	ThreadManager5()
	{
	}
	
	public void update() {
		for (String s : Dungeon.getDungeons().keySet())
			Dungeon.getDungeons().get(s).updatePlayersIfEnabled();
		/*
		Tune.progressAll();
		GustJar.progressAll();
		Boomerang.progressAll();
		Hookshot.progressAll();
		Bomb.progressAll();
		IceRodBlast.progressAll();
		FireRodBlast.progressAll();
		*/
	}
	
	public void run() {
	    try { update(); } catch (Exception e) { e.printStackTrace(); }
	}
}