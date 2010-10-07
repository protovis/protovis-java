package edu.stanford.vis.examples;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;

import pv.mark.Mark;
import pv.mark.Scene;
import pv.mark.Variable;
import pv.mark.constants.MarkType;
import pv.render.Display;
import pv.render.awt.CameraControl;
import pv.render.awt.gl.GLDisplay;
import pv.style.Font;
import pv.style.Stroke;

/**
 * This class uses a simple scatter plot to demonstrate animated scale changes.
 * By maintaining both a current and previous scale, protovis can smoothly
 * animated changes to axis gridlines and labels. Tap the space bar to
 * transition between axis scale settings.
 */
public class ScatterPlot {

	public static void main(String[] argv)
	{
		System.out.println("Hit the space bar to animate a scale change.");
		
		final int N = 2000, w = 500;
		
		// generate random data points
		List<Point2D> data = new ArrayList<Point2D>();
		for (int i=0; i<N; ++i) {
			data.add(new Point2D.Double(Math.random(), Math.random()));
		}
		
		// use variables to hold the axis scale settings
		final Variable a = new Variable(w); // the current scale
		final Variable b = new Variable(w); // the previous scale
		
		// the visual scene
		final Scene vis = new Scene()
			.left(20.51).top(20.5)
			.width(w).height(w)
			.scene();
		
		// use previous scale to set positions for animated entry
		Mark enter1 = Mark.create().bottom("{{(int)(b * data)}}");
		Mark enter2 = Mark.create().left("{{(int)(b * data)}}");
		
		// vertical gridlines, x-axis labels
		final Mark rule = vis.add(MarkType.Rule)
			.def("a", a).def("b", b)
			.data(Arrays.asList(0.2, 0.4, 0.6, 0.8))
			.datatype(Double.class)
			.key("{{data}}")
			.bottom("{{(int)(a * data)}}")
			.stroke(Stroke.solid(1, 0xcccccc))
			.enter(enter1);
		rule.add(MarkType.Label)
		    .def("a", a).def("b", b)
			.textAlign("right")
		    .textBaseline("middle")
		   	.font(Font.font("Arial", 11))
		   	.enter(enter1);
		
		// horizontal gridlines, y-axis labels
		rule.add(MarkType.Rule)
			.def("a", a).def("b", b)
		   	.left("{{(int)(a * data)}}")
		   	.bottom(0)
		   	.enter(enter2)
		   .add(MarkType.Label)
		    .def("a", a).def("b", b)
		    .textAlign("center")
		    .font(Font.font("Arial", 11))
		    .enter(enter2);
		
		// data frame
		vis.add(MarkType.Rule).left(0);
		vis.add(MarkType.Rule).bottom(0);
		vis.add(MarkType.Rule).right(0);
		vis.add(MarkType.Rule).top(0);
		
		// scatter plot points
		vis.add(MarkType.Dot)
			.def("a", a).def("b", b)
			.data(data).datatype(Point2D.class)
			.left("{{a * data.getX()}}")
			.bottom("{{a * data.getY()}}")
			.stroke(Stroke.solid(1, 0x3267ae, 0.3));
		
		Display d = new GLDisplay();
		d.setSize(w+40, w+40);
		d.addScene(vis);
		new CameraControl().attach(d);
		
		d.addKeyListener(new KeyAdapter() {
			boolean state = true;
			public void keyPressed(KeyEvent evt) {
				switch (evt.getKeyCode()) {
				case KeyEvent.VK_SPACE:
					// We first force an update to flush out any previous item
					// instances. In practice, one should not do this; protovis
					// could otherwise reuse existing items. We do it here to
					// test the animated entry of newly created items. If we
					// did not, faded items may still be resident in memory;
					// by calling update we ensure they are removed.
					// Note that this also has the side-effect that tapping
					// the space bar rapidly results in "jumps" in the
					// animation. Removing the forced update will enable
					// smooth updates on repeated taps.
					//vis.updateAndWait();
					
					b.value(a.value()); // set previous scale
					state = !state;
					
					// update tick data and current scale value
					if (state) {
						rule.data(Arrays.asList(0.2, 0.4, 0.6, 0.8));
						a.value(w);
					} else {
						rule.data(Arrays.asList(0.5, 1.0, 1.5));
						a.value(w/2);
					}
					// show change in 1-second animation
					vis.update(1).play("grid");
					break;
				}
			}
		});
		
		// create and show application window
		JFrame f = new JFrame("Plot");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().add(d.asComponent());
		f.pack();
		f.setVisible(true);
		
		// update, wait for completion, then play a 1-second animation
		vis.updateAndWait(1).play();
	}
	
}
