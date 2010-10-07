package pv.animate;

import java.util.Date;

import pv.style.Fill;
import pv.style.Font;
import pv.style.Stroke;

public interface Interpolator<T> {

	T step(float f);
	
	public static class Factory {
		public static Interpolator<?> get(Object a, Object b) {
			// TODO: make this cleaner and extensible
			Object o = (b != null ? b : a);
			if (o instanceof Integer) {
				return new IntInterpolator(a, b);
			} else if (o instanceof Long) {
				return new LongInterpolator(a, b);
			} else if (o instanceof Float) {
				return new FloatInterpolator(a, b);
			} else if (o instanceof Number) {
				return new DoubleInterpolator(a, b);
			} else if (o instanceof Date) {
				return new DateInterpolator(a, b);
			} else if (o instanceof Fill) {
				return new FillInterpolator(a, b);
			} else if (o instanceof Stroke) {
				return new StrokeInterpolator(a, b);
			} else if (o instanceof Font) {
				return new FontInterpolator(a, b);
			}
			return new ObjectInterpolator(a, b);
		}
	}
	
	public static class ObjectInterpolator implements Interpolator<Object> {
		public Object a, b;
		public ObjectInterpolator(Object a, Object b) {
			this.a = a;
			this.b = b;
		}
		public Object step(float f) {
			return f < 0.5f ? a : b;
		}
	}
	
	public static class IntInterpolator implements Interpolator<Integer> {
		public int a, b;
		public IntInterpolator(Object a, Object b) {
			this.a = (Integer)a;
			this.b = (Integer)b;
		}
		public Integer step(float f) {
			return a + ((int)(f*(b-a)));
		}
	}
	
	public static class LongInterpolator implements Interpolator<Long> {
		public long a, b;
		public LongInterpolator(Object a, Object b) {
			this.a = (Long)a;
			this.b = (Long)b;
		}
		public Long step(float f) {
			return a + ((long)(((double)f)*(b-a)));
		}
	}
	
	public static class FloatInterpolator implements Interpolator<Float> {
		public float a, b;
		public FloatInterpolator(Object a, Object b) {
			this.a = (Float)a;
			this.b = (Float)b;
		}
		public Float step(float f) {
			return a + f*(b-a);
		}
	}
	
	public static class DoubleInterpolator implements Interpolator<Double> {
		public double a, b;
		public DoubleInterpolator(Object a, Object b) {
			this.a = (Double)a;
			this.b = (Double)b;
		}
		public Double step(float f) {
			return a + f*(b-a);
		}
	}
	
	public static class DateInterpolator implements Interpolator<Date> {
		public Date a, b;
		public DateInterpolator(Object a, Object b) {
			this.a = (Date)a;
			this.b = (Date)b;
		}
		public Date step(float f) {
			long ta = a.getTime(), tb = b.getTime();
			return new Date(ta + (long)(((double)f)*(tb-ta)));
		}
	}
	
	public static class FillInterpolator implements Interpolator<Fill> {
		public Fill a, b;
		public FillInterpolator(Object a, Object b) {
			this.a = (Fill)a;
			this.b = (Fill)b;
		}
		public Fill step(float f) {
			return Fill.interpolate(f, a, b);
		}
	}
	
	public static class StrokeInterpolator implements Interpolator<Stroke> {
		public Stroke a, b;
		public StrokeInterpolator(Object a, Object b) {
			this.a = (Stroke)a;
			this.b = (Stroke)b;
		}
		public Stroke step(float f) {
			return Stroke.interpolate(f, a, b);
		}
	}
	
	public static class FontInterpolator implements Interpolator<Font> {
		public Font a, b;
		public FontInterpolator(Object a, Object b) {
			this.a = (Font)a;
			this.b = (Font)b;
		}
		public Font step(float f) {
			return Font.interpolate(f, a, b);
		}
	}
}
