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
 * Test of line marks, including a variety of interpolation methods.
 */
public class LineTest {
	
	public static void main(String[] args)
	{
		List<Point2D> data0 = new ArrayList<Point2D>();
		data0.add(new Point2D.Double(100,000));
		data0.add(new Point2D.Double(125,075));
		data0.add(new Point2D.Double(200,100));
		data0.add(new Point2D.Double(125,125));
		data0.add(new Point2D.Double(100,200));
		data0.add(new Point2D.Double(075,125));
		data0.add(new Point2D.Double(000,100));
		data0.add(new Point2D.Double(075,075));
		data0.add(new Point2D.Double(100,000));
		
		Scene path = new Scene()
			.left(25).top(25)
			.width(640)
			.height(480)
			.scene();
		path.add("Line")
			.data(data0)
			.datatype(Point2D.class)
			.segmented(true)
			.left("{{data.getX()}}")
			.top("{{data.getY()}}")
			.stroke("{{Stroke.solid(8*index+1, 0x555555, 1.0-index/8.0)}}")
			.fill("{{Fill.solid(0x8888ff,0)}}")
		.scene().update();
		
		List<Point2D> data1 = new ArrayList<Point2D>();
		for (int i=0; i<2000; ++i) {
			data1.add(new Point2D.Double(i, 40 + 20*Math.sin(0.1*i)));
		}
		Scene sine = new Scene()
			.width(1000).height(500)
			.scene();
		sine.add("Line")
			.data(data1)
			.datatype(Point2D.class)
			.segmented(true)
			.left("{{index}}")
			.top(100)
			.stroke("{{Stroke.solid(data.getY(), 0.8, 0.4, data.getY()/70, data.getY()/80)}}")
		.scene().update();
		
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
		panels.add(null); panels.add(null);
		
		Scene line = new Scene()
			.data(panels)
			.width(1000).height(500)
			.left("{{5*index+50}}").top(200)
			.scene();
		line.add("Line")
			.data(data2)
			.datatype(Point2D.class)
			.interpolate("{{parent.index==0 ? Interpolate.StepBefore : Interpolate.StepAfter}}")
			.left("{{50*index}}")
			.top("{{data.getY()}}")
			.stroke("{{Stroke.solid(1, parent.index, 0, 1-parent.index, 1)}}")
		.scene().update();
		
		GLDisplay display = display(640, 480);
		display.addScene(path);
		display.addScene(sine);
		display.addScene(line);
	}
	
	public static GLDisplay display(int width, int height) {
		GLDisplay display = new GLDisplay();
		display.setSize(width, height);
		
		Frame frame = new Frame("Protovis Line Test");
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
