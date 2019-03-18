package me.plasmarob.questcrafter.util;

import java.util.ArrayList;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.util.ParticleEffect;

public class LineConnectEffect extends Effect{
		
		double counter;
		float yaw;
		float yawStart;
		float yawEnd;
		float tempYaw;
		double speed;
		static final ParticleEffect reddust = ParticleEffect.REDSTONE;
	
		//Random rand;
		
		Location currentLoc; 
		Location location;
		static Vector blank = new Vector();
		int x = 0;

		Color col1;
		Color col2;
		Location loc;
		Location loc1;
		Location loc2;
		double distance;
		double xDist;
		double yDist;
		double zDist;
		double times;
		double scale = 3;
		int af=0;
		
		int red1 = 0;
		int red2 = 0;
		int green1 = 0;
		int green2 = 0;
		int blue1 = 0;
		int blue2 = 0;

		ArrayList<Integer> red;
		ArrayList<Integer> green;
		ArrayList<Integer> blue;
		
		// Allow 1 or 2 color args
		public LineConnectEffect(EffectManager effectManager, Location loc1, Location loc2, Color col1) {
			super(effectManager);
			lineEffect(loc1, loc2, col1, col1);
		}
		public LineConnectEffect(EffectManager effectManager, Location loc1, Location loc2, Color col1, Color col2) {
			super(effectManager);
			lineEffect(loc1, loc2, col1, col2);
		}
		
		private void lineEffect(Location loc1, Location loc2, Color col1, Color col2) {
			
			this.loc1 = loc1;
			this.loc2 = loc2;
			this.col1 = col1;
			this.col2 = col2;
			
			location = loc1;
			
			type = EffectType.REPEATING;
			iterations = 100;
			period = 3;
			counter = 2;
			speed = 2;
			
			setLocation(location);
			
			distance = loc1.distance(loc2);
			times = distance*scale;
			xDist = (loc2.getX() - loc1.getX()) / times;
			yDist = (loc2.getY() - loc1.getY()) / times;
			zDist = (loc2.getZ() - loc1.getZ()) / times;

			red1 = col1.getRed();
			red2 = col2.getRed();
			green1 = col1.getGreen();
			green2 = col2.getGreen();
			blue1 = col1.getBlue();
			blue2 = col2.getBlue();

			red = new ArrayList<Integer>();
			green = new ArrayList<Integer>();
			blue = new ArrayList<Integer>();
			for (int i = 0; i < times; i++) {
				red.add( (int) (red1*(1-i/times) + red2*(i/times)) );
				green.add( (int) (green1*(1-i/times) + green2*(i/times)) );
				blue.add( (int) (blue1*(1-i/times) + blue2*(i/times)) );
			}	
			loc = location.clone().add(0.5,0.5,0.5);	
		}
		
        double[] ao = {0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9}; // animation offsets
		@Override
		public void onRun() {	
			af = (af + 1) % 10;  // animation frame loop 
			loc = location.clone().add(0.5+xDist*ao[af],0.5+yDist*ao[af],0.5+zDist*ao[af]);
			for (int i = 0; i < times; i++) {
				reddust.display(null, loc, Color.fromRGB(red.get(i), green.get(i), blue.get(i)), 32, 0f, 0f, 0f, 0, 1);
				loc.add(xDist, yDist, zDist);
			}
		}
	}
