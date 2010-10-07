package pv.util.physics;

public class Particle {

	/** The mass (or charge) of the particle. */
	public float mass;
	/** The number of springs (degree) attached to this particle. */
	public int degree;
	/** The x position of the particle. */
	public float x;
	/** The y position of the particle. */
	public float y;
	/** The x velocity of the particle. */
	public float vx;
	/** A temporary x velocity variable. */
	public float _vx;
	/** The y velocity of the particle. */
	public float vy;
	/** A temporary y velocity variable. */
	public float _vy;
	/** The x force exerted on the particle. */
	public float fx;
	/** The y force exerted on the particle. */
	public float fy;
	/** The age of the particle in simulation ticks. */
	public float age;
	/** Flag indicating if the particle should have a fixed position. */
	public boolean fixed;
	/** Flag indicating that the particle is scheduled for removal. */
	public boolean die;
	/** Tag property for storing an arbitrary value. */
	public int tag;
	/** Optional object corresponding to this particle. */
	public Object payload = null;
	
	public Particle next = null;
	
	/**
	 * Creates a new Particle with given parameters.
	 * @param mass the mass (or charge) of the particle
	 * @param x the x position of the particle
	 * @param y the y position of the particle
	 * @param vx the x velocity of the particle
	 * @param vy the y velocity of the particle
	 * @param fixed flag indicating if the particle should have a 
	 *  fixed position
	 */
	public Particle(float mass, float x, float y,
		float vx, float vy, boolean fixed)
	{
		init(mass, x, y, vx, vy, fixed);
	}
	
	/**
	 * Creates a new Particle with given parameters.
	 */
	public Particle()
	{
		init(1, 0, 0, 0, 0, true);
	}
	
	/**
	 * Initializes an existing particle instance.
	 * @param mass the mass (or charge) of the particle
	 * @param x the x position of the particle
	 * @param y the y position of the particle
	 * @param vx the x velocity of the particle
	 * @param vy the y velocity of the particle
	 * @param fixed flag indicating if the particle should have a 
	 *  fixed position
	 */
	public void init(float mass, float x, float y,
		float vx, float vy, boolean fixed)
	{
		this.mass = mass;
		this.degree = 0;
		this.x = x;
		this.y = y;
		this.vx = this._vx = vx;
		this.vy = this._vy = vy;
		this.fx = 0;
		this.fy = 0;
		this.age = 0;
		this.fixed = fixed;
		this.die = false;
		this.tag = 0;
		this.next = null;
	}
	
	/**
	 * "Kills" this particle, scheduling it for removal in the next
	 * simulation cycle.
	 */
	public void kill() {
		this.die = true;
	}
	
}
