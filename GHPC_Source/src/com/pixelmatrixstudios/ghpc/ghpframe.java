/* 
 * Copyright (C) 2011-12 piluke <pikingqwerty@gmail.com>
 * 
 * This file is part of GHPC.
 * GHPC is free software and comes with ABSOLUTELY NO WARRANTY.
 * See LICENSE for details.
 */

package com.pixelmatrixstudios.ghpc;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;

import org.lateralgm.file.GmFile;
import org.lateralgm.main.Util;
import static org.lateralgm.main.Util.deRef;
import org.lateralgm.resources.Background;
import org.lateralgm.resources.Font.PFont;
import org.lateralgm.resources.Font;
import org.lateralgm.resources.GameSettings;
import org.lateralgm.resources.GameSettings.PGameSettings;
import org.lateralgm.resources.GmObject;
import org.lateralgm.resources.GmObject.PGmObject;
import org.lateralgm.resources.Path;
import org.lateralgm.resources.ResourceReference;
import org.lateralgm.resources.Room;
import org.lateralgm.resources.Room.PRoom;
import org.lateralgm.resources.Script;
import org.lateralgm.resources.Sound;
import org.lateralgm.resources.Sprite;
import org.lateralgm.resources.Sprite.MaskShape;
import org.lateralgm.resources.Sprite.PSprite;
import org.lateralgm.resources.Timeline;
import org.lateralgm.resources.sub.Argument;
import org.lateralgm.resources.sub.BackgroundDef;
import org.lateralgm.resources.sub.BackgroundDef.PBackgroundDef;
import org.lateralgm.resources.sub.Instance;
import org.lateralgm.resources.sub.Instance.PInstance;
import org.lateralgm.resources.sub.MainEvent;
import org.lateralgm.resources.sub.Tile;
import org.lateralgm.resources.sub.Tile.PTile;

import com.pixelmatrixstudios.ghpc.FileChooser;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;

public class ghpframe extends JFrame implements ActionListener {
	private static final long serialVersionUID = 7477350675923471946L;
	JPanel pane = new JPanel();
	JFileChooser jfc;
	JButton ob, bb, cb;
	JTextField ff;
	JProgressBar pb;
	Rectangle wr, pr;
	FileChooser lfc;
	JComboBox<String> el;
	JCheckBox zc, dc;
	Container con;
	String lf;
	String[] gml, gcl, gmc, gcc;
	String[] resourceNames, objNames;
	GmFile gmfile;
	byte curMain;
	String curEvt;
	GmObject child = null;
	
	//GML Helper scripts
	String gmhescr = "initGHP"+"addLink"+"drawText"+"drawGradientRect"+"drawGradientCircle"+"drawSetBackground";
	ghpframe()	{
		super("GHP Converter");
		wr = new Rectangle(100, 100, 315, 152);
		setBounds(wr);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((int) (dim.getWidth() - getWidth()) / 2, (int) (dim.getHeight() - getHeight()) / 2);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		con = this.getContentPane();
		pane.setLayout(null);
		jfc = new JFileChooser();
		jfc.setCurrentDirectory(new File(System.getProperty("user.home")+"/Desktop"));
		lfc = new FileChooser();
		pb = new JProgressBar(0, 100);
		proBar(0, "Select a file.");
		pb.setStringPainted(true);
		pb.setBounds(0, 0, 300, 50);
		pane.add(pb);
		pr = pb.getBounds();
		pr.x = 0;
		pr.y = 0;
		pb.paintImmediately(pr);
		ff = new JTextField(128);
		ff.addActionListener(this);
		ff.setBounds(0, 50, 300, 25);
		final Properties prop = new Properties();
		try {
				prop.load(new FileInputStream("ghpc.properties"));
		} catch (IOException err) {
				//
		}
		lf = prop.getProperty("last_file");
		if ((lf != null)&&(lf != "")) {
				ff.setText(lf);
		}
		pane.add(ff);
		ob = new JButton("Options");
		ob.addActionListener(this);
		ob.setBounds(0, 75, 100, 40);
		pane.add(ob);
		bb = new JButton("Browse");
		bb.addActionListener(this);
		bb.setBounds(100, 75, 100, 40);
		pane.add(bb);
		cb = new JButton("Convert");
		cb.addActionListener(this);
		cb.setBounds(200, 75, 100, 40);
		pane.add(cb);
		String[] export = {"HTML5"};
		el = new JComboBox<String>(export);
		if (prop.getProperty("default_export") == null) {
				el.setSelectedIndex(0);
		}
		else {
				el.setSelectedItem(prop.getProperty("default_export"));
		}
		el.setBounds(0, 120, 100, 40);
		pane.add(el);
		zc = new JCheckBox("Zip?");
		zc.setSelected(Boolean.valueOf(prop.getProperty("zip")));
		zc.setBounds(120, 120, 80, 40);
		pane.add(zc);
		dc = new JCheckBox("Debug?");
		dc.setSelected(Boolean.valueOf(prop.getProperty("debug")));
		dc.setBounds(210, 120, 100, 40);
		pane.add(dc);
		con.add(pane);
		Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
						if (el.getSelectedItem() == null) {
								el.setSelectedIndex(0);
						}
						prop.setProperty("default_export", el.getSelectedItem().toString());
						prop.setProperty("zip", zc.isSelected()+"");
						if (lf == null) {
								lf = "";
						}
						prop.setProperty("last_file", lf);
						try {
								prop.store(new FileOutputStream("ghpc.properties"), null);
						} catch (IOException err) {
								System.out.println("Could not save options.");
						}
				}
		});
		setVisible(true);
		
		DataInputStream gmlf = new DataInputStream(ghpc.class.getResourceAsStream("func/gmnames"));
		DataInputStream gclf = new DataInputStream(ghpc.class.getResourceAsStream("func/ghpnames"));
		DataInputStream gmcf = new DataInputStream(ghpc.class.getResourceAsStream("func/gmcon"));
		DataInputStream gccf = new DataInputStream(ghpc.class.getResourceAsStream("func/ghpcon"));
		String gmls = "";
		String gcls = "";
		String gmcs = "";
		String gccs = "";
		byte[] buf = new byte[2048];
		try {
				while (gmlf.read(buf) != -1) {
						gmls += new String(buf);
				}
				gmlf.close();
				buf = new byte[2048];
				while (gclf.read(buf) != -1) {
						gcls += new String(buf);
				}
				gclf.close();
				buf = new byte[2048];
				while (gmcf.read(buf) != -1) {
						gmcs += new String(buf);
				}
				gmcf.close();
				buf = new byte[2048];
				while (gccf.read(buf) != -1) {
						gccs += new String(buf);
				}
				gccf.close();
		} catch (IOException e) {
				System.out.println("Can't read fnames.");
		}
		gml = gmls.split("\n");
		gcl = gcls.split("\n");
		gmc = gmcs.split("\n");
		gcc = gccs.split("\n");
	}
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == ob) {
				Rectangle bounds = getBounds();
				if (bounds.height == wr.height) {
						setBounds(bounds.x, bounds.y, wr.width, wr.height+100);
				}
				else {
						setBounds(bounds.x, bounds.y, wr.width, wr.height);
				}
		}
		else if (e.getSource() == bb) {
			proBar(0, "Select a File");
			int returnval = jfc.showOpenDialog(this);
			if (returnval == JFileChooser.APPROVE_OPTION) {
				File file = jfc.getSelectedFile();
				ff.setText(file.getPath());
			}
		}
		else if (e.getSource() == cb) {
			if (el.getSelectedItem() == "HTML5") {
					gmtohtml();
			}
		}
		else if (e.getSource() == ff) {
			File file = new File(ff.getText());
			if (file.exists() == true) {
					if (el.getSelectedItem() == "HTML5") {
							gmtohtml();
					}
			}
		}
	}
	public void proBar(int i) {
			pb.setValue(i);
			pr = pb.getBounds();
			pr.x = 0;
			pr.y = 0;
			pb.paintImmediately(pr);
	}
	public void proBar(String s) {
			pb.setString(s);
			pr = pb.getBounds();
			pr.x = 0;
			pr.y = 0;
			pb.paintImmediately(pr);
	}
	public void proBar(int i, String s) {
			pb.setValue(i);
			pb.setString(s);
			pr = pb.getBounds();
			pr.x = 0;
			pr.y = 0;
			pb.paintImmediately(pr);
	}
	public void gmtohtml() {
			//Read and convert file
			proBar("Reading...");
			File d = new File(ff.getText());
			lf = d.getPath();
			proBar(5);
			if (d.exists() == false) {
				proBar(0, "File doesn't exist.");
				return;
			}
			
			String dir = d.getName();
			proBar(7);
			gmfile = lfc.openFile(d);
			int rsnum = gmfile.sprites.size()+gmfile.sounds.size()+gmfile.backgrounds.size()+gmfile.paths.size()+gmfile.scripts.size()+gmfile.fonts.size()+gmfile.timelines.size()+gmfile.gmObjects.size()+gmfile.rooms.size();
			proBar(10);
			
			resourceNames = new String[rsnum];
			objNames = new String[gmfile.gmObjects.size()];
			int ind = 0;
			
			for (GmObject o : gmfile.gmObjects) {
				objNames[ind] = o.getName();
				resourceNames[ind++] = o.getName();
			}
			
			for (Sprite s : gmfile.sprites)
				resourceNames[ind++] = s.getName();
			
			for (Background b : gmfile.backgrounds)
				resourceNames[ind++] = b.getName();
			
			for (Sound s : gmfile.sounds)
				resourceNames[ind++] = s.getName();
			
			for (Room r : gmfile.rooms)
				resourceNames[ind++] = r.getName();
			
			for (Timeline t : gmfile.timelines)
				resourceNames[ind++] = t.getName();
			
			for (Path p : gmfile.paths)
				resourceNames[ind++] = p.getName();
			
			for (Script s : gmfile.scripts)
				resourceNames[ind++] = s.getName();
			
			for (Font f : gmfile.fonts)
				resourceNames[ind++] = f.getName();
			
			Arrays.sort(resourceNames);
			Arrays.sort(objNames);
			
			//Get settings and other variables
			String descr = gmfile.gameSettings.get(GameSettings.PGameSettings.DESCRIPTION);
			int fps = gmfile.rooms.first().get(Room.PRoom.SPEED);
			Iterator<Sprite> sprites = gmfile.sprites.iterator();
			int sprnum = gmfile.sprites.size();
			Iterator<Sound> sounds = gmfile.sounds.iterator();
			int sndnum = gmfile.sounds.size();
			Iterator<Background> backgrounds = gmfile.backgrounds.iterator();
			int bcknum = gmfile.backgrounds.size();
			Iterator<org.lateralgm.resources.Font> fonts = gmfile.fonts.iterator();
			//int fntnum = gmfile.fonts.size();
			Iterator<Script> scripts = gmfile.scripts.iterator();
			//int scrnum = gmfile.scripts.size();
			Iterator<GmObject> gmobjects = gmfile.gmObjects.iterator(); 
			int objnum = gmfile.gmObjects.size();
			Iterator<Room> rooms = gmfile.rooms.iterator();
			int roomnum = gmfile.rooms.size();
			
			//Add files
			proBar("Adding JavaScript...");
			File tmpdir = new File(d.getPath().substring(0, d.getPath().lastIndexOf(File.separator))+File.separator+dir.substring(0, dir.lastIndexOf(".")));
			File udir = new File(d.getPath().substring(0, d.getPath().lastIndexOf(File.separator)));
			tmpdir.mkdir();
			FileOutputStream li, re, main, event, func, mainjs, debug, vars;
			byte buf;
			String line;
			try {
					//LICENSE
					File lisf = new File(tmpdir+"/LICENSE");
					lisf.createNewFile();
					li = new FileOutputStream(lisf);
					InputStreamReader lif = new InputStreamReader(ghpc.class.getResourceAsStream("/LICENSE"));
					while (lif.ready()) {
							buf = (byte) lif.read();
							li.write(buf);
					}
					li.close();
					lif.close();
					//README
					File resf = new File(tmpdir+"/README");
					resf.createNewFile();
					re = new FileOutputStream(resf);
					InputStreamReader ref = new InputStreamReader(ghpc.class.getResourceAsStream("/README"));
					while (ref.ready()) {
							buf = (byte) ref.read();
							re.write(buf);
					}
					re.close();
					lif.close();
					//main.html
					File mainsf = new File(tmpdir+"/main.html");
					mainsf.createNewFile();
					main = new FileOutputStream(mainsf);
					InputStreamReader[] mainf = new InputStreamReader[4];
					mainf[0] = new InputStreamReader(ghpc.class.getResourceAsStream("src/mainf0"));
					while (mainf[0].ready()) {
							buf = (byte) mainf[0].read();
							main.write(buf);
					}
					mainf[0].close();
					for (int i=0;i<dir.substring(0, dir.lastIndexOf(".")).length();i++) {
							buf = (byte) dir.substring(0, dir.lastIndexOf(".")).toCharArray()[i];
							main.write(buf);
					}
					mainf[1] = new InputStreamReader(ghpc.class.getResourceAsStream("src/mainf1"));
					while (mainf[1].ready()) {
							buf = (byte) mainf[1].read();
							main.write(buf);
					}
					mainf[1].close();
					for (int i=0;i<descr.length();i++) {
							buf = (byte) descr.toCharArray()[i];
							main.write(buf);
					}
					mainf[2] = new InputStreamReader(ghpc.class.getResourceAsStream("src/mainf2"));
					while (mainf[2].ready()) {
							buf = (byte) mainf[2].read();
							main.write(buf);
					}
					mainf[2].close();
					String cur = "";
					if (!(boolean) gmfile.gameSettings.get(PGameSettings.DISPLAY_CURSOR))
						cur = "cursor:url(invis.png),auto;";
					line = "<canvas id=\"maincan\" width=\"" + gmfile.rooms.first().get(PRoom.WIDTH) + "\" height=\"" + gmfile.rooms.first().get(PRoom.HEIGHT) + "\" tabindex=\"1\" style=\"" + cur + "\" onkeydown=\"eventKeyboardPress(event);\" onkeyup=\"eventKeyboardRelease(event);\" onmousemove=\"eventMouseMove(event);\" onmousedown=\"eventMousePress(event);\" onmouseup=\"eventMouseRelease(event);\">";
					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							main.write(buf);
					}
					mainf[3] = new InputStreamReader(ghpc.class.getResourceAsStream("src/mainf3"));
					while (mainf[3].ready()) {
							buf = (byte) mainf[3].read();
							main.write(buf);
					}
					mainf[3].close();
					if (dc.isSelected()) {
							line = "\t\t<script type=\"text/javascript\" src=\"debug.js\"></script>\n";
							for (int e=0;e<line.length();e++) {
									buf = (byte) line.toCharArray()[e];
									main.write(buf);
							}
					}
					line = "\n\t</body>\n</html>";
					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							main.write(buf);
					}
					main.close();
					proBar(11);
					
					//Browser icons/main dir imgs
					BufferedImage[] bric = new BufferedImage[10];
					FileOutputStream brics = new FileOutputStream(tmpdir+"/chrome.png");
					bric[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/bric0.png"));
					ImageIO.write(bric[0], "png", brics);
					brics.close();
					brics = new FileOutputStream(tmpdir+"/firefox.png");
					bric[1] = ImageIO.read(ghpc.class.getResourceAsStream("src/bric1.png"));
					ImageIO.write((RenderedImage) bric[1], "png", brics);
					brics.close();
					brics = new FileOutputStream(tmpdir+"/safari.png");
					bric[2] = ImageIO.read(ghpc.class.getResourceAsStream("src/bric2.png"));
					ImageIO.write((RenderedImage) bric[2], "png", brics);
					brics.close();
					brics = new FileOutputStream(tmpdir+"/opera.png");
					bric[3] = ImageIO.read(ghpc.class.getResourceAsStream("src/bric3.png"));
					ImageIO.write((RenderedImage) bric[3], "png", brics);
					brics.close();
					brics = new FileOutputStream(tmpdir+"/invis.png");
					bric[4] = ImageIO.read(ghpc.class.getResourceAsStream("src/invis"));
					ImageIO.write((RenderedImage) bric[4], "png", brics);
					brics.close();

					
					//Particles
					BufferedImage[] part = new BufferedImage[16];
					File partf = new File(tmpdir+"/particles");
					partf.mkdir();
					FileOutputStream parts = new FileOutputStream(tmpdir+"/particles/00_pixel.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part00.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					parts = new FileOutputStream(tmpdir+"/particles/01_disk.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part01.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					parts = new FileOutputStream(tmpdir+"/particles/02_square.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part02.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					parts = new FileOutputStream(tmpdir+"/particles/03_line.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part03.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					parts = new FileOutputStream(tmpdir+"/particles/04_star.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part04.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					parts = new FileOutputStream(tmpdir+"/particles/05_circle.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part05.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					parts = new FileOutputStream(tmpdir+"/particles/06_ring.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part06.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					parts = new FileOutputStream(tmpdir+"/particles/07_sphere.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part07.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					parts = new FileOutputStream(tmpdir+"/particles/08_flare.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part08.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					parts = new FileOutputStream(tmpdir+"/particles/09_spark.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part09.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					parts = new FileOutputStream(tmpdir+"/particles/10_explosion.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part10.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					parts = new FileOutputStream(tmpdir+"/particles/11_cloud.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part11.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					parts = new FileOutputStream(tmpdir+"/particles/12_smoke.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part12.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					parts = new FileOutputStream(tmpdir+"/particles/13_snow.png");
					part[0] = ImageIO.read(ghpc.class.getResourceAsStream("src/particles/part13.png"));
					ImageIO.write(part[0], "png", parts);
					parts.close();
					proBar(12);
					
					//event.js, func.js, main.js, debug.js
					File eventsf = new File(tmpdir+"/event.js");
					eventsf.createNewFile();
					event = new FileOutputStream(eventsf);
					InputStreamReader eventf = new InputStreamReader(ghpc.class.getResourceAsStream("src/eventf"));
					while (eventf.ready()) {
							buf = (byte) eventf.read();
							event.write(buf);
					}
					event.close();
					eventf.close();
					File funcsf = new File(tmpdir+"/func.js");
					funcsf.createNewFile();
					func = new FileOutputStream(funcsf);
					InputStreamReader funcf = new InputStreamReader(ghpc.class.getResourceAsStream("src/funcf"));
					while (funcf.ready()) {
							buf = (byte) funcf.read();
							func.write(buf);
					}
					func.close();
					funcf.close();
					File mainjsf = new File(tmpdir+"/main.js");
					mainjsf.createNewFile();
					mainjs = new FileOutputStream(mainjsf);
					InputStreamReader mainjf = new InputStreamReader(ghpc.class.getResourceAsStream("src/mainjf"));
					while (mainjf.ready()) {
							buf = (byte) mainjf.read();
							mainjs.write(buf);
					}
					mainjs.close();
					mainjf.close();
					if (dc.isSelected()) {
							File debugsf = new File(tmpdir+"/debug.js");
							debugsf.createNewFile();
							debug = new FileOutputStream(debugsf);
							InputStreamReader debugf = new InputStreamReader(ghpc.class.getResourceAsStream("src/debugf"));
							while (debugf.ready()) {
									buf = (byte) debugf.read();
									debug.write(buf);
							}
							debugf.close();
					}
					proBar(13);
					
					//vars.js
					File varsf = new File(tmpdir+"/vars.js");
					varsf.createNewFile();
					vars = new FileOutputStream(varsf);
					InputStreamReader[] varf = new InputStreamReader[10];
					varf[0] = new InputStreamReader(ghpc.class.getResourceAsStream("src/varf0"));
					while (varf[0].ready()) {
							buf = (byte) varf[0].read();
							vars.write(buf);
					}
					varf[0].close();
					for (int i=0;i<Integer.toString(fps).length();i++) {
							buf = (byte) Integer.toString(fps).toCharArray()[i];
							vars.write(buf);
					}
					varf[1] = new InputStreamReader(ghpc.class.getResourceAsStream("src/varf1"));
					while (varf[1].ready()) {
							buf = (byte) varf[1].read();
							vars.write(buf);
					}
					varf[1].close();
					while (sprites.hasNext()) {
							Sprite spr = sprites.next();
							String sh = "";
							if (spr.get(PSprite.SHAPE).equals(MaskShape.PRECISE))
								sh = "PRECISE";
							if (spr.get(PSprite.SHAPE).equals(MaskShape.RECTANGLE))
								sh = "RECTANGLE";
							if (spr.get(PSprite.SHAPE).equals(MaskShape.DISK))
								sh = "ELLIPSE";
							if (spr.get(PSprite.SHAPE).equals(MaskShape.DIAMOND))
								sh = "DIAMOND";
							line = spr.getName()+" = Sprite(\"sprites/"+spr.getName()+".png\", "+spr.subImages.size()+", " + spr.get(PSprite.TRANSPARENT) + ", " + spr.get(PSprite.SMOOTH_EDGES) + ", " + spr.get(PSprite.ORIGIN_X) + ", " + spr.get(PSprite.ORIGIN_Y) + ", " + spr.get(PSprite.BB_LEFT) + ", " + spr.get(PSprite.BB_RIGHT) + ", " + spr.get(PSprite.BB_TOP) + ", " + spr.get(PSprite.BB_BOTTOM) + ", " + sh + ", " + spr.get(PSprite.ALPHA_TOLERANCE) + ");\n";
							for (int e=0;e<line.length();e++) {
									buf = (byte) line.toCharArray()[e];
									vars.write(buf);
							}
					}
					line = "\n//Sounds\n";
					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							vars.write(buf);
					}
					while (sounds.hasNext()) {
							String sndname = sounds.next().getName();
							line = sndname+" = new Audio();\n"+sndname+".src = \"sounds/"+sndname+".wav\";\n"+sndname+".load();\n";
							for (int e=0;e<line.length();e++) {
									buf = (byte) line.toCharArray()[e];
									vars.write(buf);
							}
					}
					line = "\n//Backgrounds\n";
					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							vars.write(buf);
					}
					while (backgrounds.hasNext()) {
							String bckname = backgrounds.next().getName();
							line = bckname+" = new Image();\n"+bckname+".src = \"backgrounds/"+bckname+".png\";\n";
							for (int e=0;e<line.length();e++) {
									buf = (byte) line.toCharArray()[e];
									vars.write(buf);
							}
					}
					line = "\n//Fonts\n";
					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							vars.write(buf);
					}
					while (fonts.hasNext()) {
							org.lateralgm.resources.Font fnt = fonts.next();
							line = fnt.getName()+" = fontAdd(\""+fnt.get(PFont.FONT_NAME)+"\", "+fnt.get(PFont.SIZE)+", "+fnt.get(PFont.BOLD)+", "+fnt.get(PFont.ITALIC)+");\n";
							for (int e=0;e<line.length();e++) {
									buf = (byte) line.toCharArray()[e];
									vars.write(buf);
							}
					}
					line = "\n//Scripts\n";
					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							vars.write(buf);
					}
					while (scripts.hasNext()) {
							org.lateralgm.resources.Script scr = scripts.next();
							if (gmhescr.contains(scr.getName())) {continue;}
							line = "function "+scr.getName()+"() {\n"+gmltoghp(scr.getCode(), "this", "\t")+"\n}\n";
							for (int e=0;e<line.length();e++) {
									buf = (byte) line.toCharArray()[e];
									vars.write(buf);
							}
					}
					proBar(14);
					
					///////////
					//Objects//
					///////////
					
					line = "\n//Objects\n";
					
					//Begin Step
					boolean ie = false;
					while (gmobjects.hasNext()) {
							GmObject curobj = gmobjects.next();
							GmObject qobj = curobj;
							boolean cont = false;
							while (qobj != null) {
									for (org.lateralgm.resources.sub.Event e : qobj.mainEvents.get(MainEvent.EV_STEP).events)
											if (e.toString().equals("Begin Step")) {
													cont = true;
													break;
											}
									
									if (cont)
											break;
									
									ResourceReference ref2 = qobj.get(PGmObject.PARENT);
									if (ref2 == null)
											qobj = null;
									else
											qobj = (GmObject) ref2.get();
							}
							
							if (!cont)
								continue;
							
							if (!ie)
									line += "function objBeginStep() {\n";
							
							line += "\t"+curobj.getName()+".BeginStep();\n";
							ie = true;
					}
					if (ie)
							line += "}\n\n";
					else
							line += "function objBeginStep(){}\n";
					
					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							vars.write(buf);
					}
					
					//Normal Step
					line = "";
					ie = false;
					gmobjects = gmfile.gmObjects.iterator();
					while (gmobjects.hasNext()) {
						GmObject curobj = gmobjects.next();
						
						if (!ie)
								line += "function objStep() {\n";

						line += "\t"+curobj.getName()+".Step();\n";
						ie = true;
					}
					if (ie)
							line += "}\n\n";
					else
							line += "function objStep(){}\n";
					
					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							vars.write(buf);
					}
					
					//End Step
					line = "";
					ie = false;
					gmobjects = gmfile.gmObjects.iterator();
					while (gmobjects.hasNext()) {
							GmObject curobj = gmobjects.next();
							GmObject qobj = curobj;
							boolean cont = false;
							while (qobj != null) {
									for (org.lateralgm.resources.sub.Event e : qobj.mainEvents.get(MainEvent.EV_STEP).events)
											if (e.toString().equals("Begin Step")) {
													cont = true;
													break;
											}
									
									if (cont)
											break;
									
									ResourceReference ref2 = qobj.get(PGmObject.PARENT);
									if (ref2 == null)
											qobj = null;
									else
											qobj = (GmObject) ref2.get();
							}
							
							if (!cont)
								continue;

							if (!ie)
									line += "function objEndStep() {\n";
							
							line += "\t"+curobj.getName()+".EndStep();\n";
							ie = true;
					}
					if (ie)
							line += "}\n\n";
					else
							line += "function objEndStep(){}\n";
					
					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							vars.write(buf);
					}
					
					//Mouse Events
					String[] mLocales = {"", "Global "};
					String[] mButtons = {"Left ", "Middle ", "Right "};
					String[] mActions = {"Button", "Pressed", "Released"};
					
					for (String ml : mLocales)
							for (String mb : mButtons)
									for (String ma : mActions) {
											String eName = ml + mb + ma;
											line = "";
											ie = false;
											gmobjects = gmfile.gmObjects.iterator();
											while (gmobjects.hasNext()) {
													GmObject curobj = gmobjects.next();
													GmObject qobj = curobj;
													boolean cont = false;
													while (qobj != null) {
															for (org.lateralgm.resources.sub.Event e : qobj.mainEvents.get(MainEvent.EV_MOUSE).events)
																	if (e.toString().equals(eName)) {
																			cont = true;
																			break;
																	}
															
															if (cont)
																	break;
															
															ResourceReference ref2 = qobj.get(PGmObject.PARENT);
															if (ref2 == null)
																	qobj = null;
															else
																	qobj = (GmObject) ref2.get();
													}
													
													if (!cont)
														continue;
													if (!ie)
															line += "function obj" + eName.replace(" ", "") + "() {\n";
													
													line += "\t"+curobj.getName()+"."+eName.replace(" ", "")+"();\n";
													ie = true;
											}
											if (ie)
													line += "}\n\n";
											else
													line += "function obj" + eName.replace(" ", "") + "() {}\n";
											
											for (int e=0;e<line.length();e++) {
													buf = (byte) line.toCharArray()[e];
													vars.write(buf);
											}
									}
					
					//Keyboard
					line = "";
					ie = false;
					gmobjects = gmfile.gmObjects.iterator();
					while (gmobjects.hasNext()) {
							GmObject curobj = gmobjects.next();
							GmObject qobj = curobj;
							boolean cont = false;
							while (qobj != null) {
									if (qobj.mainEvents.get(MainEvent.EV_KEYBOARD).events.size() > 0) {
											cont = true;
											break;
									}
									
									ResourceReference ref2 = qobj.get(PGmObject.PARENT);
									if (ref2 == null)
											qobj = null;
									else
											qobj = (GmObject) ref2.get();
							}
							
							if (!cont)
								continue;
							if (!ie)
									line += "function objKeys(i) {\n";
							
							line += "\t"+curobj.getName()+".Keyboard(i);\n";
							ie = true;
					}
					if (ie)
							line += "}\n\n";
					else
							line += "function objKeys(){}\n";
					
					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							vars.write(buf);
					}
					
					//Keyboard Press
					line = "";
					ie = false;
					gmobjects = gmfile.gmObjects.iterator();
					while (gmobjects.hasNext()) {
							GmObject curobj = gmobjects.next();
							GmObject qobj = curobj;
							boolean cont = false;
							while (qobj != null) {
									if (qobj.mainEvents.get(MainEvent.EV_KEYPRESS).events.size() > 0) {
											cont = true;
											break;
									}
									
									ResourceReference ref2 = qobj.get(PGmObject.PARENT);
									if (ref2 == null)
											qobj = null;
									else
											qobj = (GmObject) ref2.get();
							}
							
							if (!cont)
								continue;

							if (!ie)
									line += "function objKeyP(i) {\n";
							
							line += "\t"+curobj.getName()+".KeyboardPress(i);\n";
							ie = true;
					}
					if (ie)
							line += "}\n\n";
					else
							line += "function objKeyP(){}\n";

					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							vars.write(buf);
					}
					
					//Keyboard Release
					line = "";
					ie = false;
					gmobjects = gmfile.gmObjects.iterator();
					while (gmobjects.hasNext()) {
							GmObject curobj = gmobjects.next();
							GmObject qobj = curobj;
							boolean cont = false;
							while (qobj != null) {
									if (qobj.mainEvents.get(MainEvent.EV_KEYRELEASE).events.size() > 0) {
											cont = true;
											break;
									}
									
									ResourceReference ref2 = qobj.get(PGmObject.PARENT);
									if (ref2 == null)
											qobj = null;
									else
											qobj = (GmObject) ref2.get();
							}
							
							if (!cont)
								continue;
		
							if (!ie)
									line += "function objKeyR(i) {\n";
							
							line += "\t"+curobj.getName()+".KeyboardRelease(i);\n";
							ie = true;
					}
					if (ie)
							line += "}\n\n";
					else
							line += "function objKeyR(){}\n";

					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							vars.write(buf);
					}
					
					//Room Start
					line = "";
					ie = false;
					gmobjects = gmfile.gmObjects.iterator();
					while (gmobjects.hasNext()) {
							GmObject curobj = gmobjects.next();
							GmObject qobj = curobj;
							boolean cont = false;
							while (qobj != null) {
									for (org.lateralgm.resources.sub.Event e : qobj.mainEvents.get(MainEvent.EV_OTHER).events)
											if (e.toString().equals("Room Start")) {
													cont = true;
													break;
											}
									
									if (cont)
											break;
									
									ResourceReference ref2 = qobj.get(PGmObject.PARENT);
									if (ref2 == null)
											qobj = null;
									else
											qobj = (GmObject) ref2.get();
							}
							
							if (!cont)
								continue;

							if (!ie)
									line += "function objRoomStart() {\n";
							
							line += "\t"+curobj.getName()+".RoomStart();\n";
							ie = true;
					}
					if (ie)
							line += "}\n\n";
					else
							line += "function objRoomStart(){}\n";
					
					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							vars.write(buf);
					}
					
					//Room End
					line = "";
					ie = false;
					gmobjects = gmfile.gmObjects.iterator();
					while (gmobjects.hasNext()) {
							GmObject curobj = gmobjects.next();
							GmObject qobj = curobj;
							boolean cont = false;
							while (qobj != null) {
									for (org.lateralgm.resources.sub.Event e : qobj.mainEvents.get(MainEvent.EV_OTHER).events)
											if (e.toString().equals("Room End")) {
													cont = true;
													break;
											}
									
									if (cont)
											break;
									
									ResourceReference ref2 = qobj.get(PGmObject.PARENT);
									if (ref2 == null)
											qobj = null;
									else
											qobj = (GmObject) ref2.get();
							}
							
							if (!cont)
								continue;

							if (!ie)
									line += "function objRoomEnd() {\n";
							
							line += "\t"+curobj.getName()+".RoomEnd();\n";
							ie = true;
					}
					if (ie)
							line += "}\n\n";
					else
							line += "function objRoomEnd(){}\n";
					
					for (int e=0;e<line.length();e++) {
							buf = (byte) line.toCharArray()[e];
							vars.write(buf);
					}

					//Object
					line = "";
					int i = 0;
					gmobjects = gmfile.gmObjects.iterator();
					while (gmobjects.hasNext()) {
							GmObject curobj = gmobjects.next();
							i++;
							String objname = curobj.getName();
							line = "//"+objname+"\n"+objname+" = function() {\n	//Do nothing\n}\n\n"+objname+".id = new Array();\n\n"+objname+".Create = function(i, x, y) {\n	"+objname+".id[i] = new Array();\n	"+objname+".id[i][\"x\"] = x;\n	"+objname+".id[i][\"y\"] = y;\n	"+objname+".id[i][\"startx\"] = x;\n	"+objname+".id[i][\"starty\"] = y;\n\t"+objname+".id[i][\"depth\"] = "+curobj.get(GmObject.PGmObject.DEPTH)+";\n\t"+objname+".id[i][\"visible\"] = "+curobj.get(GmObject.PGmObject.VISIBLE)+";\n\t"+objname+".id[i][\"objectIndex\"] = "+objname+";\n\t";
							
							String spr = "-1";
							if (curobj.get(GmObject.PGmObject.SPRITE) != null)
									spr = ((ResourceReference) curobj.get(GmObject.PGmObject.SPRITE)).get().getName();
							
							line += objname+".id[i][\"sprite\"] = "+spr+";\n\t";
							
							String mask = "-1";
							if (curobj.get(GmObject.PGmObject.MASK) != null)
								mask = ((ResourceReference) curobj.get(GmObject.PGmObject.MASK)).get().getName();
							
							line += objname+".id[i][\"mask\"] = "+mask+";\n\t";
							line += objname+".id[i][\"imgIndex\"] = 0;\n\t" + objname+".id[i][\"imgSpeed\"] = 1;\n\t";
							line += objname+".id[i][\"imgXscale\"] = 1;\n\t" + objname+".id[i][\"imgYscale\"] = 1;\n\t";
							line += objname+".id[i][\"imgAngle\"] = 0;\n\t";

							line += objname+".id[i][\"persistent\"] = "+curobj.get(PGmObject.PERSISTENT)+";\n\t";

							line += objname+".id[i][\"alarm\"] = new Array();\n\t";
							line += objname+".id[i][\"alarm\"][0] = -1;\n\t";
							line += objname+".id[i][\"alarm\"][1] = -1;\n\t";
							line += objname+".id[i][\"alarm\"][2] = -1;\n\t";
							line += objname+".id[i][\"alarm\"][3] = -1;\n\t";
							line += objname+".id[i][\"alarm\"][4] = -1;\n\t";
							line += objname+".id[i][\"alarm\"][5] = -1;\n\t";
							line += objname+".id[i][\"alarm\"][6] = -1;\n\t";
							line += objname+".id[i][\"alarm\"][7] = -1;\n\t";
							line += objname+".id[i][\"alarm\"][8] = -1;\n\t";
							line += objname+".id[i][\"alarm\"][9] = -1;\n\t";
							line += objname+".id[i][\"alarm\"][10] = -1;\n\t";
							line += objname+".id[i][\"alarm\"][11] = -1;\n";
							if (curobj.mainEvents.get(MainEvent.EV_CREATE).events.size() > 0) {
									Iterator<org.lateralgm.resources.sub.Event> ev_create = curobj.mainEvents.get(MainEvent.EV_CREATE).events.iterator();
									while (ev_create.hasNext()) {
											org.lateralgm.resources.sub.Event ev_cur = ev_create.next();
											Iterator<org.lateralgm.resources.sub.Action> ev_actions = ev_cur.actions.iterator();
											while (ev_actions.hasNext()) {
													org.lateralgm.resources.sub.Action action = ev_actions.next();
													Iterator<Argument> eab = action.getArguments().iterator();
													while (eab.hasNext()) {
															Argument arg = eab.next();
															if ((arg.kind == Argument.ARG_STRING)&&(!Character.isDigit(arg.getVal().charAt(0))))
																	line += "\t" + gmltoghp(arg.getVal(), objname, "\t").trim() + "\n";
															else
																	line += "	//Sorry no DnD support yet\n";
													}
											}
									}
							}
							line += "\t" + objname + ".id[i][\"glin\"] = glin.length;\n";
							line += "\tglin[glin.length] = " + objname + ".id[i];\n";
							line += "}\n\n";
							for (int e=0;e<line.length();e++) {
									buf = (byte) line.toCharArray()[e];
									vars.write(buf);
							}
							proBar(Math.round(15+(2*i/objnum)));
							
							//Process events.
							processEvent(curobj, objname, MainEvent.EV_DESTROY, vars);
							processEvent(curobj, objname, MainEvent.EV_ALARM, vars);
							processEvent(curobj, objname, MainEvent.EV_STEP, vars);
							processEvent(curobj, objname, MainEvent.EV_COLLISION, vars);
							processEvent(curobj, objname, MainEvent.EV_KEYBOARD, vars);
							processEvent(curobj, objname, MainEvent.EV_KEYPRESS, vars);
							processEvent(curobj, objname, MainEvent.EV_KEYPRESS, vars);
							processEvent(curobj, objname, MainEvent.EV_MOUSE, vars);
							processEvent(curobj, objname, MainEvent.EV_OTHER, vars);
							processEvent(curobj, objname, MainEvent.EV_DRAW, vars);
							processEvent(curobj, objname, MainEvent.EV_TRIGGER, vars);
					}
					
					/////////
					//Rooms//
					/////////
					
					line = "//Rooms\n\n";
					i = 0;
					while (rooms.hasNext()) {
							Room rm = rooms.next();
							line = "//"+rm.getName()+"\n"+rm.getName()+" = "+i+";\nresource[8]["+i+"] = function(i) {\n	//Do nothing\n}\n\nresource[8]["+i+"].inst = new Array();\nresource[8]["+i+"].tiles = new Array();\n\nresource[8]["+i+"].rmCrCode = ";
							if ((rm.get(PRoom.CREATION_CODE) == null)||(rm.get(PRoom.CREATION_CODE) == ""))
									line += "false;\n\n";
							else
									line += "function() {\n"+gmltoghp(rm.get(PRoom.CREATION_CODE).toString(), "", "\t")+"\n}\n\n";
							
							Iterator<Instance> rmi = rm.instances.iterator();
							
							boolean icc = false;
							line += "resource[8]["+i+"].objCrCode = ";
							while (rmi.hasNext()) {
									Instance inst = rmi.next();
									if ((inst.properties.get(PInstance.CREATION_CODE) != null)&&(inst.properties.get(PInstance.CREATION_CODE) != "")) {
											if (icc)
													line += gmltoghp(inst.getCreationCode(), "", "\t");
											else
													line += "function() {\n	with ("+inst.properties.get(PInstance.ID)+") {\n		"+gmltoghp(inst.getCreationCode(), "", "\t").replace("\n", "\n		")+"\n	}\n";
											icc = true;
									}
							}
							
							if (!icc)
									line += "false;\n\n";
							else
									line += "}\n\n";
							
							line += "resource[8]["+i+"].width = "+rm.get(PRoom.WIDTH)+";\nresource[8]["+i+"].height = "+rm.get(PRoom.HEIGHT)+";\nresource[8]["+i+"].backgroundColor = "+colorToJS((Color)rm.get(PRoom.BACKGROUND_COLOR))+";\nresource[8]["+i+"].drawBackgroundColor = "+rm.get(PRoom.DRAW_BACKGROUND_COLOR)+";\n\n";
							
							//Backgrounds
							line += "resource[8]["+i+"].backgrounds = new Array();\n";
							for (int e = 0; e < rm.backgroundDefs.size(); e++) {
									BackgroundDef def = rm.backgroundDefs.get(e);
									ResourceReference ref2 = (ResourceReference) def.properties.get(PBackgroundDef.BACKGROUND);
									
									if (ref2 == null)
										continue;
									
									String bkg = ((ResourceReference) def.properties.get(PBackgroundDef.BACKGROUND)).get().getName();
									boolean vis = def.properties.get(PBackgroundDef.VISIBLE);
									boolean fore = def.properties.get(PBackgroundDef.FOREGROUND);
									int x = def.properties.get(PBackgroundDef.X);
									int y = def.properties.get(PBackgroundDef.Y);
									boolean tileH = def.properties.get(PBackgroundDef.TILE_HORIZ);
									boolean tileV = def.properties.get(PBackgroundDef.TILE_VERT);
									boolean stretch = def.properties.get(PBackgroundDef.STRETCH);
									int hsp = def.properties.get(PBackgroundDef.H_SPEED);
									int vsp = def.properties.get(PBackgroundDef.V_SPEED);
									line += "resource[8]["+i+"].backgrounds[" + e + "] = new RoomBackground(" + bkg + ", " + vis + ", " + fore + ", " + x + ", " + y + ", " + tileH + ", " + tileV + ", " + stretch + ", " + hsp + ", " + vsp + ");\n";
							}
							line += "\n";
							
							line += "resource[8]["+i+"].Create = function() {\n\t//Instances\n";
							rmi = rm.instances.iterator();
							int e = 0;
							while (rmi.hasNext()) {
									Instance inst = rmi.next();
									line += "\tresource[8]["+i+"].inst["+e+"] = new Array();\n\tresource[8]["+i+"].inst["+e+"][0] = "+deRef((ResourceReference<?>) inst.properties.get(PInstance.OBJECT))+";\n\tresource[8]["+i+"].inst["+e+"][1] = "+inst.getPosition().x+";\n\tresource[8]["+i+"].inst["+e+"][2] = "+inst.getPosition().y+";\n";
									e++;
							}
							
							Iterator<Tile> tiles = rm.tiles.iterator();
							line += "\n\t//Tiles\n";
							e = 0;
							while (tiles.hasNext()) {
									Tile t = tiles.next();
									line += "\tresource[8]["+i+"].tiles[" + e + "] = new Array();\n";
									line += "\tresource[8]["+i+"].tiles[" + e + "][0] = " + ((ResourceReference) t.properties.get(PTile.BACKGROUND)).get().getName() + ";\n";
									line += "\tresource[8]["+i+"].tiles[" + e + "][1] = " + t.getBackgroundPosition().x + ";\n";
									line += "\tresource[8]["+i+"].tiles[" + e + "][2] = " + t.getBackgroundPosition().y + ";\n";
									line += "\tresource[8]["+i+"].tiles[" + e + "][3] = " + t.getSize().width + ";\n";
									line += "\tresource[8]["+i+"].tiles[" + e + "][4] = " + t.getSize().height + ";\n";
									line += "\tresource[8]["+i+"].tiles[" + e + "][5] = " + t.getRoomPosition().x + ";\n";
									line += "\tresource[8]["+i+"].tiles[" + e + "][6] = " + t.getRoomPosition().y + ";\n";
									line += "\tresource[8]["+i+"].tiles[" + e + "][7] = " + t.getDepth() + ";\n";
									e++;
							}
							
							
							line += "}" + (rooms.hasNext() ? "\n\n" : "");
							for (e=0;e<line.length();e++) {
									buf = (byte) line.toCharArray()[e];
									vars.write(buf);
							}
							proBar(Math.round(18+(2*++i/roomnum)));
					}
			} catch (IOException e) {
					System.out.println("Could not include files.");
					return;
			}
			
			//Add sprites
			proBar("Converting Sprites...");
			File sprdir = new File(tmpdir+"/sprites");
			sprdir.mkdir();
			proBar(20);
			sprites = gmfile.sprites.iterator();
			for (int i=0;sprites.hasNext();i++) {
					Sprite sprname = sprites.next();
					try {
							ImageOutputStream spr = new FileImageOutputStream(new File(sprdir.getPath()+"/"+sprname.toString()+".png"));
							if (sprname.subImages.get(0) == null) {
									spr.close();
									continue;
							}
							BufferedImage img = new BufferedImage(sprname.getDisplayImage().getWidth()*sprname.subImages.size(), sprname.getDisplayImage().getHeight(), BufferedImage.TYPE_INT_ARGB);
							Graphics g = img.getGraphics();
							g.drawImage(sprname.subImages.get(0), 0, 0, this);
							for (int e=1;e<sprname.subImages.size();e++) {
									g.drawImage(sprname.subImages.get(e), sprname.getDisplayImage().getWidth()*e, 0, this);
							}
							ImageIO.write((RenderedImage) img, "png", spr);
							spr.close();
					} catch (IOException e) {
							System.out.println("Could not include "+sprname.toString()+".");
					}
					proBar(Math.round(22+(2*i/sprnum)));
			}
			if (sprnum == 0) {
					sprdir.delete();
			}
			
			//Add sounds
			proBar("Converting Sounds...");
			File snddir = new File(tmpdir+"/sounds");
			snddir.mkdir();
			proBar(25);
			sounds = gmfile.sounds.iterator();
			for (int i=0;sounds.hasNext();i++) {
					Sound sndname = sounds.next();
					try {
							FileOutputStream snd = new FileOutputStream(snddir.getPath()+"/"+sndname.toString()+".wav");
							byte[] aud = gmfile.sounds.get(sndname.toString()).data;
							if (aud == null) {
									snd.close();
									continue;
							}
							snd.write(aud);
							snd.close();
					} catch (IOException e) {
							System.out.println("Could not include "+sndname+".");
					}
					proBar(Math.round(27+(2*i/sndnum)));
			}
			if (sndnum == 0) {
					snddir.delete();
			}
			
			//Add backgrounds
			proBar("Converting Backgrounds...");
			File bckdir = new File(tmpdir+"/backgrounds");
			bckdir.mkdir();
			proBar(30);
			backgrounds = gmfile.backgrounds.iterator();
			for (int i=0;backgrounds.hasNext();i++) {
					Background bckname = backgrounds.next();
					try {
							FileOutputStream bck = new FileOutputStream(bckdir.getPath()+"/"+bckname.toString()+".png");
							BufferedImage bimg = gmfile.backgrounds.get(bckname.toString()).getDisplayImage();
							if (bimg == null) {
									bck.close();
									continue;
							}
							ImageIO.write(bimg, "png", bck);
							bck.close();
					} catch (IOException e) {
							System.out.println("Could not include "+bckname+".");
					}
					proBar(Math.round(32+(2*i/bcknum)));
			}
			if (bcknum == 0) {
					bckdir.delete();
			}
			
			//Add paths
			proBar("Converting Paths...");
			Iterator<Path> paths = gmfile.paths.iterator();
			int pathnum = gmfile.paths.size();
			proBar(35);
			for (int i=0;paths.hasNext();i++) {
					Path pathname = paths.next();
					try {
							int pnum = pathname.points.size();
							if (pnum == 0) {
									return;
							}
							//Write to file
							vars.write(null);
					} catch (IOException e) {
							System.out.println("Could not include "+pathname+".");
					}
					proBar(Math.round(37+(2*i/pathnum)));
			}
			if (pathnum == 0) {
					//Do something?
			}
			
			//Add to zip
			dir = dir.substring(0, dir.lastIndexOf("."));
			if (!zc.isSelected()) {
					proBar(100, "Done!");
					try {
							vars.close();
					} catch (IOException err) {
							System.out.println("Could not close files.");
					}
					try {
							if (System.getProperty("os.name").indexOf("Windows") != -1) {
									Runtime.getRuntime().exec("explorer /e /select,"+udir+"\\"+dir);
							}
							else if (System.getProperty("os.name").indexOf("Linux") != -1) {
									Runtime.getRuntime().exec("xdg-open "+udir+"/"+dir);
							}
							else if (System.getProperty("os.name").indexOf("Mac") != -1) {
									Runtime.getRuntime().exec("/usr/bin/open "+udir+"/"+dir);
							}
					} catch (IOException err) {
							System.out.println("Couldn't open "+udir+"/"+dir+".");
					}
					return;
			}
			proBar(0, "Packing into zip...");
			d = tmpdir;
			String[] entries = d.list();
			byte[] buffer = new byte[4096];
			int bytesRead;
			ZipOutputStream out = null;
			FileInputStream in = null;
			ZipEntry entry = null;
			File f = null;
			try {
					out = new ZipOutputStream(new FileOutputStream(udir+"/"+dir+".zip"));
			} catch (FileNotFoundException err) {
					System.out.println("Couldn't create zip "+dir+".zip.");
			}
			for (int i=0;i<entries.length;i++) {
					f = new File(d, entries[i]);
					if (f.isDirectory()) {
							File cdir = f;
							String[] subdirent = f.list(); 
							for (int e=0;e<subdirent.length;e++) {
									f = new File(cdir, subdirent[e]);
									try {
											in = new FileInputStream(tmpdir+"/"+cdir.getName()+"/"+f.getName());
									} catch (FileNotFoundException err) {
											System.out.println("Couldn't open entry "+tmpdir+"/"+cdir.getName()+"/"+f.getName()+".");
									}
									entry = new ZipEntry(cdir.getName()+"/"+f.getName());
									try {
											out.putNextEntry(entry);
									} catch (IOException err) {
											System.out.println("Couldn't add entry "+entry.getName()+".");
									}
									try {
											while ((bytesRead = in.read(buffer)) != -1) {
													out.write(buffer, 0, bytesRead);
											}
											out.flush();
											in.close();
									} catch (IOException err) {
											System.out.println("Couldn't write entry "+entry.getName()+".");
									}
							}
							proBar(i/rsnum);
							continue;
					}
					try {
							in = new FileInputStream(f);
					} catch (FileNotFoundException err) {
							System.out.println("Couldn't open entry "+f.getName()+".");
					}
					entry = new ZipEntry(f.getName());
					try {
							out.putNextEntry(entry);
					} catch (IOException err) {
							System.out.println("Couldn't add entry "+entry.getName()+".");
					}
					try {
							while ((bytesRead = in.read(buffer)) != -1) {
									out.write(buffer, 0, bytesRead);
							}
							out.flush();
							in.close();
					} catch (IOException err) {
							System.out.println("Couldn't write entry "+entry.getName()+".");
					}
					proBar(i/rsnum);
			}
			try {
					out.close();
					in.close();
			} catch (IOException err) {
					System.out.println("Couldn't close zip "+dir+".zip.");
			}
			delDir(tmpdir);
			proBar(100, "Done!");
			Desktop desktop = Desktop.getDesktop();
			try {
					desktop.open(new File(udir+"/"+dir+".zip"));
			} catch (IOException err) {
					System.out.println("Couldn't open zip "+dir+".zip.");
			}
	}
	
	public boolean isValidVarName(String s) {
		if (s.length() == 0)
			return false;
	
		if (!("" + s.charAt(0)).matches("[_A-Za-z]"))
			return false;
		
		if (!s.matches("[A-Za-z0-9_]*"))
			return false;
		
		return true;
	}
	
	public String colorToJS(Color c) {
		String rString = Integer.toHexString(c.getRed());
		String gString = Integer.toHexString(c.getGreen());
		String bString = Integer.toHexString(c.getBlue());
		
		if (rString.length() < 2)
			rString = "0" + rString;
		
		if (gString.length() < 2)
			gString = "0" + gString;
		
		if (bString.length() < 2)
			bString = "0" + bString;
		
		return "\"#" + rString + gString + bString + "\"";
	}
	
	public String gmltoghp(String code, String obj, String indent) {
			String[] cl = code.split("\n");
			
			String endl = "";
			if (cl.length > 1)
				endl = "\n" + indent;

			String cc = "";
			for (int i=0;i<cl.length;i++) {
					String before = cl[i].substring(0,cl[i].indexOf(cl[i].trim()));
					cl[i] = cl[i].trim();
			
					String nc = new String(cl[i]);
					
					//Inheritance
					if (cl[i].replace(";", "").equals("event_inherited()")) {
							cl[i] = "";
							GmObject ob = null;
							for (GmObject o : gmfile.gmObjects)
									if (o.toString().equals(obj)) {
											ob = o;
											break;
									}
							
							if (ob == null)
									continue;
							
							if (child == null)
									child = ob;
							
							GmObject par = (GmObject) ((ResourceReference) child.get(PGmObject.PARENT)).get();
							
							if (par == null || par.mainEvents.size() <= curMain || par.mainEvents.get(curMain).events.size() <= 0)
									continue;
							
							org.lateralgm.resources.sub.Event evt = null;
							for (org.lateralgm.resources.sub.Event ev : par.mainEvents.get(curMain).events)
									if (ev.toString().equals(curEvt)) {
											evt = ev;
											break;
									}
							
							if (evt == null || evt.actions.size() <= 0)
									continue;
							
							for (org.lateralgm.resources.sub.Action a : evt.actions) {
									Iterator<Argument> eab = a.getArguments().iterator();
									while (eab.hasNext()) {
											Argument arg = eab.next();
											if ((arg.kind == Argument.ARG_STRING) && (!Character.isDigit(arg.getVal().charAt(0))))
													cl[i] += gmltoghp(arg.getVal(), obj, indent).trim() + "\n" + indent;
									}
							}
							
							child = null;
							cc += cl[i];
							continue;
					}
					
					//Handle things like if statements and loops.
					String[] sts = {"else", "for", "if", "repeat", "while", "with"};
					String type = cl[i].trim().split("[\\s(]")[0];
					if (Arrays.binarySearch(sts, type) > -1) {
							//The part in parentheses and the part after that.
							String inp = "", end = "";
							
							String str = cl[i].substring(cl[i].indexOf(type) + type.length()).trim();
							
							if (str.length() > 0) {
									if (type.equals("else")) {
											cl[i] = "else " + gmltoghp(str, obj, "").trim();
											cc += cl[i] + (i < cl.length - 1 ? endl : "");
											continue;
									}
							
								//The first meaningful character after the statement word.
								char firstChar = str.charAt(0);
								
								//Not already in parentheses.
								if (firstChar != '(') {
										if (str.contains("{")) {
												inp = str.substring(str.indexOf(type) + type.length(), str.indexOf('{'));
												end = str.substring(str.indexOf('{'));
										} else
												inp = str.substring(str.indexOf(type) + type.length());
								}
								else {
										//Find the part in parenthesis.
										int in = cl[i].indexOf('(');
										int pnum = 1;
										while (pnum > 0 && ++in < cl[i].length()) {
												char ch = cl[i].charAt(in);
												if (ch == '(')
													pnum++;
												else if (ch == ')')
													pnum--;
												if (pnum > 0)
													inp += ch;
										}
										
										if (in < cl[i].length())
											end = cl[i].substring(in + 1);
								}
								
								//Convert to JavaScript.
								String[] parts;
								if (type.equals("for")) {
										parts = new String[3];
										String strType = "";
										int ind = 0;
										String cStr = "";
										for (int j = 0; j < inp.length(); j++) {
												char ch = inp.charAt(j);
												if (("" + ch).equals(strType))
														strType = "";
												else if ((ch == '"' || ch == '\'') && strType.length() == 0)
														strType = "" + ch;
												
												cStr += ch;
												
												if (ch == ';' && strType.length() == 0) {
														if (ind >= 3)
															break;
														
														parts[ind++] = cStr;
														cStr = "";
												}
												
												if (j == inp.length() - 1)
														parts[ind] = cStr;
										}
								} else {
										parts = new String[1];
										parts[0] = inp;
								}
								
								for (int j = 0; j < parts.length; j++) {
										parts[j] = gmltoghp(parts[j], obj, "").trim();
										if (parts[j].charAt(parts[j].length() - 1) == ';') {
												if (j == parts.length - 1)
													parts[j] = parts[j].substring(0, parts[j].length() - 1);
										} else if (j < parts.length - 1)
												parts[j] += ';';
								}
	
								//Replace single equals signs with double equals signs.
								if (!type.equals("repeat") && !type.equals("with")) {
									String boolPart = "";
									if (type.equals("for"))
										boolPart = parts[2];
									else
										boolPart = parts[0];
									int eq = 0;
									while (boolPart.indexOf('=', eq) > -1 && eq < boolPart.length()) {
										int eInd = boolPart.indexOf('=', eq);
										
										char prevChar = ' ';
										char nextChar = ' ';
										
										if (eInd > 0)
											prevChar = boolPart.charAt(eInd - 1);
										
										if (eInd < boolPart.length() - 1)
											nextChar = boolPart.charAt(eInd + 1);
										
										if ((eInd == boolPart.length() - 1 || nextChar != '=') && prevChar != '<' && prevChar != '>' && prevChar != '!')
											boolPart = boolPart.substring(0, ++eInd) + '=' + boolPart.substring(eInd);
										
										eq = eInd + 1;
									}
								}
								
								if (type.equals("for"))
										inp = parts[0] + " " + parts[1] + " " + parts[2];
								else
										inp = parts[0];
								
								end = gmltoghp(end, obj, "").trim();
								
								if (type.equals("repeat"))
									cl[i] = "for (var __repvar = 0; __repvar < (" + inp + "); __repvar++) " + end;
								else
									cl[i] = type + " (" + inp + ") " + end;
							}
							
							cc += cl[i] + (i < cl.length - 1 ? endl : "");
							if (i < cl.length - 1)
								cc += indent + before;
							continue;
					}
					
					int pap = cl[i].indexOf("(");
					
					//Convert simple statements without parentheses
					if (pap == -1) {
							String regex = "[\\s+\\-/*\\[{;<>=!]";
							
							int len = 1;
							for (char ch : cl[i].toCharArray())
								if (("" + ch).matches(regex))
									len += 2;
							
							String[] cla = new String[len];
							String cStr = "";
							String strType = "";
							int ind = 0;
							for (int e = 0; e < cl[i].length(); e++) {
									char ch = cl[i].charAt(e);
									
									if (("" + ch).matches(regex) && strType.length() == 0) {
											cla[ind++] = cStr;
											cStr = "";
											
											String addStr = "" + ch;
											if (e < cl[i].length() - 1 && (ch == '=' || ch == '>' || ch == '<' || ch == '!') && cl[i].charAt(e + 1) == '=')
												addStr += '=';
											
											cla[ind++] = addStr;
											e += addStr.length() - 1;
											
											continue;
									}
									
									if (e == cl[i].length() - 1) {
											cStr += ch;
											cla[ind] = cStr;
											break;
									}
									
									cStr += ch;
									
									if (strType.length() > 0) {
										if (ch == strType.charAt(0))
											strType = "";
									}
									else if (ch == '"')
										strType = "\"";
									else if (ch == '\'')
										strType = "'";
							}
							
							cl[i] = "";
							for (int e=0;e<cla.length;e++) {
									if (cla[e] == null)
											break;
									
									//Variables of other objects
									if (cla[e].contains(".") && !cla[e].contains("'") && !cla[e].contains("\"") && !cla[e].trim().replace(",", "").replace(";", "").matches("[+\\-]?[0-9]*[.]?[0-9]*")) {
											String[] ov = cla[e].split("[.]");
											cla[e] = "";
											for (int j = 0; j < ov.length; j++) {
													String v = ov[j].trim().replace(",", "").replace(";", "");
													
													//References to variables of objects or global variables.
													boolean isVar = true;
													if (j == 0)
															if (v.equals("global")) {
																	ov[j] = ov[j].replace(v, "global");
																	isVar = false;
															}
															else if (Arrays.binarySearch(objNames, v) > 0) {
																	ov[j] = ov[j].replace(v, v + ".id[0]");
																	isVar = false;
															}

													//References to instances referred to by variables.
													if (isVar && isValidVarName(v)) {
															String beg = "";
															if (j == 0)
																beg = obj + ".id[i]";
															
															//Alarms
															if (v.matches("alarm[0-9]"))
																	ov[j] = beg + ov[j].replace(v, "[\"alarm\"][" + v.substring(5) + "]");
															else //Others
																	ov[j] = beg + ov[j].replace(v, "[\"" + v + "\"]");
													}
													cla[e] += ov[j];
											}
											cl[i] += cla[e];
											continue;
									}

									boolean found = false;
									for (int o=0;o<gmc.length;o++) {
											if (cla[e].trim().replace(",", "").replace(";", "").equals(gmc[o].trim())) {
													cla[e] = cla[e].replace(gmc[o].trim(), gcc[o].trim().replace("%obj%", obj + ".id[i]"));
													found = true;
													break;
											}
									}
									
									//Handle custom variables.
									if (!found) {
										String v = cla[e].trim().replace(",", "").replace(";", "");
										if (isValidVarName(v)) {
											String[] exclude = {"begin", "break", "case", "continue", "default", "do", "end", "exit", "for", "globalvar", "if", "repeat", "return", "switch", "then", "until", "var", "while", "with", "xor"};
											found = Arrays.binarySearch(resourceNames,v) >= 0 || Arrays.binarySearch(exclude,v) >= 0;
											
										if (!found)
											cla[e] = cla[e].replace(v, obj + ".id[i][\"" + v + "\"]");
										}
									}
									
									cl[i] += cla[e];
							}
							cc += cl[i] + (i < cl.length - 1 ? endl : "");
							if (i < cl.length - 1)
								cc += indent + before;
							continue;
					}
					
					//Separate the code into different parts and convert them.
					
					//The different characters that separate code pieces.
					String regex = "[\\s+\\-/*\\[{;<>=!]";
					
					//This part allows two functions to be used in the same line of
					//code without one being inside an argument of the other.
					boolean cont = false;
					int pCount = 1;
					String strType2 = "";
					for (int in = pap + 1; in < cl[i].lastIndexOf(')'); in++)
						if (strType2.length() == 0) {
								if (cl[i].charAt(in) == '"')
									strType2 = "\"";
								else if (cl[i].charAt(in) == '\'')
									strType2 = "'";
								else if (cl[i].charAt(in) == '(')
									pCount++;
								else if (cl[i].charAt(in) == ')')
									if (--pCount == 0) {
										cont = true;
										break;
									}
						} else if (("" + cl[i].charAt(in)).equals(strType2))
							strType2 = "";
					
					String rem = cl[i].substring(cl[i].lastIndexOf(')') + 1).trim();
					if (!cont && !rem.equals("") && !rem.equals(";"))
						cont = true;
					
					Matcher m = Pattern.compile(regex).matcher(cl[i]);
					if (m.find() && (m.start() < pap || cont)) {
						//The different pieces of code
						String[] parts = new String[cl[i].split(regex).length];
						String strType = "";
						int pNum = 0;
						int curPart = 0;
						
						//Some strings use double quotes while other use single quotes. This method handles both.
						String str = "";
						
						//Go through every character in the string.
						for (int in2 = 0; in2 < cl[i].length(); in2++) {
							char ch = cl[i].charAt(in2);
							
							//If in a string and the end quote is found, get out of the string.
							if (strType.length() > 0 && ch == strType.charAt(0)) {
								strType = "";
								str += ch;
								continue;
							}
							
							//If not in a string.
							if (strType.equals("")) {
								//In parentheses
								if (ch == '(' || ch == '[')
									pNum++;
								else if (ch == ')' || ch == ']')
									pNum--;
								//Quote stuff
								else if (ch =='"')
									strType = "\"";
								else if (ch =='\'')
									strType = "'";
								//If a separating character is found, separate the pieces of code.
								else if (("" + ch).matches(regex) && pNum == 0) {
									str += ch;
									parts[curPart++] = str;
									str = "";
									continue;
								}
							}
							
							str += ch;
							
							if (in2 == cl[i].length() - 1)
								parts[curPart] = str;
						}
						
						//Put the parts together.
						String fStr = indent + before;
						for (String part : parts) {
							if (part == null)
								break;
							
							if (part.equals(""))
								continue;
							
							//Convert to JavaScript
							
							//If in parentheses, convert only the inner part.
							if (part.charAt(0) == '(' || part.charAt(0) == '[') {
								char start = part.charAt(0);
								int endi = -1;
								if (start == '[')
									endi = part.lastIndexOf(']');
								else
									endi = part.lastIndexOf(')');
								String end = part.substring(endi);
								part = start + gmltoghp(part.substring(1, endi), obj, "") + end;
							}
							else if (!part.equals(cl[i]))
								part = gmltoghp(part, obj, "");
							
							fStr += part;
						}
						
						cl[i] = fStr;
						cc += cl[i] + (i < cl.length - 1 ? endl : "");
						if (i < cl.length - 1)
							cc += indent + before;
						continue;
					}
					
					//Ignore stuff added for GM compatibility with strictly GHP functions.
					if (nc.contains("initGHP")) {
							continue;
					}
					
					//Rename GML names to GHP names.
					int gc = -1;
					for (int e=0;e<gml.length;e++) {
							String gmlc = gml[e];
							
							//If it is a function, rename only the function part.
							int gmlp = gmlc.indexOf("(");
							gmlc = gmlc.substring(0, (gmlp == -1) ? gmlc.length() : gmlp);
							if (nc.substring(0, pap).equals(gmlc)) {
									gc = e;
									break;
							}
					}
					
					//Stuff with functions
					String ars = "";
					if (gc > -1) {
							//The string containing all of the arguments
							String ac = gcl[gc].substring(gcl[gc].indexOf("(")+1);
							
							//Exit if there is no closing parenthesis
							if (ac.indexOf(")") == -1) {break;}
							
							ac = ac.substring(0, ac.indexOf(")"));
							
							//The array of arguments for the function as required for GHP
							String[] garg = gcl[gc].split(",");
							
							//Replace the arguments.
							if (garg.length > 0) {
							
								//The array of arguments as provided by GM
								String[] oarg = new String[17];
								
								//Put the arguments in the array.
								if (nc.indexOf("(") > -1 || nc.indexOf("'") > -1 || nc.indexOf("\"") > -1) {
										String as = nc.substring(nc.indexOf("(")+1, nc.lastIndexOf(")"));
										if (as.indexOf(")") > -1) {
												int curArg = 0;
												int pNum = 0;
												String arg = "";
												String strType = "";
												for (int ind = 0; ind < as.length(); ind++) {
														char ch = as.charAt(ind);
														
														if (!strType.equals("")) {
															if (ch == strType.charAt(0))
																strType = "";
														} else {
															if (ch == '"')
																strType = "\"";
															else if (ch == '\'')
																strType = "'";
															else if (ch == '(')
																pNum++;
															else if (ch == ')')
																pNum--;
															if (ch == ',' && pNum == 0 || ind == as.length() - 1) {
																if (ch != ',')
																	arg += ch;
																oarg[curArg++] = arg;
																arg = "";
																continue;
															}
														}
														arg += ch;
												}
										}
										else {
												oarg = as.split(",");
										}
								}
								else {
										oarg = nc.split(",");
								}
								
								//Fix unordered arguments.
								for (int e=0;e<oarg.length;e++) {
										if ((e+1 < oarg.length)&&(oarg[e] == null)) {
												oarg[e] = oarg[e+1];
										}
								}
								
								//Convert the arguments to JavaScript.
								int numargs = oarg.length;
								for (int e=0;e<oarg.length;e++) {
										if (oarg[e] == null) {
												numargs = e;
												break;
										}
										oarg[e] = oarg[e].trim();
										oarg[e] = gmltoghp(oarg[e], obj, "");
								}
								
								//Put the arguments together in the function.
								boolean added = false;
								for (int e=0;e<garg.length;e++) {
										boolean thisadded = false;
										if (garg[e].contains("%obj%")) {
												if (added)
													ars += ", ";
												ars += obj + ".id[i]";
												thisadded = true;
										}
										else if (garg[e].contains("%q%") && numargs >= 1) {
												if (added)
													ars += ", ";
												ars += oarg[0];
												thisadded = true;
										}
										else if (garg[e].contains("%w%") && numargs >= 2) {
												if (added)
													ars += ", ";
												ars += oarg[1];
												thisadded = true;
										}
										else if (garg[e].contains("%e%") && numargs >= 3) {
												if (added)
													ars += ", ";
												ars += oarg[2];
												thisadded = true;
										}
										else if (garg[e].contains("%r%") && numargs >= 4) {
												if (added)
													ars += ", ";
												ars += oarg[3];
												thisadded = true;
										}
										else if (garg[e].contains("%t%") && numargs >= 5) {
												if (added)
													ars += ", ";
												ars += oarg[4];
												thisadded = true;
										}
										else if (garg[e].contains("%y%") && numargs >= 6) {
												if (added)
													ars += ", ";
												ars += oarg[5];
												thisadded = true;
										}
										else if (garg[e].contains("%u%") && numargs >= 7) {
												if (added)
													ars += ", ";
												ars += oarg[6];
												thisadded = true;
										}
										else if (garg[e].contains("%i%") && numargs >= 8) {
												if (added)
													ars += ", ";
												ars += oarg[7];
												thisadded = true;
										}
										else if (garg[e].contains("%o%") && numargs >= 9) {
												if (added)
													ars += ", ";
												ars += oarg[8];
												thisadded = true;
										}
										else if (garg[e].contains("%p%") && numargs >= 10) {
												if (added)
													ars += ", ";
												ars += oarg[9];
												thisadded = true;
										}
										else if (garg[e].contains("%a%") && numargs >= 11) {
												if (added)
													ars += ", ";
												ars += oarg[10];
												thisadded = true;
										}
										else if (garg[e].contains("%s%") && numargs >= 12) {
												if (added)
													ars += ", ";
												ars += oarg[11];
												thisadded = true;
										}
										else if (garg[e].contains("%d%") && numargs >= 13) {
												if (added)
													ars += ", ";
												ars += oarg[12];
												thisadded = true;
										}
										else if (garg[e].contains("%f%") && numargs >= 14) {
												if (added)
													ars += ", ";
												ars += oarg[13];
												thisadded = true;
										}
										else if (garg[e].contains("%g%") && numargs >= 15) {
												if (added)
													ars += ", ";
												ars += oarg[14];
												thisadded = true;
										}
										else if (garg[e].contains("%h%") && numargs >= 16) {
												if (added)
													ars += ", ";
												ars += oarg[15];
												thisadded = true;
										}
										else if (garg[e].contains("%j%") && numargs >= 17) {
												if (added)
													ars += ", ";
												ars += oarg[16];
												thisadded = true;
										}
										
										added = thisadded;
										
										if (e == garg.length-1) {
												if (cl[i].indexOf(")") != -1) {
														ars += ")";
												}
												if (!indent.equals("")) {
														ars += ";";
												}
										}
								}
							}
							nc = new String(gcl[gc].substring(0, gcl[gc].indexOf("(")+1));
					}
					cc += indent + before + nc + ars;
					if (!indent.equals("") && i < cl.length - 1)
							cc += "\n";
			}
			
			return cc;
	}
	
	public boolean delDir(File dir) {
			if (dir.isDirectory()) {
					String[] df = dir.list();
					for (int i=0;i<df.length;i++) {
							boolean b = delDir(new File(dir, df[i]));
							if (!b) {
									System.out.println("Couldn't delete temp file "+df[i]+".");
									return false;
							}
					}
			}
			return dir.delete();
	}

	public boolean processEvent(GmObject curobj, String objname, byte evt, FileOutputStream vars) {
			String line = "";
			boolean hasStep = false;
			boolean hasDraw = false;
			curMain = evt;
			ArrayList<org.lateralgm.resources.sub.Event> events = new ArrayList<org.lateralgm.resources.sub.Event>();
			ArrayList<GmObject> objs = new ArrayList<GmObject>();
			GmObject qobj = curobj;
			while (qobj != null) {
					for (org.lateralgm.resources.sub.Event e : qobj.mainEvents.get(evt).events) {
							boolean found = false;
							for (org.lateralgm.resources.sub.Event e2 : events)
									if (e.toString().equals(e2.toString())) {
											found = true;
											break;
									}

							if (!found) {
									events.add(e);
									objs.add(qobj);
							}
					}
					
					ResourceReference ref = qobj.get(PGmObject.PARENT);
					if (ref == null)
							qobj = null;
					else
							qobj = (GmObject) ref.get();
			}
			
			if (events.size() > 0) {
					Iterator<org.lateralgm.resources.sub.Event> ev = events.iterator();
					int evInd = 0;
					while (ev.hasNext()) {
							boolean ia = false;
							org.lateralgm.resources.sub.Event ev_cur = ev.next();
							curEvt = ev_cur.toString();
							Iterator<org.lateralgm.resources.sub.Action> ev_actions = ev_cur.actions.iterator();
							if (ev_cur.toString().equals("Normal Step")) {
									line += objname+".Step = function() {\n	";
									hasStep = true;
							}
							else if (ev_cur.toString().equals("Draw")) {
									line += objname+".Draw = function(i) {\n";
									hasDraw = true;
							}
							else if (ev_cur.toString().equals("Animation End")) {
									line += objname+".AnimationEnd = function(i) {\n";
									hasDraw = true;
							}
							else if (evt == MainEvent.EV_ALARM) {
									if (!ia)
										line += objname + ".Alarm = function(i, num) {\n\t";
									ia = true;
							}
							else
									line += objname+"."+ev_cur.toString().replace(" ", "")+" = function() {\n\t";
							
							if (evt == MainEvent.EV_ALARM)
									line += "if (num == " + ev_cur.toString().replaceAll("[^\\d.]", "") + ") {\n\t\t";
							else if (!ev_cur.toString().equals("Create") && !ev_cur.toString().equals("Draw") && !ev_cur.toString().equals("Animation End"))
									line += "for (var i = 0; i < " + objname + ".id.length; i++) {\n\t\t";
							
							if (ev_cur.toString().matches("(?:Left|Right|Middle) (?:Button|Pressed|Released)"))
								line += "if (!checkPoint(mouseX, mouseY, " + objname + ".id[i]))\n\t\t\tcontinue;\n\t\t";
							
							while (ev_actions.hasNext()) {
									org.lateralgm.resources.sub.Action action = ev_actions.next();
									Iterator<Argument> eab = action.getArguments().iterator();
									while (eab.hasNext()) {
											Argument arg = eab.next();
											if ((arg.kind == Argument.ARG_STRING)&&(!Character.isDigit(arg.getVal().charAt(0)))) {
													child = objs.get(evInd);
													line += "\t" + gmltoghp(arg.getVal(), objname, "\t").trim() + "\n";
													child = null;
											}
											else
													line += "\t//Sorry no DnD support yet\n";
									}
							}
							
							if (ev_cur.toString().equals("Normal Step")) {
									if (curobj.mainEvents.get(MainEvent.EV_ALARM).events.size() > 0) {
											line += "\t\tfor (var j = 0; j < " + objname + ".id[i][\"alarm\"].length; j++) {\n\t\t\t";
											line += objname + ".id[i][\"alarm\"][j] = Math.max(-1, " + objname + ".id[i][\"alarm\"][j] - 1);\n\t\t\t";
											line += "if (" + objname + ".id[i][\"alarm\"][j] == 0)\n\t\t\t\t";
											line += objname + ".Alarm(i, j);\n\t\t";
											line += "}\n\t\t";
									}
									
									if (curobj.mainEvents.get(MainEvent.EV_OTHER).events.size() > 0) {
											boolean hasAnim = false;
											for (org.lateralgm.resources.sub.Event e : curobj.mainEvents.get(MainEvent.EV_OTHER).events) {
													if (e.toString().equals("Animation End")) {
															hasAnim = true;
															break;
													}
											}
											
											if (hasAnim) {
													line += "\n\t\tif (%obj%[\"sprite\"] >= 0 && %obj%[\"imgIndex\"] + %obj%[\"imgSpeed\"] >= resource[0][%obj%[\"sprite\"]].numb) {\n\t\t\t".replace("%obj%", objname + ".id[i]");
													line += objname + ".id[i][\"imgIndex\"] = 0;\n\t\t\t";
													line += objname + ".AnimationEnd(i);\n\t\t";
													line += "}";
											}
									}
							}
							
							if (!ev_cur.toString().equals("Draw") && !ev_cur.toString().equals("Animation End"))
									line += "\t}\n";
							if (evt != MainEvent.EV_ALARM || !ev.hasNext())
									line += "}\n\n";
							evInd++;
					}
					try {
							for (int e=0;e<line.length();e++) {
								byte buf = (byte) line.toCharArray()[e];
								vars.write(buf);
							}
					}
					catch (IOException e) {
							System.out.println("Could not write object event.");
					}
					return true;
			}

			if (!hasDraw && evt == MainEvent.EV_DRAW) {
					line += objname+".Draw = function(i) {\n	";
					line += "drawSelf("+objname+".id[i]);";
					line += "\n}\n\n";
					try {
							for (int e=0;e<line.length();e++) {
								byte buf = (byte) line.toCharArray()[e];
								vars.write(buf);
							}
					}
					catch (IOException e) {
							System.out.println("Could not write object event.");
					}
					return true;
			}
			else if (!hasStep && evt == MainEvent.EV_STEP) {
					line += objname+".Step = function() {\n	";
					line += "for (var i = 0; i < " + objname + ".id.length; i++) {\n\t\t";
					line += "for (var j = 0; j < " + objname + ".id[i][\"alarm\"].length; j++) {\n\t\t\t";
					line += objname + ".id[i][\"alarm\"][j] = Math.max(-1, " + objname + ".id[i][\"alarm\"][j] - 1);\n\t\t\t";
					line += "if (" + objname + ".id[i][\"alarm\"][j] == 0)\n\t\t\t\t";
					line += objname + ".Alarm(i, j);\n\t\t";
					line += "}\n";
					
					boolean hasAnim = false;
					for (org.lateralgm.resources.sub.Event e : curobj.mainEvents.get(MainEvent.EV_OTHER).events) {
							if (e.toString().equals("Animation End")) {
									hasAnim = true;
									break;
							}
					}
					
					if (hasAnim) {
							line += "\n\t\tif (%obj%[\"sprite\"] >= 0 && %obj%[\"imgIndex\"] + %obj%[\"imgSpeed\"] >= resource[0][%obj%[\"sprite\"]].numb) {\n\t\t\t\t".replace("%obj%", objname + ".id[i]");
							line += objname + ".id[i][\"imgIndex\"] = 0;\n\t\t\t\t";
							line += objname + ".AnimationEnd(i);\n\t\t\t";
							line += "}";
					}
					line += "\t}\n";
					line += "}\n\n";
					
					try {
							for (int e=0;e<line.length();e++) {
								byte buf = (byte) line.toCharArray()[e];
								vars.write(buf);
							}
					}
					catch (IOException e) {
							System.out.println("Could not write object event.");
					}
					return true;
			}
			return false;
	}
}