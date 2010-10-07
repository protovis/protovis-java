package pv.util;

public class Rect {

	public double x;
	public double y;
	public double w;
	public double h;
	
	public Rect() {
	}
	
	public Rect(double x, double y, double w, double h) {
		set(x, y, w, h);
	}
	
	public void set(double x, double y, double w, double h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}
	
	public void set(Rect r) {
		this.x = r.x;
		this.y = r.y;
		this.w = r.w;
		this.h = r.h;
	}
	
	public double getMinX() { return w < 0 ? x+w : x; }
	public double getMaxX() { return w < 0 ? x : x+w; }
	public double getMinY() { return h < 0 ? y+h : y; }
	public double getMaxY() { return h < 0 ? y : y+h; }
	
	public boolean contains(double x, double y) {
		return x >= getMinX() && x <= getMaxX() &&
			y >= getMinY() && y <= getMaxY();
	}
	
	public static void union(Rect s, Rect t, Rect r) {
		double x1 = Math.min(s.getMinX(), t.getMinX());
		double y1 = Math.min(s.getMinY(), t.getMinY());
		double x2 = Math.max(s.getMaxX(), t.getMaxX());
		double y2 = Math.max(s.getMaxY(), t.getMaxY());
		r.set(x1, y1, x2-x1, y2-y1);
	}
	
	public boolean intersects(Rect r) {
		return !(getMinX() > r.getMaxX() || getMaxX() < r.getMinX() ||
				getMinY() > r.getMaxY() || getMaxY() < r.getMinY());
	}
	
	public boolean intersects(double x, double y, double w, double h) {
		double minX = w < 0 ? x + w : x, maxX = w < 0 ? x : x + w;
		double minY = h < 0 ? y + h : y, maxY = h < 0 ? y : y + h;
		return !(getMinX() > maxX || getMaxX() < minX ||
				getMinY() > maxY || getMaxY() < minY);
	}
	
}
