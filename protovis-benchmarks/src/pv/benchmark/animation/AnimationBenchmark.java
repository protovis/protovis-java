package pv.benchmark.animation;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import pv.animate.Transition;
import pv.benchmark.Benchmark;
import pv.benchmark.BenchmarkRunner;
import pv.benchmark.SceneBenchmark;
import pv.mark.Scene;
import pv.mark.constants.MarkType;
import pv.mark.constants.Shape;
import pv.render.awt.gl.GLDisplay;
import pv.style.Fill;

public class AnimationBenchmark {

	static int N = 5000; // vary over runs: 5000, 50000, 500000
	private static GLDisplay display;
	private static Scene dots;
	
	public static void main(String[] args)
	{
		display = display();
		
		// create data
		ArrayList<Point2D> data = new ArrayList<Point2D>();
		for (int i=0; i<N; ++i) {
			data.add(new Point2D.Double(Math.random(), Math.random()));
		}
		dots = new Scene()
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
			.size("{{(index%20)*(index%20) + 16}}")
			.fill(Fill.solid(0xaa5555, 1));
		dots.update();
		
		BenchmarkRunner.instance.iterations(101);
		BenchmarkRunner.instance.run("ANIM", benchmark(dots));
	}
	
	public static GLDisplay display() {
		GLDisplay display = new GLDisplay();
		display.setSize(1000, 500);
		
		Frame frame = new Frame("Protovis Animation Benchmark");
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
	
	public static Benchmark benchmark(Scene scene) {
		return new SceneBenchmark(display, "DOTS", scene) {
			
			public long[] run(int iterations) {
				long[] t = new long[1000 * iterations];
				TimedTransition tt = new TimedTransition(t);
				
				for (int i=0; i<iterations; ++i) {
					tt.trans = dots.update(4);
					tt.play();
					synchronized (tt) {
						try {
							tt.wait();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				
				long[] s = new long[tt.i];
				System.arraycopy(t, 0, s, 0, tt.i);
				return s;
			}
		};
	}
	
	public static class TimedTransition extends Transition {
		public Transition trans;
		public long[] times;
		public int i = 0;
		
		public TimedTransition(long[] times) {
			this.times = times;
		}
		
		@Override
		public long evaluate(long t0) {
			long t = System.currentTimeMillis();
			long r = trans.evaluate(t0);
			times[i++] = System.currentTimeMillis() - t;
			if (r <= 0) {
				synchronized (this) {
					this.notifyAll();
				}
			}
			return r;
		}
	}
}
