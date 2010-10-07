package edu.stanford.vis.examples;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;

import pv.mark.Mark;
import pv.mark.MarkEvent;
import pv.mark.Scene;
import pv.mark.constants.MarkType;
import pv.mark.constants.Shape;
import pv.mark.eval.EventHandler;
import pv.render.awt.gl.GLDisplay;
import pv.scene.Item;

/**
 * This example shows the use of caching a panel in video texture memory
 * to speed up rendering. If a panel changes infrequently or not at all,
 * this can result in significant performance improvements.
 * 
 * This particular demo uses an undulating sine wave path drawn over
 * a large field of dots.
 */
public class PanelCache {

	// Controls how finely the sine wave path is sampled
	private static int N = 10;
	// The number of dots to include in the background
	private static int M = 100000;
	// Variable indicating whether or not caching is enabled
	private static boolean cache = true;
	
	public static void main(String[] args) {
		System.out.println("Hit the space bar to toggle panel caching of background dots.");
		
		// first generate the sine wave path data
		final List<Double> data = new ArrayList<Double>(6*N);
		for (double x=0; x<6; x+=1.0/N) {
			data.add(Math.sin(x));
		}
		// next generate a large random field of dots 
		final List<Point2D> pts = new ArrayList<Point2D>();
	    for (int i=0; i<M; ++i) {
			pts.add(new Point2D.Double(Math.random(), Math.random()));
		}
	    // finally we will use this list to create a 4x4 panel grid of dots
	    List<Object> panels = Arrays.asList(null, null);
		
	    // -- DOT FIELD --
	    
	    // 1. create the root element, consisting of two offset panels
		final Scene dots = new Scene()
			.data(panels)
	        .left("{{10+340*index}}")
	        .top(10)
			.width(340)
	        .height(380)
	        .scene();
		
		// 2. give each panel two sub-layers
	    final Mark panel = dots.add(MarkType.Panel)
	    	.data(panels)
	    	.cache(true) // cache each panel instance as a texture
	    	.left(0)
	    	.top("{{190*index}}")
	    	.width(340)
	    	.height(190);
	    
	    // 3. add a field of random points to each panel
	    panel.add(MarkType.Dot)
			.datatype(Point2D.class)
			.data(pts)
			.left("{{parent.width * data.getX()}}")
			.bottom("{{parent.height * data.getY()}}")
			.shape(Shape.Point) // use a simple point as the shape
			// distribute colors across red & blue components
			.fill("{{Fill.solid(data.getX(), 0.5, data.getY(), 1)}}");
		
	    // -- SINE WAVE PATH --
	    
	    // 1. create the root element of the visualization
		final Scene vis = new Scene()
	        .width(500)
	        .height(200)
	        .top(100)
	        .bottom(100)
	        .left(100)
	        .right(100)
	        .sceneUpdate(new EventHandler() {
	        	double o = 0;
				public void handle(MarkEvent event, Item item) {
					// on each frame, update the sine wave data
		    		o += 0.02;
		    		updateData(data, o);
				}
	        })
	        .scene();
		
		// 2. add the sine wave data as a stroked path
	    vis.add(MarkType.Line)
	    	.datatype(Double.class)
	        .segmented(true) // break the path into segments
	        .data(data)
	        .left("{{index / (6.0*"+N+"-1) * 500}}")
	        .bottom("{{(data + 1)/2.0 * 200}}")
	        // modify stroke width and color based on the data
	        .stroke("{{Stroke.solid(30*data*data+10,"
	        	+ "Color.hsb((data+1)/2.5, 0.7, 0.8))}}");
	    
	    // update both visualizations
	    dots.update();
	    vis.repeatedUpdate(1.0/60); // 60 FPS
	    
	    GLDisplay display = new GLDisplay();
	    display.bgcolor(0x333333);
		display.setSize(700, 400);
		display.addScene(dots);
		display.addScene(vis);
		
		// Toggle caching when space bar is pressed
		display.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent k) {
				if (k.getKeyCode() == KeyEvent.VK_SPACE) {
					cache = !cache;
					panel.cache(cache);
					dots.update();
				}
			}
		});
		
		JFrame frame = new JFrame("Protovis Panel Cache Test");
		frame.getContentPane().add(display);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setResizable(false);
	}

	static void updateData(List<Double> data, double o) {
		int i = 0;
		for (double x=0; x<6; x+=1.0/N, ++i) {
			data.set(i, Math.sin(x+o));
		}
	}

}
