package edu.stanford.vis.test;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import pv.mark.Scene;
import pv.render.awt.gl.GLDisplay;

/**
 * Test of area marks including different interpolation methods.
 */
public class AreaTest {

	public static void main(String[] args)
	{		
		List<Point2D> data2 = new ArrayList<Point2D>();
		data2.add(new Point2D.Double(0, 100));
		data2.add(new Point2D.Double(1, 200));
		data2.add(new Point2D.Double(2, 225));
		data2.add(new Point2D.Double(3, 125));
		data2.add(new Point2D.Double(4, 050));
		data2.add(new Point2D.Double(5, 100));
		data2.add(new Point2D.Double(6, 200));
		data2.add(new Point2D.Double(7, 075));
		
		List<Object> panels = new ArrayList<Object>();
		panels.add(null); panels.add(null); panels.add(null);
		
		Scene line = new Scene()
			.data(panels)
			.width(1000).height(150)
			.left(50).top("{{10 + index*150}}")
			.scene();
		line.add("Area")
			.data(data2)
			.datatype(Point2D.class)
			.segmented(true)
			.interpolate("{{parent.index==0 ? Interpolate.StepBefore " +
				": parent.index==1 ? Interpolate.StepAfter : Interpolate.Linear}}")
			.left("{{50*index}}")
			.bottom(10)
			.height("{{0.5*data.getY()}}")
			.stroke("{{Stroke.solid(1, 0x888888)}}")
			.fill("{{Fill.solid(Math.min(1,parent.index), 0, Math.max(0, 1-parent.index), 0.25+0.75*index/10.0)}}")
		.scene().update();
		
		GLDisplay display = display(640, 480);
		display.addScene(line);
	}
	
	public static GLDisplay display(int width, int height) {
		GLDisplay display = new GLDisplay();
		display.setSize(width, height);
		
		Frame frame = new Frame("Protovis Area Test");
		frame.add(display);
		frame.pack();
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.setVisible(true);
		
		//Animator animator = new Animator(_canvas);
		//animator.start();
		
		return display;
	}
	
}
