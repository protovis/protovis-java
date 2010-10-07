package pv.util.physics;

import java.util.ArrayList;

public class NBodyForce {

	private float _g;     // gravitational constant
	private float _t;     // barnes-hut theta
	private float _max, _max2;   // max effective distance
	private float _min, _min2;   // min effective distance
	private float _eps;   // epsilon for determining 'same' location
	
	private float _x1, _y1, _x2, _y2;
	private QuadTreeNode _root;
	
	/** The gravitational constant to use. 
	 *  Negative values produce a repulsive force. */
	public float gravitation() { return _g; }
	public void gravitation(float g) { _g = g; }
	
	/** The maximum distance over which forces are exerted. 
	 *  Any greater distances will be ignored. */
	public float maxDistance() { return _max; }
	public void maxDistance(float d) { _max = d; _max2 = d*d; }
	
	/** The minumum effective distance over which forces are exerted.
	 * 	Any lesser distances will be treated as the minimum. */
	public float minDistance() { return _min; }
	public void minDistance(float d) { _min = d; _min2 = d*d; }
	
	private Simulation.ParticleFunction forces = new Simulation.ParticleFunction() {
		public void run(Particle start, Particle end) {
			for (Particle p = start; p != end; p=p.next) {
				forces(p, _root, _x1, _y1, _x2, _y2);
			}
		}
	};
	
	// --------------------------------------------------------------------
	
	/**
	 * Creates a new NBodyForce with given parameters.
	 * @param g the gravitational constant to use.
	 *  Negative values produce a repulsive force.
	 */
	public NBodyForce(float g) {
		this(g, 500, 2, 0.01f, 0.9f);
	}
	
	/**
	 * Creates a new NBodyForce with given parameters.
	 * @param g the gravitational constant to use.
	 *  Negative values produce a repulsive force.
	 * @param maxd a maximum distance over which the force should operate.
	 *  Particles separated by more than this distance will not interact.
	 * @param mind the minimum distance over which the force should operate.
	 *  Particles closer than this distance will interact as if they were
	 *  the minimum distance apart. This helps avoid extreme forces.
	 *  Helpful when particles are very close together.
	 * @param eps an epsilon values for determining a minimum distance
	 *  between particles
	 * @param t the theta parameter for the Barnes-Hut approximation.
	 *  Determines the level of approximation (default value if 0.9).
	 */
	public NBodyForce(float g, float max, float min, float eps, float t)
	{
		_g = g;
		maxDistance(max);
		minDistance(min);
		_eps = eps;
		_t = t;
		_root = QuadTreeNode.node();
	}
	
	/**
	 * Applies this force to a simulation.
	 * @param sim the Simulation to apply the force to
	 */
	public void run(Simulation sim)
	{
		if (_g == 0) return;
		
		// clear the quadtree
		clear(_root); _root = QuadTreeNode.node();
		
		// get the tree bounds
		bounds(sim);
    
    	// populate the tree
		for (Particle p = sim.particles(); p!=null; p=p.next) {
    		insert(p, _root, _x1, _y1, _x2, _y2);
    	}	
    	
    	// traverse tree to compute mass
    	accumulate(_root);
    	
    	// calculate forces on each particle
    	sim.run(forces);
	}
	
	private void accumulate(QuadTreeNode n) {
		float xc=0, yc=0;
		n.mass = 0;
		
		// accumulate childrens' mass
		if (n.hasChildren) {
			QuadTreeNode c;
			if ((c=n.c1) != null) {
				accumulate(c);
				n.mass += c.mass;
				xc += c.mass * c.cx;
				yc += c.mass * c.cy;
			}
			if ((c=n.c2) != null) {
				accumulate(c);
				n.mass += c.mass;
				xc += c.mass * c.cx;
				yc += c.mass * c.cy;
			}
			if ((c=n.c3) != null) {
				accumulate(c);
				n.mass += c.mass;
				xc += c.mass * c.cx;
				yc += c.mass * c.cy;
			}
			if ((c=n.c4) != null) {
				accumulate(c);
				n.mass += c.mass;
				xc += c.mass * c.cx;
				yc += c.mass * c.cy;
			}
		}
		
		// accumulate own mass
		if (n.p != null) {
			n.mass += n.p.mass;
			xc += n.p.mass * n.p.x;
			yc += n.p.mass * n.p.y;
		}
		n.cx = xc / n.mass;
		n.cy = yc / n.mass;
	}
	
	private void forces(Particle p, QuadTreeNode n,
		float x1, float y1, float x2, float y2)
	{
		float f = 0;
		float dx = n.cx - p.x;
		float dy = n.cy - p.y;
		float dd = dx*dx + dy*dy, dn;
		
		// fast inverse square root
		float half = 0.5f*dd;
		int i = Float.floatToIntBits(dd);
		i = 0x5f3759df - (i>>1);
		dn = Float.intBitsToFloat(i);
		dn = dn*(1.5f - half*dn*dn);
		
		boolean max = _max > 0 && dd > _max2;
		if (dd==0) { // add direction when needed
			dx = _eps * (float)(0.5-Math.random());
			dy = _eps * (float)(0.5-Math.random());
		}
		
		// the Barnes-Hut approximation criteria is if the ratio of the
    	// size of the quadtree box to the distance between the point and
    	// the box's center of mass is beneath some threshold theta.
    	if ( (!n.hasChildren && n.p != p) || ((x2-x1)*dn < _t) )
    	{
        	if ( max ) return;
        	// either only 1 particle or we meet criteria
        	// for Barnes-Hut approximation, so calc force
        	dn = dd<_min2 ? (1/_min) : dn;
        	f = _g * p.mass * n.mass * (dn*dn*dn);
        	p.fx += f*dx; p.fy += f*dy;
    	}
    	else if ( n.hasChildren )
    	{
        	// recurse for more accurate calculation
        	float sx = (x1+x2)/2f;
        	float sy = (y1+y2)/2f;
        	
        	if (n.c1 != null) forces(p, n.c1, x1, y1, sx, sy);
			if (n.c2 != null) forces(p, n.c2, sx, y1, x2, sy);
			if (n.c3 != null) forces(p, n.c3, x1, sy, sx, y2);
			if (n.c4 != null) forces(p, n.c4, sx, sy, x2, y2);

        	if ( max ) return;
        	if ( n.p != null && n.p != p ) {
        		dn = dd<_min2 ? (1/_min) : dn;
            	f = _g * p.mass * n.p.mass * (dn*dn*dn);
            	p.fx += f*dx; p.fy += f*dy;
        	}
		}
	}
			
	// -- Helpers ---------------------------------------------------------
	
	private void insert(Particle p, QuadTreeNode n,
		float x1, float y1, float x2, float y2)
	{
		// ignore particles with NaN coordinates
		if (Float.isNaN(p.x) || Float.isNaN(p.y)) return;
		
		// try to insert particle p at node n in the quadtree
    	// by construction, each leaf will contain either 1 or 0 particles
    	if ( n.hasChildren ) {
        	// n contains more than 1 particle
        	insertHelper(p,n,x1,y1,x2,y2);
    	} else if ( n.p != null ) {
        	// n contains 1 particle
        	if ( isSameLocation(n.p, p) ) {
        		// recurse
            	insertHelper(p,n,x1,y1,x2,y2);
        	} else {
        		// divide
        		Particle v = n.p; n.p = null;
            	insertHelper(v,n,x1,y1,x2,y2);
            	insertHelper(p,n,x1,y1,x2,y2);
        	}
    	} else { 
        	// n is empty, add p as leaf
        	n.p = p;
    	}
	}
	
	private void insertHelper(Particle p, QuadTreeNode n,
		float x1, float y1, float x2, float y2)
	{
		// determine split
		float sx = (x1+x2)/2f;
		float sy = (y1+y2)/2f;
		int c = (p.x >= sx ? 1 : 0) + (p.y >= sy ? 2 : 0);
		
		// update bounds
		if (c==1 || c==3) x1 = sx; else x2 = sx;
		if (c>1) y1 = sy; else y2 = sy;
		
		// update children
		QuadTreeNode cn;
		if (c == 0) {
			if (n.c1==null) n.c1 = QuadTreeNode.node();
			cn = n.c1;
		} else if (c == 1) {
			if (n.c2==null) n.c2 = QuadTreeNode.node();
			cn = n.c2;
		} else if (c == 2) {
			if (n.c3==null) n.c3 = QuadTreeNode.node();
			cn = n.c3;
		} else {
			if (n.c4==null) n.c4 = QuadTreeNode.node();
			cn = n.c4;
		}
		n.hasChildren = true;
		insert(p,cn,x1,y1,x2,y2);
	}
	
	private void clear(QuadTreeNode n)
	{
		if (n.c1 != null) clear(n.c1);
		if (n.c2 != null) clear(n.c2);
		if (n.c3 != null) clear(n.c3);
		if (n.c4 != null) clear(n.c4);
		QuadTreeNode.reclaim(n);
	}
	
	private void bounds(Simulation sim)
	{
		_x1 = _y1 = Float.MAX_VALUE;
		_x2 = _y2 = Float.MIN_VALUE;

		// get bounding box
		for (Particle p = sim.particles(); p!=null; p=p.next) {
			if (p.x < _x1) _x1 = p.x;
			if (p.y < _y1) _y1 = p.y;
			if (p.x > _x2) _x2 = p.x;
			if (p.y > _y2) _y2 = p.y;
		}
		
		// square the box
		float dx = _x2 - _x1;
		float dy = _y2 - _y1;
		if (dx > dy) {
			_y2 = _y1 + dx;
		} else {
			_x2 = _x1 + dy;
		}
	}
	
	private boolean isSameLocation(Particle p1, Particle p2) {
    	return (Math.abs(p1.x - p2.x) < _eps && 
    			Math.abs(p1.y - p2.y) < _eps);
	}
	
	// -- Helper QuadTreeNode class -------------------------------------------
	
	static class QuadTreeNode
	{
		public float mass = 0;
		public float cx = 0;
		public float cy = 0;
		public Particle p = null;
		public QuadTreeNode c1 = null;
		public QuadTreeNode c2 = null;
		public QuadTreeNode c3 = null;
		public QuadTreeNode c4 = null;
		public boolean hasChildren = false;
		
		// -- Factory ---------------------------------------------------------
		
		private static ArrayList<QuadTreeNode> _nodes = new ArrayList<QuadTreeNode>();
		
		public static QuadTreeNode node() {
			QuadTreeNode n; int len = _nodes.size();
			if (len > 0) {
				n = _nodes.remove(len-1);
			} else {
				n = new QuadTreeNode();
			}
			return n;
		}
		
		public static void reclaim(QuadTreeNode  n) {
			n.mass = n.cx = n.cy = 0;
			n.p = null;
			n.hasChildren = false;
			n.c1 = n.c2 = n.c3 = n.c4 = null;
			_nodes.add(n);
		}
	}

}