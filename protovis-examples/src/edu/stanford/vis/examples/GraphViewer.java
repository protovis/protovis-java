package edu.stanford.vis.examples;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JFrame;

import pv.layout.ForceDirectedLayout;
import pv.mark.Mark;
import pv.mark.Scene;
import pv.mark.constants.MarkType;
import pv.mark.constants.Shape;
import pv.render.Display;
import pv.render.awt.CameraControl;
import pv.render.awt.gl.GLDisplay;
import pv.style.Stroke;
import pv.util.ThreadPool;
import pv.util.physics.Simulation;

/**
 * Sample graph visualization example.
 * Visualizes a synthetic graph using a force-directed layout
 */
public class GraphViewer {

	public static void main(String[] argv)
	{
		System.out.println("Click and drag to pan the view.");
		System.out.println("Hold the Shift key and click and drag vertically to zoom.");
		
		// The N parameter below changes the size of the graph.
		// You can adjust this to test scalability to larger graphs.
		// Note that as N rises, we are penalized quadratically due to the
		// V*E betweenness-centrality calculation. If one disables this, the
		// bottleneck usually becomes the N log N n-body calculation performed
		// by the force-directed layout.
		
		// first, generate synthetic graph data
		int N = 2000, k = 20, graphType = 1;
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
		final ForceDirectedLayout fdl = new ForceDirectedLayout(
				new Simulation(0, 0, 0.1f, -2));
		fdl.iterations(2);
		
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
			.size("{{16}}")// + 1000*bc.centrality(data)/maxbc}}")
			.stroke(Stroke.solid(1, 0x0021be))
			//.fill(Fill.solid(0x0021be))
		// add edges, "inherits" the nodes from its prototype 
		   .add(MarkType.Link)
		    .data(edges)
		    .datatype(int[].class)
		    .extend(keys)
		    .extend(fdl.edge())
		   	.stroke(Stroke.solid(1, 0xcccccc, 0.1)) // very faint
		   	;
		
		final Display display = new GLDisplay();
	    display.setSize(700, 700);
	    display.addScene(vis.scene());
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
		
	    JFrame frame = new JFrame("Graph Viewer");
		frame.getContentPane().add(display.asComponent());
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}
