package pv.util.physics;

/**
 * Represents a Spring in a physics simulation. A spring connects two
 * particles and is defined by the springs rest length, spring tension,
 * and damping (friction) co-efficient.
 */
public class Spring {

	/** The first particle attached to the spring. */
	public Particle p1;
	/** The second particle attached to the spring. */
	public Particle p2;
	/** The rest length of the spring. */
	public float restLength;
	/** The tension of the spring. */
	public float tension;
	/** The damping (friction) co-efficient of the spring. */
	public float damping;
	/** The x force exerted by the spring. */
	public float fx;
	/** The y force exerted by the spring. */
	public float fy;
	/** Flag indicating that the spring is scheduled for removal. */
	public boolean die;
	/** Tag property for storing an arbitrary value. */
	public int tag;
	/** Optional object corresponding to this spring. */
	public Object payload = null;
	
	public Spring next = null;
	
	/**
	 * Creates a new Spring with given parameters.
	 * @param p1 the first particle attached to the spring
	 * @param p2 the second particle attached to the spring
	 * @param restLength the rest length of the spring
	 * @param tension the tension of the spring
	 * @param damping the damping (friction) co-efficient of the spring
	 */
	public Spring(Particle p1, Particle p2, float restLength,
			float tension, float damping)
	{
		init(p1, p2, restLength, tension, damping);
	}
	
	/**
	 * Creates a new Spring with default parameters.
	 * @param p1 the first particle attached to the spring
	 * @param p2 the second particle attached to the spring
	 */
	public Spring(Particle p1, Particle p2)
	{
		init(p1, p2, 10, 0.1f, 0.1f);
	}
	
	/**
	 * Initializes an existing spring instance.
	 * @param p1 the first particle attached to the spring
	 * @param p2 the second particle attached to the spring
	 * @param restLength the rest length of the spring
	 * @param tension the tension of the spring
	 * @param damping the damping (friction) co-efficient of the spring
	 */
	public void init(Particle p1, Particle p2, float restLength,
			float tension, float damping)
	{
		this.p1 = p1;
		this.p2 = p2;
		this.restLength = restLength;
		this.tension = tension;
		this.damping = damping;
		this.die = false;
		this.tag = 0;
		this.next = null;
	}
	
	/**
	 * "Kills" this spring, scheduling it for removal in the next
	 * simulation cycle.
	 */
	public void kill() {
		this.die = true;
	}
	
}
