package pv.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Objects {

	// -- Vectors -------------------------------------------------------------
	
	private static ArrayList<double[]> __vecs = new ArrayList<double[]>();
	private static ArrayList<double[]> __used = new ArrayList<double[]>();
	
	public static double[] leaseVector(double x, double y) {
		return leaseVector(x, y, 0);
	}
	
	public static double[] leaseVector(double x, double y, double z) {
		int len = __vecs.size();
		double[] vec = null;
		if (len > 0) {
			vec = __vecs.remove(len-1);
		} else {
			vec = new double[3];
		}
		__used.add(vec);
		vec[0] = x;
		vec[1] = y;
		vec[2] = z;
		return vec;
	}
	
	public static void reclaimLeasedVectors() {
		for (int i = __used.size(); --i >= 0;) {
			__vecs.add(__used.remove(i));
		}
	}
	
	// -- Vectors -------------------------------------------------------------
	
	public static final ObjectPool<double[]> Vector = new ObjectPool<double[]>(100) {
		public double[] create() { return new double[3]; }
	};
	
	// -- Array Lists ---------------------------------------------------------
	
	public static final ObjectPool<List<?>> List = new ObjectPool<List<?>>(100) {
		public List<?> create() { return new ArrayList<Object>(); }
		public void clear(List<?> list) { list.clear(); }
	};
	
	// -- Rectangle2D ---------------------------------------------------------
	
	public static final ObjectPool<Rect> Rect = new ObjectPool<Rect>(100) {
		public Rect create() { return new Rect(); }
		public void clear(Rect r) { r.set(0, 0, 0, 0); }
	};
	
	// -- HashMaps ------------------------------------------------------------
	
	public static final ObjectPool<Map<?,?>> Map = new ObjectPool<Map<?,?>>(100) {
		public Map<?,?> create() { return new HashMap<Object,Object>(); }
		public void clear(Map<?,?> map) { map.clear(); }
	};
	
	// -- String Buffers ------------------------------------------------------
	
	public static final ObjectPool<StringBuffer> StringBuffer = new ObjectPool<StringBuffer>(100) {
		public StringBuffer create() { return new StringBuffer(); }
		public void clear(StringBuffer sbuf) { sbuf.delete(0, sbuf.length()); }
	};
	
}
