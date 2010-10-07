package pv.render;

import java.awt.Component;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.util.List;

import pv.mark.Scene;
import pv.scene.Item;

public interface Display {

	Component asComponent();
	
	// populate the display
	void addScene(Scene scene);
	boolean removeScene(Scene scene);
	List<Scene> scenes();
	List<Item> items();
	
	// configure and update the display
	void setSize(int width, int height);
	int getWidth();
	int getHeight();
	
	void render();	
	void preRender();
	void postRender();
	
	AffineTransform getTransform();
	AffineTransform getInverseTransform();
	
	// add / remove listeners
	void addKeyListener(KeyListener kl);
	
	// pan & zoom controls
	void reset();
	void pan(double dx, double dy);
	void zoom(double s, double x, double y);
	void rotate(double theta, double x, double y);
}
