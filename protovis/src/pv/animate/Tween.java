package pv.animate;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import pv.style.Easing;

public class Tween extends Transition {

	private Object _target;
	private Map<String,Interpolator<?>> _interp
		= new HashMap<String,Interpolator<?>>();
	
	public Tween(double duration, Object target) {
		super._duration = duration;
		_target = target;
	}
	
	public Tween set(String name, Object end) {
		return set(name, get(_target, name), end);
	}
	
	public Tween set(String name, Object start, Object end) {
		Interpolator<?> i = Interpolator.Factory.get(start, end);
		_interp.put(name, i);
		return this;
	}
	
	public void remove(String name) {
		_interp.remove(name);
	}
	
	public long step(double dt, double dd, Easing e) {
		float f = (float)(dt/dd);
		if (f <= 0) {
			f = 0;
		} else if (f >= 1) {
			f = 1;
		} else {
			Easing ease = _ease==null ? e : _ease;
			if (ease != null)
				f = ease.ease(f);
		}
		
		for (Map.Entry<String,Interpolator<?>> k : _interp.entrySet()) {
			set(_target, k.getKey(), k.getValue().step(f));
		}
		return (dt > dd) ? -1 : _pause;
	}
	
	public static Object get(Object target, String name) {
		try {
			Field f = target.getClass().getField(name);
			return f.get(target);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void set(Object target, String name, Object value) {
		try {
			Field f = target.getClass().getField(name);
			f.set(target, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
