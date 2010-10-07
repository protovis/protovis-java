package pv.benchmark.graph;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.opengl.GLAutoDrawable;
import javax.swing.JFrame;

import pv.benchmark.render.GLRendererP;
import pv.layout.ForceDirectedLayout;
import pv.mark.Mark;
import pv.mark.Scene;
import pv.mark.constants.MarkType;
import pv.mark.constants.Shape;
import pv.render.awt.CameraControl;
import pv.render.awt.gl.GLDisplay;
import pv.render.awt.gl.GLRenderer;
import pv.style.Stroke;
import pv.util.ThreadPool;
import pv.util.physics.Simulation;

/**
 * Sample graph visualization example.
 * Visualizes a synthetic graph using a force-directed layout
 */
public class GraphViewer {

	static int N = 100, k = 10, graphType = 1, numThreads = 1;
	
	static int _n = 0, _t = 0;
	static int[] nodes = new int[] { 100, 1000, 10000, 100000 };
	static int[] threads = new int[] { 6 };//1, 2, 3, 4, 5, 6 };
	
	static long delay = 1000;
	static Timer timer = new Timer();
	static PrintWriter out;
	
	private static class PDisplay extends GLDisplay {
		private static final long serialVersionUID = -5204253651992453979L;
		public void setRenderer(GLRenderer r) {
			_renderer = r;
		}
	}
	
	public static void main(String[] argv) {
		try {
			out = new PrintWriter(new BufferedWriter(
					new FileWriter("graph.benchmark.txt")));
		} catch (Exception e) {
			e.printStackTrace();
		}
		numThreads = threads[_t];
		run();
	}
	
	public static void step() {
		if (_n == nodes.length-1) {
			if (_t == threads.length-1) {
				out.flush(); out.close();
				System.exit(0);
			} else {
				_t++; _n = 0;
			}
		} else {
			_n++;
		}
		numThreads = threads[_t];
		N = nodes[_n];
		run();
	}
	
	public static void run()
	{
		ThreadPool.setThreadCount(numThreads);
		
		// first, generate synthetic graph data
		ArrayList<Integer> nodes = new ArrayList<Integer>();
		for (int i=0; i<N; ++i) nodes.add(i);
		
		ArrayList<int[]> edges = new ArrayList<int[]>();
		
		if (graphType == 0) {
			// make a simple star graph
			for (int i=0, j=1; j<N; ++j) {
				edges.add(new int[] {i,j});
			}
		} else if (graphType == 1) {
			// make a chain of nodes with local connectivity 'k'
			for (int i=0; i<N-1; ++i) {
				for (int j=i+1; j<i+k && j<N; ++j) {
					edges.add(new int[] {i,j});
				}
			}
		} else if (graphType == 2) {
			// make a grid of nodes
			int m = (int)Math.sqrt(N), n = N / m;
	        for (int i=0; i<m*n; ++i) {
	            if (i >= n) {
	                edges.add(new int[] {i-n, i});
	            }
	            if (i % n != 0) {
	            	edges.add(new int[] {i-1, i});
	            }
	        }
		}
		
		// the force directed layout
		final Simulation sim = new Simulation(0, 0, 0.1f, -2);
		final ForceDirectedLayout fdl = new ForceDirectedLayout(sim);
		fdl.iterations(1); fdl.ticksPerIteration(1);
		
		// key properties for node-edge lookup
		Mark keys = Mark.create()
			.nodeKey("{{item.index}}")
			.sourceKey("{{data[0]}}")
			.targetKey("{{data[1]}}");
		
		// the root element of the visualization
		final Scene vis = new Scene()
			.sceneUpdate(fdl.update())
			.width(100).height(100)
			.scene();
		
		// add nodes, sized by betweenness centrality
		vis.add(MarkType.Dot)
		   	.data(nodes)
			.extend(fdl.node())
			.shape(Shape.Circle)
			.size("{{16}}")
			.stroke(Stroke.solid(1, 0x0021be))
		// add edges, "inherits" the nodes from its prototype 
		   .add(MarkType.Link)
		    .data(edges)
		    .datatype(int[].class)
		    .extend(keys)
		    .extend(fdl.edge())
		   	.stroke(Stroke.solid(1, 0xcccccc, 0.1)) // very faint
		   	;
		
		final JFrame frame = new JFrame("Trial "+numThreads+"("+N+")");
		
		final PDisplay display = new PDisplay()
		{
			private static final long serialVersionUID = -1749583214087340169L;
			private long t0;
        	private float _frameTime;
        	private int _fpi = 0, count = 0;
        	public void display(GLAutoDrawable glDrawable) {
				float t1 = (System.currentTimeMillis() - t0) / 1000f;
				_frameTime += t1;
				_fpi = (_fpi + 1);
				if (_fpi+1 == 5) {
					float fps = _fpi / _frameTime;
					out.println((count++) + "\t" + fps + "\t" + numThreads + "\t" + N);
					_fpi = 0; _frameTime = 0;
				}
				if (count > 20) {
					out.flush();
					vis.repeatedUpdate(false);
					frame.setVisible(false);
					frame.dispose();
					timer.schedule(new TimerTask() {
						public void run() { step(); this.cancel(); }
					}, delay);
				}
				t0 = System.currentTimeMillis();
				
				super.display(glDrawable);
			}
		};
		display.setRenderer(GLRendererP.instance());
	    display.setSize(700, 700);
	    display.addScene(vis.scene());
	    display.pan(350, 350);
	    display.zoom(0.1, 350, 350);
	    new CameraControl().attach(display);
		
	    display.addKeyListener(new KeyAdapter() {
	    	public void keyPressed(KeyEvent e) {
	    		switch (e.getKeyCode()) {
	    		case KeyEvent.VK_L:
	    			fdl.enabled(!fdl.enabled());
	    			break;
	    		case KeyEvent.VK_T:
	    			int nt = 1 + (ThreadPool.getThreadCount() % 4);
	    			ThreadPool.setThreadCount(nt);
	    			System.out.println("Thread Count: "+nt);
	    			break;
	    		case KeyEvent.VK_I:
	    			int it = (int)Math.pow(5,
	    				1 + (int)(Math.log(fdl.iterations()) / Math.log(5)));
	    			if (it > 500) it = 1;
	    			System.out.println("Iterations Per Frame: "+it);
	    			fdl.iterations(it);
	    			break;
	    		case KeyEvent.VK_A:
	    			fdl.simulation().attraction(-1 * fdl.simulation().attraction());
	    			System.out.println("Attraction: "+fdl.simulation().attraction());
	    			break;
	    		}
	    	}
		});
	    
		vis.repeatedUpdate(true);
		frame.getContentPane().add(display.asComponent());
		frame.pack();
		frame.setVisible(true);
	}
}
