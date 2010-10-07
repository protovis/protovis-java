package edu.stanford.vis.test;

import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import pv.mark.Scene;
import pv.mark.constants.MarkType;
import pv.mark.constants.Shape;
import pv.render.awt.gl.GLDisplay;
import pv.style.Fill;
import pv.util.ThreadPool;

/**
 * Simple animation test for a field of dots.
 * Hit the space bar to animate.
 * Hit the "t" key to cycle through the number of threads to use.
 */
public class AnimationTest {

	static int N = 100000;
	static long t0;
	
	public static long tick() {
		return (t0 = System.currentTimeMillis());
	}
	public static float tock() {
		return (System.currentTimeMillis() - t0) / 1000f;
	}
	
	public static void main(String[] args)
	{
		System.out.println("Hit the space bar to randomly animate points.");
		
		// create data
		ArrayList<Point2D> data = new ArrayList<Point2D>();
		for (int i=0; i<N; ++i) {
			data.add(new Point2D.Double(Math.random(), Math.random()));
		}
		
		final Scene dots = new Scene()
			.width(900)
			.height(400)
			.left(50)
			.top(50)
			.scene();
		
		dots.add(MarkType.Dot)
			.datatype(Point2D.class)
			.data(data)
			.left("{{(int)(900 * (data.getX() + 0.05*(0.5 - Math.random())))}}")
			.bottom("{{(int)(400 * (data.getY() + 0.05*(0.5 - Math.random())))}}")
			.shape(Shape.Point)
			//.size("{{(index%20)*(index%20) + 16}}")
			.fill(Fill.solid(0xaa5555, 1))
			.set("radius", "{{1}}")
			;
		
		dots.update();
		
		final GLDisplay display = display();
		display.addKeyListener(new KeyAdapter() {	
			public void keyPressed(KeyEvent k) {
				switch (k.getKeyCode()) {
				case KeyEvent.VK_SPACE:
					dots.update(4).play("dots-transition");
					break;
				case KeyEvent.VK_T:
					int nt = 1 + (ThreadPool.getThreadCount() % 4);
					System.out.println("NUM THREADS: "+nt);
					ThreadPool.setThreadCount(nt);
					break;
				}
			}			
		});
		
		display.addScene(dots);
	}
	
	public static GLDisplay display() {
		GLDisplay display = new GLDisplay();
		display.setSize(1000, 500);
		
		Frame frame = new Frame("Protovis Animation Test");
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
