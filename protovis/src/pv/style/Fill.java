package pv.style;

import pv.util.IntObjectHashMap;

public abstract class Fill {

	public static final Fill none = null;
	
	public abstract String key();
	protected abstract Fill interp(float f, Fill b);
	
	public static Fill fill(String spec) {
		throw new UnsupportedOperationException();
	}
	
	public static Fill solid(int color) {
		return solid(color, 1);
	}
	
	public static Fill solid(int color, double alpha) {
		int a = alpha >= 1 ? 255 : alpha <= 0 ? 0 : (int)(alpha*255 + 0.5);
		color = ((a & 0xFF) << 24) | (0x00FFFFFF & color);
		return Solid.get(color);
	}
	
	public static Fill solid(double r, double g, double b, double a) {
		// TODO clamp input values
		int color = ((((int)(a*255+0.5)) & 0xFF) << 24) |
					((((int)(r*255+0.5)) & 0xFF) << 16) | 
        			((((int)(g*255+0.5)) & 0xFF) <<  8) |
        			(((int)(b*255+0.5)) & 0xFF);
		return Solid.get(color);
	}
	
	public static Fill interpolate(float f, Fill a, Fill b) {
		boolean na = a==null, nb = b==null;
		if (na && nb) {
			return null;
		} else if (nb || (!na && a.getClass() == b.getClass())) {
			return a.interp(f, b);
		} else if (na) {
			return b.interp(1-f, a);
		} else if (f < 0.5f) {
			return a;
		} else {
			return b;
		}
	}
	
	public static class Solid extends Fill {
		static final IntObjectHashMap s_cache = new IntObjectHashMap();
		static Solid get(int color) {
			Solid f = (Solid) s_cache.get(color);
			if (f != null) return f;
			f = new Solid(color);
			s_cache.put(color, f);
			return f;
		}
		
		private final int _color;
		
		private Solid(int color) {
			_color = color;
		}
		
		public String key() { return String.valueOf(_color); }
		public int color() { return _color; }
		public double alpha() { return Color.alpha(_color) / 255.0; }
		
		protected Fill interp(float f, Fill fill) {
			int c = fill==null ? (_color & 0x00FFFFFF) : ((Solid)fill)._color;
			return get(Color.interpolate(f, _color, c));
		}
	}
	
}
