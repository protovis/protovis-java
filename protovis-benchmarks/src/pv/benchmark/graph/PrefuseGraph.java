package pv.benchmark.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.ShapeAction;
import prefuse.action.layout.RandomLayout;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.activity.Activity;
import prefuse.controls.DragControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.io.GraphMLReader;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.ShapeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.GraphicsLib;
import prefuse.util.display.DisplayLib;
import prefuse.util.display.ItemBoundsListener;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualItem;

/**
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class PrefuseGraph extends JPanel {

	static int N = 100000, k = 10, graphType = 1;
	
	private static final long serialVersionUID = 7351986863776461695L;

	private static final String graph = "graph";
    private static final String nodes = "graph.nodes";
    private static final String edges = "graph.edges";

    private Visualization m_vis;
    
    public static Graph buildGraph(List<Integer> nodes, List<int[]> edges) {
    	Graph g = new Graph();
    	Map<Integer,Node> map = new HashMap<Integer,Node>();
    	for (Integer u : nodes) {
    		map.put(u, g.addNode());
    	}
    	for (int[] e : edges) {
    		Node u = map.get(e[0]);
    		Node v = map.get(e[1]);
    		g.addEdge(u, v);
    	}
    	return g;
    }
    
    public PrefuseGraph(List<Integer> nodeList, List<int[]> edgeList) {
    	this(buildGraph(nodeList, edgeList));
    }
    
    public PrefuseGraph(Graph g) {
        super(new BorderLayout());
    	
        // create a new, empty visualization for our data
        m_vis = new Visualization();
        
        // --------------------------------------------------------------------
        // set up the renderers
        
        ShapeRenderer sr = new ShapeRenderer();
        m_vis.setRendererFactory(new DefaultRendererFactory(sr));

        // --------------------------------------------------------------------
        // register the data with a visualization
        
        // adds graph to visualization and sets renderer label field
        setGraph(g);
        
        // fix selected focus nodes
//        TupleSet focusGroup = m_vis.getGroup(Visualization.FOCUS_ITEMS); 
//        focusGroup.addTupleSetListener(new TupleSetListener() {
//            public void tupleSetChanged(TupleSet ts, Tuple[] add, Tuple[] rem)
//            {
//                for ( int i=0; i<rem.length; ++i )
//                    ((VisualItem)rem[i]).setFixed(false);
//                for ( int i=0; i<add.length; ++i ) {
//                    ((VisualItem)add[i]).setFixed(false);
//                    ((VisualItem)add[i]).setFixed(true);
//                }
//                if ( ts.getTupleCount() == 0 ) {
//                    ts.addTuple(rem[0]);
//                    ((VisualItem)rem[0]).setFixed(false);
//                }
//                m_vis.run("draw");
//            }
//        });
        
        
        
        // --------------------------------------------------------------------
        // create actions to process the visual data

        //int hops = 30;
        //final GraphDistanceFilter filter = new GraphDistanceFilter(graph, hops);

        //ColorAction fill = new ColorAction(nodes, 
        //        VisualItem.FILLCOLOR, ColorLib.rgb(200,200,255));
        //fill.add(VisualItem.FIXED, ColorLib.rgb(255,100,100));
        //fill.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255,200,125));
        
        ActionList draw = new ActionList();
        //draw.add(filter);
        draw.add(new RandomLayout(nodes));
        draw.add(new ShapeAction(nodes, Constants.SHAPE_ELLIPSE));
        draw.add(new ColorAction(nodes, VisualItem.FILLCOLOR, 0));
        draw.add(new ColorAction(nodes, VisualItem.STROKECOLOR, 0xff3456aa));
        //draw.add(new ColorAction(nodes, VisualItem.TEXTCOLOR, ColorLib.rgb(0,0,0)));
        draw.add(new ColorAction(edges, VisualItem.FILLCOLOR, ColorLib.gray(200)));
        draw.add(new ColorAction(edges, VisualItem.STROKECOLOR, ColorLib.gray(200)));
        
        ActionList animate = new ActionList(Activity.INFINITY);
        animate.add(new ForceDirectedLayout(graph));
        animate.add(new Action() {
        	private long t0;
        	private float _frameTime;
        	private int _fpi = 0, count = 0;
			public void run(double f) {
				this.getVisualization().getDisplay(0).damageReport();
				float t1 = (System.currentTimeMillis() - t0) / 1000f;
				_frameTime += t1;
				_fpi = (_fpi + 1);
				if (_fpi+1 == 5) {
					float fps = _fpi / _frameTime;
					System.out.println((count++) + "\t" + fps);
					_fpi = 0; _frameTime = 0;
				}
				if (count > 100) System.exit(0);
				t0 = System.currentTimeMillis();
			}
        });
        animate.add(new RepaintAction());
        
        // finally, we register our ActionList with the Visualization.
        // we can later execute our Actions by invoking a method on our
        // Visualization, using the name we've chosen below.
        m_vis.putAction("draw", draw);
        m_vis.putAction("layout", animate);

        m_vis.runAfter("draw", "layout");
        
        
        // --------------------------------------------------------------------
        // set up a display to show the visualization
        
        Display display = new Display(m_vis);
        display.setSize(700,700);
        display.pan(350, 350);
        display.zoom(new Point2D.Double(0,0), 0.1);
        display.setForeground(Color.GRAY);
        display.setBackground(Color.WHITE);
        display.setHighQuality(true);
        
        // main display controls
        //display.addControlListener(new FocusControl(1));
        display.addControlListener(new DragControl());
        display.addControlListener(new PanControl());
        display.addControlListener(new ZoomControl());
        display.addControlListener(new WheelZoomControl());
        display.addControlListener(new ZoomToFitControl());
        //display.addControlListener(new NeighborHighlightControl());

        // overview display
//        Display overview = new Display(vis);
//        overview.setSize(290,290);
//        overview.addItemBoundsListener(new FitOverviewListener());
        
        display.setForeground(Color.GRAY);
        display.setBackground(Color.WHITE);
        
        // --------------------------------------------------------------------        
        // launch the visualization
        
//        // create a panel for editing force values
//        ForceSimulator fsim = ((ForceDirectedLayout)animate.get(0)).getForceSimulator();
//        JForcePanel fpanel = new JForcePanel(fsim);
//        
//        // create a new JSplitPane to present the interface
//        JSplitPane split = new JSplitPane();
//        split.setLeftComponent(display);
//        split.setRightComponent(fpanel);
//        split.setOneTouchExpandable(true);
//        split.setContinuousLayout(false);
//        split.setDividerLocation(700);
        
        // now we run our action list
        m_vis.run("draw");
        
        add(display);
    }
    
    public void setGraph(Graph g) {
        // update graph
        m_vis.removeGroup(graph);
        m_vis.addGraph(graph, g);
        m_vis.setValue(edges, null, VisualItem.INTERACTIVE, Boolean.FALSE);
    }
    
    // ------------------------------------------------------------------------
    // Main and demo methods
    
    public static Graph getGraph() {
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
		return buildGraph(nodes, edges);
    }
    
    public static void main(String[] args) {
        UILib.setPlatformLookAndFeel();
        
        // create graphview
        String datafile = null;
        if ( args.length > 0 ) {
            datafile = args[0];
        }
        
        JFrame frame = demo(datafile);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    public static JFrame demo(String datafile) {
        Graph g = null;
        if ( datafile == null ) {
            g = getGraph();
        } else {
            try {
                g = new GraphMLReader().readGraph(datafile);
            } catch ( Exception e ) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return demo(g);
    }
    
    public static JFrame demo(Graph g) {
        final PrefuseGraph view = new PrefuseGraph(g);
        
        // launch window
        JFrame frame = new JFrame("prefuse graph");
        frame.setContentPane(view);
        frame.pack();
        frame.setVisible(true);
        
        frame.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                view.m_vis.run("layout");
            }
            public void windowDeactivated(WindowEvent e) {
                view.m_vis.cancel("layout");
            }
        });
        
        return frame;
    }
    
    
    // ------------------------------------------------------------------------
    
    public static class FitOverviewListener implements ItemBoundsListener {
        private Rectangle2D m_bounds = new Rectangle2D.Double();
        private Rectangle2D m_temp = new Rectangle2D.Double();
        private double m_d = 15;
        public void itemBoundsChanged(Display d) {
            d.getItemBounds(m_temp);
            GraphicsLib.expand(m_temp, 25/d.getScale());
            
            double dd = m_d/d.getScale();
            double xd = Math.abs(m_temp.getMinX()-m_bounds.getMinX());
            double yd = Math.abs(m_temp.getMinY()-m_bounds.getMinY());
            double wd = Math.abs(m_temp.getWidth()-m_bounds.getWidth());
            double hd = Math.abs(m_temp.getHeight()-m_bounds.getHeight());
            if ( xd>dd || yd>dd || wd>dd || hd>dd ) {
                m_bounds.setFrame(m_temp);
                DisplayLib.fitViewToBounds(d, m_bounds, 0);
            }
        }
    }
    
} // end of class GraphView
