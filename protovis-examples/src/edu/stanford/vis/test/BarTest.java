package edu.stanford.vis.test;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pv.mark.Scene;
import pv.render.awt.gl.GLDisplay;
import pv.style.Stroke;

/**
 * Test of bar marks including stacking, variable visibility, and
 * multiple panels.
 */
public class BarTest {
	
	public static void main(String[] args) {
		List<List<Point2D>> stacks = new ArrayList<List<Point2D>>();
		for (int i=0; i<200; ++i) {
			List<Point2D> stack = new ArrayList<Point2D>();
			for (int j=0; j<80; ++j) {
				stack.add(new Point2D.Double(1,3));
			}
			stacks.add(stack);
		}
		
		Scene bars = new Scene()
			.data(Arrays.asList(null, null))
			.left("{{640 * index}}")
			.height(480)
			.scene();
		bars.add("Panel")
			.data(stacks)
			.width(640)
			.height(480)
		   .add("Bar")
			.datatype(Point2D.class)
			.visible("{{(parent.index%2)==0}}")
			.bottom("{{cousin==null ? 0 : cousin.bottom + cousin.height}}")
			.left("{{item.index(2)==0 ? parent.width - (8*(1+index)) : 8*index}}")
			.width(8)
			.height("{{data.getY() + index}}")
			.stroke(Stroke.solid(1, 0x555555))
			.fill("{{Fill.solid(item.index(2)==0 ? 0x8888ff : 0xff8888, index/80.0)}}")
			;
		
		bars.update();
		
		GLDisplay display = display(2*640, 480);
		display.addScene(bars);
	}
	
	public static GLDisplay display(int width, int height) {
		GLDisplay display = new GLDisplay();
		display.setSize(width, height);
		
		Frame frame = new Frame("Protovis Bar Test");
		frame.add(display);
		frame.pack();
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.setVisible(true);
		
		return display;
	}
	
}
