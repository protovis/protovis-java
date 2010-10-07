package pv.animate;

import java.util.ArrayList;
import java.util.List;

import pv.style.Easing;

public class Parallel extends Transition {

	public List<Transition> _trans = new ArrayList<Transition>();
	
	public Parallel(double duration) {
		_duration = duration;
	}
	
	@Override
	public void add(Transition t) {
		if (t == null) return;
		_trans.add(t);
	}
	
	public long step(double dt, double dd, Easing e)
	{		
		long next = -1;
		for (Transition t : _trans) {
			long time = t.step(dt, dd, e); 
			if (time > 0)
				next = (next > 0 ? Math.min(next, time) : time);
		}
		return next;
	}

}
