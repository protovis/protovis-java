package pv.util.physics;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import pv.util.ThreadPool;

/**
 * A physical simulation involving particles, springs, and forces.
 * Useful for simulating a range of physical effects or layouts.
 */
public class Simulation {

	private Particle _ph = new Particle(), _pt = null;
	private Spring _sh = new Spring(null, null), _st = null;
	private int _pcount = 0;
	private int _scount = 0;
	
	private NBodyForce _nbody;
	private ParticleInit _init;
	private ParticleEval _eval;
	private SpringForce _spring;
	private Rectangle2D _bounds = null;
	
	/** Sets a bounding box for particles in this simulation.
	 *  Null (the default) indicates no boundaries. */
	public Rectangle2D bounds() { return _bounds; }
	public Simulation bounds(Rectangle2D b) {
		if (_bounds == b) return this;
		if (b == null) { _bounds = null; return this; }
		if (_bounds == null) { _bounds = new Rectangle2D.Float(); }
		// ensure x is left-most and y is top-most
		_bounds.setRect(
				b.getX() + (b.getWidth() < 0 ? b.getWidth() : 0),
				b.getY() + (b.getHeight() < 0 ? b.getHeight() : 0),
				(b.getWidth() < 0 ? -1 : 1) * b.getWidth(),
				(b.getHeight() < 0 ? -1 : 1) * b.getHeight());
		return this;
	}
	
	/** The gravitational force along the x-dimension. */
	public float gravityX() { return _init._gx; }
	public Simulation gravityX(float g) { _init._gx = g; return this; }
	
	/** The gravitational force along the y-dimension. */
	public float gravityY() { return _init._gy; }
	public Simulation gravityY(float g) { _init._gy = g; return this; }
	
	/**  The drag (viscosity) co-efficient. */
	public float drag() { return _init._dc; }
	public Simulation drag(float d) { _init._dc = d; return this; }
	
	/** The attraction (or repulsion if negative) between particles. */
	public float attraction() { return _nbody.gravitation(); }
	public Simulation attraction(float a) { _nbody.gravitation(a); return this; }
	
	/** The maximum range over which attractive/repulsive forces are exerted. */
	public float range() { return _nbody.maxDistance(); }
	public Simulation range(float r) { _nbody.maxDistance(r); return this; }
	
	// ------------------------------------------------------------------------
	
	/**
	 * Creates a new physics simulation.
	 */
	public Simulation()
	{
		this(0, 0, 0.1f, -10);
	}
	
	/**
	 * Creates a new physics simulation.
	 * @param gx the gravitational acceleration along the x dimension
	 * @param gy the gravitational acceleration along the y dimension
	 * @param drag the default drag (viscosity) co-efficient
	 * @param attraction the gravitational attraction (or repulsion, for
	 *  negative values) between particles.
	 */
	public Simulation(float gx, float gy, float drag, float attraction)
	{
		_nbody = new NBodyForce(attraction);
		_spring = new SpringForce();
		_init = new ParticleInit(gx, gy, drag);
		_eval = new ParticleEval();
	}
	
	// -- Init Simulation -------------------------------------------------
		
	/**
	 * Adds a new particle to the simulation.
	 * @param mass the mass (charge) of the particle
	 * @param x the particle's starting x position
	 * @param y the particle's starting y position
	 * @return the added particle
	 */
	public Particle addParticle(float mass, float x, float y)
	{
		Particle p = getParticle(mass, x, y);
		if (_pt == null) _pt = _ph;
		_pt.next = p; _pt = p;
		_pcount += 1;
		return p;
	}
	
	/**
	 * Adds a spring to the simulation
	 * @param p1 the first particle attached to the spring
	 * @param p2 the second particle attached to the spring
	 * @param restLength the rest length of the spring
	 * @param tension the tension of the spring
	 * @param damping the damping (friction) co-efficient of the spring
	 * @return the added spring
	 */
	public Spring addSpring(Particle p1, Particle p2, float restLength,
		float tension, float damping)
	{
		Spring s = getSpring(p1, p2, restLength, tension, damping);
		p1.degree++;
		p2.degree++;
		if (_st == null) _st = _sh;
		_st.next = s; _st = s;
		_scount += 1;
		return s;
	}
	
	/**
	 * Returns the particle list.
	 * @return the particle list
	 */	
	public Particle particles() {
		return _ph.next;
	}
		
	/**
	 * Returns the spring list.
	 * @return the spring list
	 */
	public Spring springs() {
		return _sh.next;
	}
	
	public void reset() {
		Particle p = _ph.next;
		_ph.next = null; _pt = null;
		for (Particle n=p.next; n!=null; p=n, n=n.next)
			reclaimParticle(p);
		reclaimParticle(p);
		_pcount = 0;
	
		Spring s = _sh.next;
		_sh.next = null; _st = null;
		for (Spring n=s.next; n!=null; s=n, n=n.next)
			reclaimSpring(s);
		reclaimSpring(s);
		_scount = 0;
	}
	
	// -- Run Simulation --------------------------------------------------
	
	/**
	 * Advance the simulation by 1 timestep.
	 */
	public void tick() { tick(1, 1); }
	
	/**
	 * Advance the simulation for the specified time interval.
	 * @param dt the time interval to step the simulation (default 1)
	 */
	public void tick(float dt, int iter)
	{
		_init._dt = dt;
		_init.dt1 = dt/2;
		_init.dt2 = dt*dt/2;
		_eval.dt1 = _init.dt1;
		_eval.bounds = (_bounds != null);
		if (_eval.bounds) {
			_eval.minX = (float)_bounds.getMinX();
			_eval.maxX = (float)_bounds.getMaxX();
			_eval.minY = (float)_bounds.getMinY();
			_eval.maxY = (float)_bounds.getMaxY();
		}

		Particle p; Spring s;
		
		// remove springs connected to dead particles
		for (Spring i = _sh; (s=i.next) != null; i=i.next) {
			if (s.die || s.p1.die || s.p2.die) {
				s.p1.degree--;
				s.p2.degree--;
				i.next = s.next;
				reclaimSpring(s);
				_scount -= 1;
				if (i.next == null) { _st = i; break; }
			}
		}
		
		// remove dead particles
		for (Particle i = _ph; (p=i.next)!=null; i=i.next) {
			if (p.die) {
				i.next = p.next;
				reclaimParticle(p);
				_pcount -= 1;
				if (i.next == null) { _pt = i; break; }
			}
		}
		
		initTasks();
		
		while (--iter >= 0) {
			// evaluate the forces
			run(_init);
			_nbody.run(this);
			run(_spring); _spring.finish(_sh.next);
			run(_eval);
		}
	}
	
	// -- Tasks and Functions -------------------------------------------------
	
	private List<ParticleTask> _ptasks = new ArrayList<ParticleTask>();
	private List<SpringTask> _stasks = new ArrayList<SpringTask>();
	
	public void initTasks() {
		int nt = ThreadPool.getThreadCount();
		// set task counts
		while (_ptasks.size() < nt) { _ptasks.add(new ParticleTask()); }
		for (int i=_ptasks.size(); --i >= nt; ) { _ptasks.remove(i); }
		while (_stasks.size() < nt) { _stasks.add(new SpringTask()); }
		for (int i=_stasks.size(); --i >= nt; ) { _stasks.remove(i); }
		
		// init particle tasks
		int pstep = (int)Math.ceil(_pcount / (double)nt);
		Particle p = _ph.next;
		for (int i=0, j=0; i<_pcount; ++i) {
			if (i % pstep == 0) {
				_ptasks.get(j).start = p;
				if (j > 0) _ptasks.get(j-1).end = p;
				j += 1;
			}
			p = p.next;
		}
		_ptasks.get(_ptasks.size()-1).end = null;
		
		// init spring tasks
		int sstep = (int)Math.ceil(_scount / (double)nt);
		Spring s = _sh.next;
		for (int i=0, j=0; i<_scount; ++i) {
			if (i % sstep == 0) {
				_stasks.get(j).start = s;
				if (j > 0) _stasks.get(j-1).end = s;
				j += 1;
			}
			s = s.next;
		}
		_stasks.get(_stasks.size()-1).end = null;
	}
	
	public void run(ParticleFunction pf) {
		for (int i=0; i<_ptasks.size(); ++i)
			_ptasks.get(i).function = pf;
		try {
			ThreadPool.getThreadPool().invokeAll(_ptasks);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void run(SpringFunction sf) {
		for (int i=0; i<_stasks.size(); ++i)
			_stasks.get(i).function = sf;
		try {
			ThreadPool.getThreadPool().invokeAll(_stasks);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static interface ParticleFunction {
		void run(Particle start, Particle end);
	}
	
	public static interface SpringFunction {
		void run(Spring start, Spring end);
	}
	
	public static class ParticleTask implements Callable<ParticleTask> {
		public ParticleFunction function;
		public Particle start, end;
		
		public ParticleTask call() throws Exception {
			function.run(start, end);
			return this;
		}
	}
	
	public static class SpringTask implements Callable<SpringTask> {
		public SpringFunction function;
		public Spring start, end;
		
		public SpringTask call() throws Exception {
			function.run(start, end);
			return this;
		}
	}
	
	public static class ParticleInit implements ParticleFunction {
		public float _dt, _dc, _gx, _gy;
		public float dt1, dt2;
		
		public ParticleInit(float gx, float gy, float drag) {
			_gx = gx; _gy = gy; _dc = drag;
		}
		
		public void run(Particle start, Particle end)
		{	
			float ax, ay;
				
			for (Particle p = start; p!=end; p=p.next) {
				// update particle age
				p.age += _dt;
				
				// update particles using Verlet integration
				if (p.fixed) {
					p.vx = p.vy = 0;
				} else {
					ax = p.fx / p.mass; ay = p.fy / p.mass;
					p.x  += p.vx*_dt + ax*dt2;
					p.y  += p.vy*_dt + ay*dt2;
					p._vx = p.vx + ax*dt1;
					p._vy = p.vy + ay*dt1;
				}
				
				// apply gravity force and drag force
				p.fx = (_gx * p.mass) - (_dc * p.vx);
				p.fy = (_gy * p.mass) - (_dc * p.vy);
			}
		}
	}
	
	public static class ParticleEval implements ParticleFunction {
		float dt1, minX, minY, maxX, maxY;
		boolean bounds;
		
		public void run(Particle start, Particle end)
		{
			for (Particle p = start; p!=end; p=p.next) {
				if (!p.fixed) {
					float ax = dt1 / p.mass;
					p.vx = p._vx + p.fx * ax;
					p.vy = p._vy + p.fy * ax;
				}
				if (bounds) {
					if (p.x < minX) {
						p.x = minX; p.vx = 0;
					} else if (p.x > maxX) {
						p.x = maxX; p.vx = 0;
					}
					if (p.y < minY) {
						p.y = minY; p.vy = 0;
					}
					else if (p.y > maxY) {
						p.y = maxY; p.vy = 0;
					}
				}
			}
		}
	}
	
	public static class SpringForce implements SpringFunction {
		public void run(Spring start, Spring end) {
			Particle p1, p2;
			float dx, dy, dn, dd, k;
			
			for (Spring s = start; s!=end; s=s.next) {
				p1 = s.p1;
				p2 = s.p2;				
				dx = p1.x - p2.x;
				dy = p1.y - p2.y;
				
				dn = dx*dx + dy*dy;
				if (dn > 0) {
					float half = 0.5f*dn;
					int i = Float.floatToIntBits(dn);
					i = 0x5f3759df - (i>>1);
					dn = Float.intBitsToFloat(i);
					dn = dn*(1.5f - half*dn*dn);
					dd = dn;
					dn = 1/dn;
				} else {
					dd = 1;
				}
				
				k  = s.tension * (dn - s.restLength);
				k += s.damping * (dx*(p1.vx-p2.vx) + dy*(p1.vy-p2.vy)) * dd;
				k *= dd;
				
				// provide a random direction when needed
				if (dn==0) {
					dx = 0.01f * (float)(0.5-Math.random());
					dy = 0.01f * (float)(0.5-Math.random());
				}
				s.fx = -k * dx;
				s.fy = -k * dy;	
			}
		}
		
		public void finish(Spring s) {
			for (; s!=null; s=s.next) {
				s.p1.fx += s.fx; s.p1.fy += s.fy;
				s.p2.fx -= s.fx; s.p2.fy -= s.fy;
			}
		}
	}
	
	// -- Particle Pool ---------------------------------------------------
	
	/** The maximum number of items stored in a simulation object pool. */
	public static int objectPoolLimit = 5000;
	protected static ArrayList<Particle> _ppool = new ArrayList<Particle>();
	protected static ArrayList<Spring> _spool = new ArrayList<Spring>();
	
	/**
	 * Returns a particle instance, pulling a recycled particle from the
	 * object pool if available.
	 * @param mass the mass (charge) of the particle
	 * @param x the particle's starting x position
	 * @param y the particle's starting y position
	 * @return a particle instance
	 */
	protected static Particle getParticle(float mass, float x, float y)
	{
		int len = _ppool.size();
		if (len > 0) {
			Particle p = _ppool.remove(len-1);
			p.init(mass, x, y, 0, 0, false);
			return p;
		} else {
			return new Particle(mass, x, y, 0, 0, false);
		}
	}
	
	/**
	 * Returns a spring instance, pulling a recycled spring from the
	 * object pool if available.
	 * @param p1 the first particle attached to the spring
	 * @param p2 the second particle attached to the spring
	 * @param restLength the rest length of the spring
	 * @param tension the tension of the spring
	 * @param damping the damping (friction) co-efficient of the spring
	 * @return a spring instance
	 */
	protected static Spring getSpring(Particle p1, Particle p2,
		float restLength, float tension, float damping)
	{
		int len = _spool.size();
		if (len > 0) {
			Spring s = _spool.remove(len-1);
			s.init(p1, p2, restLength, tension, damping);
			return s;
		} else {
			return new Spring(p1, p2, restLength, tension, damping);
		}
	}
	
	/**
	 * Reclaims a particle, adding it to the object pool for recycling
	 * @param p the particle to reclaim
	 */
	protected static void reclaimParticle(Particle p)
	{
		if (_ppool.size() < objectPoolLimit) {
			_ppool.add(p);
		}
	}
	
	/**
	 * Reclaims a spring, adding it to the object pool for recycling
	 * @param s the spring to reclaim
	 */
	protected static void reclaimSpring(Spring s)
	{
		if (_spool.size() < objectPoolLimit) {
			_spool.add(s);
		}
	}
	
}