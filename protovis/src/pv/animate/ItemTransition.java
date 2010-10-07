package pv.animate;

import java.util.List;

import pv.scene.Item;
import pv.style.Easing;

public class ItemTransition extends Transition {

	protected Item x, a, b;
	protected List<Animator> interp;
	
	public ItemTransition()
	{
	}
	
	public ItemTransition(Item item, Item prev, Item next, List<Animator> animators)
	{
		this.x = item;
		this.a = prev;
		this.b = next;
		this.interp = animators;
	}
	
	public long step(double dt, double dd, Easing e) {
		return interpolate(x, a, b, dt, dd, e);
	}
	
	protected long interpolate(Item x, Item a, Item b, double dt, double dd, Easing e)
	{
		// get delay and easing, calculate interpolation fraction
		double delay = 1000*x.delay;
		float f = (float)((dt-delay)/dd);
		if (f <= 0) {
			f = 0;
		} else if (f >= 1) {
			f = 1;
		} else {
			Easing ease = x.ease==null ? e : x.ease;
			if (ease != null)
				f = ease.ease(f);
		}
		
		// interpolate item properties
		int num = interp.size();
		for (int j=0; j<num; ++j) {
			interp.get(j).step(f, x, a, b);
		}
		boolean last = (dt-delay > dd);
		if (last && x.zombie()) x.dead(true);
		return last ? -1 : _pause;
	}
	
}
