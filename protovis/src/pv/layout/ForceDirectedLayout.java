package pv.layout;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;

import pv.mark.Mark;
import pv.mark.MarkEvent;
import pv.mark.eval.EventHandler;
import pv.mark.property.AbstractProperty;
import pv.scene.Item;
import pv.scene.LinkItem;
import pv.util.physics.Particle;
import pv.util.physics.Simulation;
import pv.util.physics.Spring;

public class ForceDirectedLayout {

	private Simulation _sim;
	private int _step = 1;
	private int _iter = 1;
	private int _gen = 0;
	private boolean _key = true;
	private boolean _enabled = true;
	private Rectangle2D _bounds = null;
	
	// simulation defaults
	protected float _mass = 1;
	protected float _restLength = 10;
	protected float _tension = 0.01f;
	protected float _damping = 0.1f;
	
	/** The default mass value for node/particles. */
	public float defaultParticleMass() { return _mass; }
	public void defaultParticleMass(float v) { _mass = v; }
	
	/** The default spring rest length for edge/springs. */
	public float defaultSpringLength() { return _restLength; }
	public void defaultSpringLength(float v) { _restLength = v; }
	
	/** The default spring tension for edge/springs. */
	public float defaultSpringTension() { return _tension; }
	public void defaultSpringTension(float v) { _tension = v; }
	
	/** The number of iterations to run the simulation per invocation
	 *  (default is 1, expecting continuous updates). */
	public int iterations() { return _iter; }
	public void iterations(int iter) { _iter = iter; }
	
	/** The number of time ticks to advance the simulation on each
	 *  iteration (default is 1). */
	public int ticksPerIteration() { return _step; }
	public void ticksPerIteration(int ticks) { _step = ticks; }
	
	/** The physics simulation driving this layout. */
	public Simulation simulation() { return _sim; }
	
	/** Indicates if item data (true) or the item itself (false) should
	 *  be used as a lookup key for simulation items. */
	public boolean useDataAsKey() { return _key; }
	public void useDataAsKey(boolean b) { _key = b; }
	
	/** Enables and disables simulation runs. */
	public boolean enabled() { return _enabled; }
	public void enabled(boolean b) { _enabled = b; }
	
	/** Layout boundaries, null for no bounds. */
	public Rectangle2D bounds() { return _bounds; }
	public void bounds(Rectangle2D b) { _bounds = b; }
	
	private Set<Iterable<Item>> _nodes = new HashSet<Iterable<Item>>();
	private Set<Iterable<Item>> _edges = new HashSet<Iterable<Item>>();
	private Iterable<Item> _niter = Iterables.concat(_nodes);
	private Iterable<Item> _eiter = Iterables.concat(_edges);
	
	// --------------------------------------------------------------------
	
	/**
	 * Creates a new ForceDirectedLayout.
	 */
	public ForceDirectedLayout()
	{
		this(null);
	}
	
	/**
	 * Creates a new ForceDirectedLayout.
	 * @param sim the physics simulation to use for the layout. If null
	 *  (the default), default simulation settings will be used
	 */
	public ForceDirectedLayout(Simulation sim)
	{
		_sim = (sim==null ? new Simulation(0, 0, 0.1f, -10) : sim);
	}
	
	// -- simulation management -------------------------------------------
	
	private Map<Object,Particle> _pmap = new HashMap<Object,Particle>();
	private Map<Object,Spring> _smap = new HashMap<Object,Spring>();
	
	public void clear() {
		_nodes.clear();
		_edges.clear();
		reset();
	}
	
	public ForceDirectedLayout nodes(Iterable<Item> nodes) {
		_nodes.add(nodes);
		return this;
	}
	
	public ForceDirectedLayout edges(Iterable<Item> edges) {
		_edges.add(edges);
		return this;
	}
	
	private Particle particle(Item item) {
		Object key = _key ? item.data : item;
		return _pmap.get(key);
	}
	
	private void particle(Item item, Particle p) {
		Object key = _key ? item.data : item;
		if (p == null) {
			_pmap.remove(key);
		} else {
			_pmap.put(key, p);
			p.payload = item;
		}
	}
	
	private Spring spring(LinkItem item) {
		Object key = _key ? item.data : item;
		return _smap.get(key);
	}
	
	private void spring(LinkItem item, Spring s) {
		Object key = _key ? item.data : item;
		if (s == null) {
			_smap.remove(key);
		} else {
			_smap.put(key, s);
			s.payload = item;
		}
	}
	
	// -- run simulation ------------------------------------------------------
	
	float tt = 0;
	int ii = 0;
	
	public void step()
	{
		if (!_enabled) return;
		
		++_gen; // update generation counter
		init(); // populate simulation
		
		// run simulation
		_sim.bounds(_bounds != null ? _bounds : null);
		
		//long t0 = System.currentTimeMillis();
		_sim.tick(_step, _iter);
		//float t1 = (System.currentTimeMillis()-t0)/1000f;
		//tt += t1;
		//ii += 1;
		//System.out.println(t1+"\t"+(tt/ii));
	}
	
	public void reset()
	{
		_pmap.clear();
		_smap.clear();
		_sim.reset();
	}
	
	/**
	 * Initializes the Simulation for this ForceDirectedLayout
	 */
	protected void init()
	{
		// TODO this method can be a bottleneck
		// find way to cull redundant computation
		
		// initialize all simulation entries
		for (Item n : _niter) {
			if (!n.visible) continue;
			Particle p = particle(n);
			if (p == null) {
				p = _sim.addParticle(_mass,
					(float)(n.left + 2*(Math.random() - 0.5)),
					(float)(n.top + 2*(Math.random() - 0.5)));
				particle(n, p);
				//p.fixed = o.fixed;
			} else {
				p.x = (float)n.left;
				p.y = (float)n.top;
				//p.fixed = o.fixed;
			}
			p.tag = _gen;
		}
		for (Item item : _eiter) {
			if (!item.visible) continue;
			LinkItem e = (LinkItem) item;
			if (!e.source.visible || !e.target.visible) continue;
			Spring s = spring(e);
			if (s == null) {
				Particle p1 = particle(e.source);
				Particle p2 = particle(e.target);
				s = _sim.addSpring(p1, p2, _restLength, _tension, _damping);
				spring(e, s);
			}
			s.tag = _gen;
		}
		
		// set up simulation parameters
		// this needs to be kept separate from the above initialization
		// to ensure all simulation items are created first
		for (Item n : _niter) {
			Particle p = particle(n);
			if (p == null)
				continue;
			p.mass = mass(n, p);
		}
		for (Item item : _eiter) {
			LinkItem e = (LinkItem) item;
			Spring s = spring(e);
			if (s == null)
				continue;
			s.restLength = restLength(e, s);
			s.tension = tension(e, s);
			s.damping = damping(e, s);
		}
		
		for (Particle p = _sim.particles(); p!=null; p=p.next) {
			if (p.tag != _gen) {
				p.kill();
				particle((Item)p.payload, null);
			}
		}
		for (Spring s = _sim.springs(); s!=null; s=s.next) {
			if (s.tag != _gen) {
				s.kill();
				spring((LinkItem)s.payload, null);
			}
		}
	}
	
	/**
	 * Function for assigning mass values to particles. By default, this
	 * simply returns the default mass value. This function can be replaced
	 * to perform custom mass assignment.
	 */
	protected float mass(Item n, Particle p) { return _mass; }
	
	/**
	 * Function for assigning rest length values to springs. By default,
	 * this simply returns the default rest length value. This function can
	 * be replaced to perform custom rest length assignment.
	 */
	protected float restLength(LinkItem e, Spring s) { return _restLength; }
	
	/**
	 * Function for assigning tension values to springs. By default, this
	 * method computes spring tension adaptively, based on the connectivity
	 * of the attached particles, to create more stable layouts. More
	 * specifically, the tension is computed as the default tension value
	 * divided by the square root of the maximum degree of the attached
	 * particles. This function can be replaced to perform custom tension
	 * assignment.
	 */
	protected float tension(LinkItem e, Spring s) {
		double n = Math.max(s.p1.degree, s.p2.degree);
		return (float)(_tension / Math.sqrt(n));
	}
	
	/**
	 * Function for assigning damping constant values to springs. By
	 * default, this simply uses the spring's computed tension value
	 * divided by the default damping. This function can be replaced
	 * to perform custom damping assignment.
	 */
	protected float damping(LinkItem e, Spring s) {
		return s.tension * _damping;
	}
	
	// ------------------------------------------------------------------------
	
	public Mark node() {
		return Mark.create()
			.left(new Left())
			.top(new Top())
			.onBuild(new AddNodes());
	}
	
	public Mark edge() {
		return Mark.create()
			.onBuild(new AddEdges());
	}
	
	protected final Update updater = new Update();
	
	public EventHandler update() {
		return updater;
	}
	
	protected class Update implements EventHandler {
		public void handle(MarkEvent event, Item item) {
			step();
		}
	}
	
	protected class AddNodes implements EventHandler {
		public void handle(MarkEvent event, Item item) {
			nodes(item.items());
		}
	}
	
	protected class AddEdges implements EventHandler {
		public void handle(MarkEvent event, Item item) {
			edges(item.items());
		}
	}
	
	protected class Left extends AbstractProperty {
		public Left() { super(double.class); }
		public double number(Item item) {
			Particle p = particle(item);
			return p==null ? item.left : p.x;
		}
	}
	
	protected class Top extends AbstractProperty {
		public Top() { super(double.class); }
		public double number(Item item) {
			Particle p = particle(item);
			return p==null ? item.top : p.y;
		}
	}
	
}
