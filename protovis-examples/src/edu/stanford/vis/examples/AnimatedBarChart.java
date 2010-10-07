package edu.stanford.vis.examples;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JFrame;

import pv.mark.Mark;
import pv.mark.Scene;
import pv.mark.constants.MarkType;
import pv.render.Display;
import pv.render.awt.gl.GLDisplay;
import pv.style.Easing;

/**
 * This class is a bar chart view testing Protovis' animation support.
 * Hit the space bar to do an animated step.
 * Type 'F' to update the data values.
 * Type 'S' to sort the data.
 * Type 'D' to shuffle the data.
 */
public class AnimatedBarChart {
	
	// method to randomly shuffle a list
	@SuppressWarnings("unchecked")
	public static void shuffle(List<?> list) {
		List<Object> ll = (List<Object>) list;
		for (int i=list.size(); --i >= 0;) {
			int j = (int)(Math.random() * i + 0.5);
			Object a = list.get(i);
			Object b = list.get(j);
			ll.set(i, b);
			ll.set(j, a);
		}
	}
	// comparator for sorting the data
	public static class ArrayComparator implements Comparator<int[]> {
		public static final ArrayComparator instance = new ArrayComparator();
		public int compare(int[] a, int[] b) { return b[0] - a[0]; }
	}
	
	public static void main(String[] args) {
		System.out.println("Hit the space bar to do an animated step.");
		System.out.println("Type 'F' to update the data values.");
		System.out.println("Type 'S' to sort the data.");
		System.out.println("Type 'D' to shuffle the data.");
		run();
	}
	
	public static void run() {
		final float dur = 0.5f; // default animation duration
		int N = 100, w = 500, h = 200; // sizing parameters
		
		// generate a list of data elements
		// we use a one element array so we can update the value
		final List<int[]> data = new ArrayList<int[]>();
		for (int i=0; i<N; ++i) {
			data.add(new int[]{(int)(1 + 100*Math.random())});
		}
		
		// -- vis ---
		
		// Create the visualization scene
		final Scene vis = new Scene(); 
		vis.top(10).left(10).width(w).height(h);
		
		// Define property bundles to define how items should
		//  (a) enter the scene: how to initialize newly visible elements
		//  (b) exit the scene: how removed elements should disappear
		// Note that be default, elements animate to/from alpha=0
		// To change this, one must define the alpha property
		Mark enter, exit;
		
		// new elements will 'grow' out of the baseline
		enter = Mark.create()
			.left("{{item.left + 5}}")
			.bottom(0)
			.height(0)
			.width(0)
			;
		// exiting elements will 'shrink' into the baseline
		exit = Mark.create()
			.left("{{item.left - 3}}")
			.bottom("{{item.bottom}}")
			.height(0)
			.width(0)
			;
		
//		// new elements will drop in from the sky
//		enter = Mark.build()
//			.left("{{item.left + 5}}")
//			.bottom(h)
//			;
//		// exiting elemtns will 'blow up' as they fade out
//		exit = Mark.build()
//			.left("{{item.left - 3 - 5}}")
//			.bottom("{{item.bottom - 5}}")
//			.width("{{item.width + 10}}")
//			.height("{{item.height + 10}}")
//			;
		
		// Show data as a simple bar chart
		vis.add(MarkType.Bar)
			.def("i", -1)
			.data(data).datatype(int[].class)
			.key("{{data.hashCode()}}")
			.left("{{index*5}}")
			.height("{{data[0]}}")
			.bottom(0)
			.width(3)
			.fill("{{Fill.solid(i==index ? 0xff0000 : 0x1f27b4)}}")
			.delay("{{0.005*index}}") // determine animation delay
			.ease(Easing.Poly(2.2)) // determine animation style (easing)
			.enter(enter)
			.exit(exit)
			.mouseEnter("{{i(index).update()}}")
			.mouseExit("{{i(-1).update()}}");
		
		vis.update();
		
		// -- display stuff ---
		
		Display display = new GLDisplay();
	    display.setSize(w+20, h+20);
	    display.addScene(vis);
	    
	    // key controls
	    display.addKeyListener(new KeyAdapter() {
	    	private String id = "anim";
			public void keyPressed(KeyEvent k) {
				switch (k.getKeyCode()) {
				case KeyEvent.VK_SPACE:
					data.remove(0);
					data.add(new int[]{(int)(1 + 100*Math.random())});
					vis.update(dur).play(id);
					break;
				case KeyEvent.VK_S:
					Collections.sort(data, ArrayComparator.instance);
					vis.update(3*dur).play(id);
					break;
				case KeyEvent.VK_D:
					shuffle(data);
					vis.update(3*dur).play(id);
					break;
				case KeyEvent.VK_F:
					for (int i=0; i<data.size(); ++i) {
						int[] d = data.get(i);
						d[0] = (int)Math.max(5, Math.min(95,
								d[0] + 50*(0.5 - Math.random())));
					}
					vis.update(dur).play(id);
					break;
				}
			}
	    });
	    
	    JFrame frame = new JFrame("Animated Bar Chart");
		frame.getContentPane().add(display.asComponent());
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

}
