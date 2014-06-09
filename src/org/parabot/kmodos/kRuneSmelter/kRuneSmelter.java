package org.parabot.kmodos.kRuneSmelter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.*;

import org.parabot.environment.api.interfaces.Paintable;
import org.parabot.environment.api.utils.Time;
import org.parabot.environment.input.Keyboard;
import org.parabot.environment.input.Mouse;
import org.parabot.environment.scripts.Category;
import org.parabot.environment.scripts.Script;
import org.parabot.environment.scripts.ScriptManifest;
import org.parabot.environment.scripts.framework.SleepCondition;
import org.parabot.environment.scripts.framework.Strategy;
import org.rev317.min.Loader;
import org.rev317.min.api.methods.Calculations;
import org.rev317.min.api.methods.Menu;
import org.rev317.min.api.methods.Npcs;
import org.rev317.min.api.methods.Players;
import org.rev317.min.api.methods.SceneObjects;
import org.rev317.min.api.methods.Skill;
import org.rev317.min.api.wrappers.Npc;
import org.rev317.min.api.wrappers.SceneObject;
import org.rev317.min.api.wrappers.Tile;


@ScriptManifest(author = "Kmodos", category = Category.SMITHING, description = "Smelts Rune Bars at Fally, Premium zone, or the Crafting Guild", name = "kRuneSmelter", servers = { "PKHonor" }, version = 1)
public class kRuneSmelter extends Script implements Paintable{

	/*
	 * Vars
	 */
	private ArrayList<Strategy> strats = new ArrayList<Strategy>();
	private boolean falador = false;
	private boolean guiDone = false;
	private int barsMade = 0;
	private int lastState = 1;
	private int furnaceId = 11666;

	//Constants

	private final int STATE_TELE = 2;
	private final int STATE_BANK = 0;
	private final int STATE_SMELT = 1;

	private final long EXP_SMITHING_START = Skill.SMITHING.getExperience();
	private final int LVL_SMITHING_START = fixLevel(Skill.SMITHING.getRealLevel());

	private final int ANI_SMELT = 899;

	private final int OBJ_BANKBOOTH = 2213;
	private final int OBJ_FURNACE_PREM = 3994,
				      OBJ_FURNACE_FALLY = 11666,
			          OBJ_FURNACE_CRAFTING_GUILD = 2643;

	private final int ITEM_COAL = 453;
	private final int ITEM_RUNEORE = 451;

	private final int INTERFACE_SMELT = 2400;
	private final int INTERFACE_BANK = 23350;
	private final int INTERFACE_SKILLS = 2492;

	private final Area FURNACE_FALLY = new Area(new Tile(2970, 3375), new Tile(2980, 3375), new Tile(2980, 3367), new Tile(2970, 3367));
	private final Area FURNACE_PREMIUM = new Area(new Tile(2287, 3159), new Tile(2287, 3146), new Tile(2295, 3146), new Tile(2295, 3159));
	private final Area FURNACE_CRAFTING_GUILD = new Area(new Tile(2926, 3294), new Tile(2944, 3294), new Tile(2944, 3275), new Tile(2926, 3275));
	//End Constants

	//AntiRandom
	private final int[] RANDOMS = { 410, 1091, 3117, 3022 };
	private int rCount = 0;
	private final Area BOBS_ISLAND = new Area(new Tile(2511, 4765), new Tile(2511, 4790), new Tile(2542, 4790), new Tile(2542, 4765));

	//Paint
	private final Color color1 = new Color(0, 0, 102);
	private final Color color2 = new Color(0, 0, 0);
	private final Color color3 = new Color(255, 255, 255);

	private final BasicStroke stroke1 = new BasicStroke(1);

	private final Font font1 = new Font("Arial", 1, 14);
	private final Font font2 = new Font("Arial", 0, 12);

	private org.parabot.environment.api.utils.Timer timer;

	/*
	 * End Vars
	 */

	@Override
	public boolean onExecute(){
		@SuppressWarnings("unused")
		GUI g = new GUI();

		while(!guiDone){
			Time.sleep(100);
		}
		strats.add(new Relog());
		strats.add(new Antis());
		strats.add(new Banker());
		strats.add(new Smelt());
		strats.add(new Teleport());
		provide(strats);
		return true;
	}


	/*
	 *Paint 
	 */
	public void paint(Graphics g1) {
		Graphics2D g = (Graphics2D)g1;
		g.setColor(color1);
		g.fillRoundRect(7, 250, 503, 85, 16, 16);
		g.setColor(color2);
		g.setStroke(stroke1);
		g.drawRoundRect(7, 250, 503, 85, 16, 16);
		g.setFont(font1);
		g.setColor(color3);
		g.drawString("Kmodos' Rune Smelter", 184, 265);
		g.setFont(font2);
		g.drawString("Run Time: " + timer.toString(), 17, 285);
		g.drawString("Bars Made: " + barsMade, 17, 300);
		g.drawString("Bars / Hour: " + timer.getPerHour(barsMade), 17, 315);
		g.drawString("Randoms Solved: " + rCount, 225, 285);
		g.drawString("Smithing Exp: " + getExpString(), 225, 300);
		g.drawString("Smithing Level: " + getLvlString(), 225, 315);
	}

	/*
	 * End Paint
	 */

	/*
	 *Strats 
	 */

	public class Smelt implements Strategy{

		@Override
		public boolean activate() {
			return lastState != STATE_SMELT && Players.getMyPlayer().getAnimation() != ANI_SMELT && !Players.getMyPlayer().isInCombat() && (FURNACE_FALLY.contains(Players.getMyPlayer().getLocation()) || FURNACE_PREMIUM.contains(Players.getMyPlayer().getLocation()) || FURNACE_CRAFTING_GUILD.contains(Players.getMyPlayer().getLocation()));
		}

		@Override
		public void execute() {
			lastState = STATE_SMELT;
			SceneObject furnace = SceneObjects.getClosest(furnaceId);
			if(furnace != null){
				Menu.sendAction(900, furnace.getHash(), furnace.getLocalRegionX(), furnace.getLocalRegionY());
				Time.sleep(new SleepCondition() {
					@Override
					public boolean isValid() {
						return Loader.getClient().getBackDialogId() == INTERFACE_SMELT;
					}
				}, 4000);
				Menu.sendAction(315, 655, 0, 7449);
				Time.sleep(4500,5000);
			}
		}
	}

	public class Banker implements Strategy{

		@Override
		public boolean activate() {
			return  lastState == STATE_SMELT && Players.getMyPlayer().getAnimation() != ANI_SMELT && !Players.getMyPlayer().isInCombat();
		}

		@Override
		public void execute() {
			lastState = STATE_BANK;
			if(falador){
				Menu.sendAction(315, 151437312, 508, 1195);
				Time.sleep(new SleepCondition() {

					@Override
					public boolean isValid() {
						return Calculations.distanceBetween(new Tile(3212, 3443), Players.getMyPlayer().getLocation()) < 10;
					}
				}, 1500);
			}
			Time.sleep(2500,3000);
			SceneObject bankBooth = SceneObjects.getClosest(OBJ_BANKBOOTH);
			if(bankBooth != null){
				Menu.sendAction(502, bankBooth.getHash(), bankBooth.getLocalRegionX(), bankBooth.getLocalRegionY());
				Time.sleep(new SleepCondition() {
					@Override
					public boolean isValid() {
						return Loader.getClient().getOpenInterfaceId() == INTERFACE_BANK;
					}
				}, 5000);
				barsMade = (int) ((Skill.SMITHING.getExperience() - EXP_SMITHING_START)/10000);
				depositAll();
				Menu.sendAction(78, ITEM_RUNEORE, 1, 5382);
				Time.sleep(100);
				Menu.sendAction(867, ITEM_COAL, 0, 5382);
				Time.sleep(100);
				Menu.sendAction(867, ITEM_COAL, 0, 5382);
				Time.sleep(750,1000);
			}
		}
	}

	public class Teleport implements Strategy{

		@Override
		public boolean activate() {
			return  lastState != STATE_TELE && falador && !FURNACE_FALLY.contains(Players.getMyPlayer().getLocation());
		}

		@Override
		public void execute() {
			lastState = STATE_TELE;
			//Skills Tele
			Menu.sendAction(315, 20938752, 361, 1170);
			Time.sleep(new SleepCondition() {

				@Override
				public boolean isValid() {
					return Loader.getClient().getBackDialogId() == INTERFACE_SKILLS;
				}
			}, 1000);
			//Smithing
			Menu.sendAction(315, 1761, 351, 2497);
			Time.sleep(new SleepCondition() {
				@Override
				public boolean isValid() {
					return FURNACE_FALLY.contains(Players.getMyPlayer().getLocation());
				}
			}, 5000);
			Time.sleep(1500,2000);
		}

	}

	/**
	 * Sexy anti random by Minimal 
	 * http://www.parabot.org/community/user/10775-minimal/
	 */
	public class Antis implements Strategy{
		@Override
		public boolean activate(){
			for (Npc n : Npcs.getNearest(RANDOMS)){
				if (n.getLocation().distanceTo() < 3)
					return true;
			}
			return false;
		}

		@Override
		public void execute(){
			sleep(750);
			Npc[] n = Npcs.getNearest(RANDOMS);
			System.out.println("There is a random nearby!");
			sleep(750);
			if (n[0].getDef().getId() == 1091){
				SceneObject[] portal = SceneObjects.getNearest(8987);

				for (int i = 0; i < portal.length; i++){
					if (BOBS_ISLAND.contains(Players.getMyPlayer().getLocation())){
						final SceneObject portal2 = portal[i];
						portal2.interact(0);
						Time.sleep(new SleepCondition(){
							@Override
							public boolean isValid(){
								return portal2.getLocation().distanceTo() < 2;
							}
						}, 7500);
						portal2.interact(0);
						sleep(1000);
					}
					else
						break;
				}
				System.out.println("Bob's Island has been completed");
			}
			else if (n[0].getDef().getId() == 3022){
				System.exit(0);
				System.out.println("A mod called a Genie random onto you.\n" +
						"The client was closed to protect your account.");
			}
			else{
				n[0].interact(0);
				sleep(1500);
				System.out.println("Sandwich lady/Old man random has been completed");
			}
			rCount++;
			lastState = STATE_SMELT;
		}
	}

	/**
	 * Relog Handler by Minimal & Made better by Kmodos
	 * http://www.parabot.org/community/user/10775-minimal/
	 */
	public class Relog implements Strategy{
		public boolean activate(){
			for (@SuppressWarnings("unused") SceneObject so: SceneObjects.getNearest()){
				return false;
			}

			return true;
		}

		public void execute(){ 
			System.out.println("Relogging");
			if (!isLoggedIn()){
				Keyboard.getInstance().sendKeys("");
				sleep(6000);
			}
			if (!isLoggedIn()){
				Keyboard.getInstance().sendKeys("");
				sleep(6000);
			}
			lastState = STATE_SMELT;
		}
	}

	//Helper Methods and Classes

	public class GUI extends JFrame implements ActionListener{

		private static final long serialVersionUID = 3124781341234L;
		private JButton start;
		private Container cont;
		private Container locationCont;
		private JComboBox<String> locationSelector;
		private JLabel locationLabel;
		
		private String[] locations = {"Falador", "Premium Zone", "Crafting Guild"};
		
		private final String TITLE = "kRuneSmelter";
		private final int WIDTH = 200;
		private final int HIEGHT = 75;

		public GUI(){
			cont = new Container();
			locationCont = new Container();

			setDefaultCloseOperation(EXIT_ON_CLOSE);
			setSize(WIDTH, HIEGHT);
			setTitle(TITLE);
			setResizable(false);

			locationSelector = new JComboBox<>(locations);
			locationLabel = new JLabel(" Location: ");
			
			start = new JButton("Start");
			start.addActionListener(this);

			locationCont.setLayout(new BoxLayout(locationCont, BoxLayout.X_AXIS));
			cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));

			locationCont.add(locationLabel);
			locationCont.add(locationSelector);
			
			cont.add(locationCont);
			cont.add(start);

			add(cont);

			setVisible(true);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource().equals(start)){
				switch(locationSelector.getSelectedIndex()){
				case 0:
					falador = true;
					furnaceId = OBJ_FURNACE_FALLY;
					break;
				case 1:
					furnaceId = OBJ_FURNACE_PREM;
					break;
				case 2:
					furnaceId = OBJ_FURNACE_CRAFTING_GUILD;
					break;
				}
				setVisible(false);
				dispose();
				timer = new org.parabot.environment.api.utils.Timer();
				guiDone = true;
			}
		}
	}

	/**
	 * By Minimal
	 */
	public boolean isLoggedIn(){
		SceneObject[] so = SceneObjects.getNearest();
		if (so.length > 0)
			return true;
		else
			return false;
	}

	/**
	 *
	 * @author Matt
	 *
	 */
	public class Area {
		private Polygon p;

		/**
		 * Initializes a PolygonArea with the tiles given
		 *
		 * @param tiles
		 *            tiles to use in the area
		 */
		public Area(Tile... tiles) {
			this.p = new Polygon();
			for (int i = 0; i < tiles.length; i++) {
				p.addPoint(tiles[i].getX(), tiles[i].getY());
			}
		}

		/**
		 * Checks if a tile is in the area
		 *
		 * @param tile
		 *            The tile to check
		 * @return <b>true</b> if area does contain the tile, otherwise <b>false</b>
		 */
		public boolean contains(Tile tile) {
			return this.contains(tile.getX(), tile.getY());
		}

		public boolean contains(int x, int y) {
			int i;
			int j;
			boolean result = false;
			for (i = 0, j = p.npoints - 1; i < p.npoints; j = i++) {
				if ((p.ypoints[i] > y - 1) != (p.ypoints[j] > y - 1)
						&& (x <= (p.xpoints[j] - p.xpoints[i]) * (y - p.ypoints[i])
						/ (p.ypoints[j] - p.ypoints[i]) + p.xpoints[i])) {
					result = !result;
				}
			}
			return result;
		}
	}


	private String getExpString(){
		return Skill.SMITHING.getExperience() + " (+" + (Skill.SMITHING.getExperience() - EXP_SMITHING_START) + ")";
	}

	private String getLvlString(){
		return Skill.SMITHING.getRealLevel() + " (+" + (fixLevel(Skill.SMITHING.getRealLevel()) - LVL_SMITHING_START) + ")";
	}

	private void depositAll(){
		Mouse.getInstance().click(new Point(397,300));
		Time.sleep(750,1250);
	}

	private int fixLevel(int realLevel){
		if(realLevel > 99){
			realLevel = 99;
		}
		return realLevel;
	}
}