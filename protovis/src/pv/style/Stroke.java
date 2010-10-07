package pv.style;

import java.util.HashMap;
import java.util.Map;

public class Stroke {
	
	private static final Map<String,Stroke> s_cache = new HashMap<String,Stroke>();
	public static final Stroke none = null;
	
	private final double _width;
	private final Fill _fill;
	
	public double width() { return _width; }
	public Fill fill() { return _fill; }
	
	private Stroke(double width, Fill fill) {
		_width = width;
		_fill = fill;
	}
	
	// parse CSS statements? 1px solid #hhhhhh
	// (\d+)[px]?
	// (solid|others)
	// (#ddd|#dddddd)
	public static Stroke stroke(String spec) {
		throw new UnsupportedOperationException();
	}
	
	public static Stroke solid(double width) {
		return solid(width, 0, 1);
	}
	
	public static Stroke solid(double width, int color) {
		return solid(width, color, 1);
	}
	
	public static Stroke solid(double width, int color, double alpha) {
		return fill(width, Fill.solid(color, alpha));
	}
	
	public static Stroke solid(double width, double r, double g, double b, double a) {
		return fill(width, Fill.solid(r, g, b, a));
	}
	
	public static Stroke fill(double width, Fill fill) {
		String key = width+"-"+(fill==null ? "0" : fill.key());
		Stroke s = s_cache.get(key);
		if (s != null) return s;
		
		s = new Stroke(width, fill);
		s_cache.put(key, s);
		return s;
	}
	
	public static Stroke interpolate(float f, Stroke a, Stroke b) {
		double aw = a==null ? 0 : a._width;
		double bw = b==null ? 0 : b._width;
		Fill fill = Fill.interpolate(f,
			a==null ? null : a._fill,
			b==null ? null : b._fill);
		return fill(aw + f * (bw - aw), fill);
	}
	
}
