package edu.stanford.vis.examples;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import pv.layout.ForceDirectedLayout;
import pv.mark.MarkEvent;
import pv.mark.Scene;
import pv.mark.Variable;
import pv.mark.constants.MarkType;
import pv.mark.eval.EventHandler;
import pv.render.Display;
import pv.render.awt.gl.GLDisplay;
import pv.scene.Item;
import pv.style.Fill;
import pv.style.Stroke;

/**
 * Example demonstrating how to break up a graph into small multiples
 * based on a simple predicate (in this case, a "year" value), but
 * still use the full graph to create a consistent layout across
 * each small multiple view.
 * 
 * This example also demonstrates the use of linked, animated
 * highlighting of elements across small multiple views.
 */
public class GraphPartition {

	public static void main(String[] args)
	{		
		// generate graph data
		GraphData gd = GraphData.generate(7);
		
		// create a force-directed layout with 300 x 300 bounds
		// the bounds ensure the multiple stay within their layer
		final ForceDirectedLayout fdl = new ForceDirectedLayout();
		fdl.defaultSpringLength(250);
		fdl.bounds(new Rectangle2D.Double(0, 0, 300, 300));
		
		// a variable to hold the currently selected datum
		final Variable active = new Variable(null);
		
		// build the visualization
		final Scene root = new Scene();
		
		// first, create a layer for each multiple
		root.add(MarkType.Panel)
			.data(gd.years()) // one multiple per year
			.datatype(Integer.class)
			.width(300)
			.height(300)
			.left("{{20 + index * 400}}") // horizontally offset layers
			.top(20)
			.sceneUpdate(fdl.update()) // run layout on update
			
		// next add the nodes as dots
		   .add(MarkType.Dot)
		   	.def("gd", gd)              // include graph data source
		   	.def("active", active)      // holds active (mouse-hovered) datum
		   	.data("{{gd.nodes(data)}}") // retrieves nodes for current year
		   	.datatype(GNode.class)
		   	.extend(fdl.node())         // include nodes within the layout
		   	.shape("circle").size(100)
		   	.stroke("{{Stroke.solid(1, data==active ? 0xff0000 : 0x3366aa)}}")
		   	// interaction to highlight matching nodes on mouse over
		   	// use the datum (not the item) to ensure linking across views
		   	.mouseEnter(new EventHandler() {
				public void handle(MarkEvent event, Item item) {
					active.value(item.data);
					// create a named 0.5 second animation. by naming it, we
					// cancel any previous animations with the same name
					root.update(0.5).play("highlight");
				}
			})
		   	.mouseExit(new EventHandler() {
				public void handle(MarkEvent event, Item item) {
					active.value(null);
					root.update(0.5).play("highlight");
				}
			})
			
		// now add the graph edges
		   .add(MarkType.Link)
		   	.def("gd", gd)                // include graph data source
		   	.data("{{gd.edges(data)}}")
		   	.datatype(GEdge.class)
		   	.extend(fdl.edge())           // include edges in the layout
		   	.nodeKey("{{data.label}}")    // index nodes by label field
		   	.sourceKey("{{data.source}}") // index for source nodes
		   	.targetKey("{{data.target}}") // index for target nodes
		   	.stroke(Stroke.solid(1, 0xcccccc))
		   	.top(0).left(0)               // zero-out top and left
		   	
		// finally, add text labels to nodes
		   .proto().add(MarkType.Label)
		    .def("gd", gd) // include data source for inherited properties
		    .stroke(Stroke.none)
		    .fill(Fill.solid(0))
		    .textAlign("center").textBaseline("middle")
		    .text("{{String.valueOf(data.label)}}")
		   ;
		
		// update (and run layout) on every frame
		root.repeatedUpdate(true);
		
		// create display and show the visualization
		Display d = display();
		d.addScene(root);
		d.render();
	}
	
	public static Display display() {
		Display display = new GLDisplay();
		display.setSize(1150, 340);
		
		Frame frame = new Frame("Protovis Graph Partition");
		frame.add(display.asComponent());
		frame.pack();
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.setVisible(true);
		
		return display;
	}
	
	// provides graph data partitioned by year
	public static class GraphData {
		
		private List<Integer> years = Arrays.asList(2007,2008,2009);
		private List<GNode> nodes;
		private List<GEdge> edges;
		
		public List<Integer> years() { return years; }
		
		public List<GNode> nodes() { return nodes; }
		public List<GNode> nodes(int year) {
			ArrayList<GNode> a = new ArrayList<GNode>();
			for (GNode n : nodes) {
				if (n.year0 <= year && n.year1 >= year)
					a.add(n);
			}
			return a;
		}
		
		public List<GEdge> edges() { return edges; }
		public List<GEdge> edges(int year) {
			ArrayList<GEdge> a = new ArrayList<GEdge>();
			for (GEdge e : edges) {
				if (e.year == year)
					a.add(e);
			}
			return a;
		}
		
		public static GraphData generate(int N) {
			GraphData gd = new GraphData();
			
			List<GNode> nodes = new ArrayList<GNode>();
			for (int i=0; i<N; ++i) {
				nodes.add(new GNode(i, 2007, 2009));
			}
			
			Random r = new Random(1234L);
			List<GEdge> edges = new ArrayList<GEdge>();
			for (int i=0; i<N; ++i) {
				for (int j=i+1; j<N; ++j) {
					GEdge e = new GEdge(i, j, 2007 + r.nextInt(3));
					edges.add(e);
				}
			}
			
			gd.nodes = nodes;
			gd.edges = edges;
			
			return gd;
		}
		
	}
	
	// simple node defined over a year range
	public static class GNode {
		public final int year0;
		public final int year1;
		public final int label;
		
		public GNode(int label, int y0, int y1) {
			this.label = label;
			this.year0 = y0;
			this.year1 = y1;
		}
	}
	
	// simple edge defined for a single year
	public static class GEdge {
		public final int source;
		public final int target;
		public final int year;
		
		public GEdge(int source, int target, int year) {
			this.source = source;
			this.target = target;
			this.year = year;
		}
	}

}
