package pv.animate;

import pv.style.Easing;

public class Transition implements Scheduler.Task
{
	protected String _id = null;
	protected long _startTime = Long.MAX_VALUE;
	protected long _stepTime;
	protected long _endTime;
	protected long _pause = 1000/60;
	protected double _duration;
	protected Easing _ease = new Easing.Polynomial(2.2f);
	
	public String id() { return _id; }
	public Transition id(String id) { _id = id; return this; }
	
	public long step(double dt, double dd, Easing e) {
		return 0;
	}
	
	public void add(Transition t) {
		throw new UnsupportedOperationException();
	}
	
	public void play() {
		_startTime = System.currentTimeMillis();
		_endTime = _startTime + (long)(1000 * _duration);
		Scheduler.instance().add(this);
	}
	
	public void play(String id) {
		id(id);
		play();
	}
	
	public void stop() {
		_startTime = -1;
	}
	
	public long evaluate(long t0)
	{
		if (_startTime == Long.MAX_VALUE) play();
		if (_startTime < 0) return 0;
		if (t0 - _stepTime < _pause) return (t0 - _stepTime); 
		
		// compute timing parameters
		_stepTime = t0;
		double dt = t0 - _startTime, dd = _endTime - _startTime;
		long next = step(dt, dd, _ease); 
		if (next < 0) _startTime = -1;
		return next;
	}
	
}
