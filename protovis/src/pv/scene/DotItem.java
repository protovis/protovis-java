package pv.scene;

import java.util.Map;

import pv.mark.property.Property;
import pv.util.Rect;

public class DotItem extends Item {

	public String shape;
	public double size;
	public double radius;
	
	@Override
	public Rect bounds(Rect bounds)
	{
		double s = stroke==null ? 0 : stroke.width();
		if (s > 1) s = s/2;
		if (s < 1) s = 1;
		s += radius;
		bounds.set(left-s, top-s, s=(s+s), s);
		return bounds;
	}
	
	protected boolean inBounds(double x, double y)
	{
		// circular hit testing for now. TODO: fix this up
		double s = stroke==null ? 0 : stroke.width();
		double dx = x - left;
		double dy = y - top;
		return (dx*dx + dy*dy) <= (size+s);
	}
	
	public void populate(Item item) {
		DotItem ditem = (DotItem) item;
		shape = ditem.shape;
		size = ditem.size;
		radius = ditem.radius;
		super.populate(item);
	}
	
	public static int checkProperties(Map<String,Property> props) {
		return Item.checkProperties(props);
	}
	
	public void buildImplied(int props)
	{
		double pw = group.group.width;
		double ph = group.group.height;
		double l = ((props & LEFT) > 0)   ? 0 : left;
		double r = ((props & RIGHT) > 0)  ? 0 : right;
		double t = ((props & TOP) > 0)    ? 0 : top;
		double b = ((props & BOTTOM) > 0) ? 0 : bottom;
			
		if ((props & RIGHT) > 0) {
			r = pw - l;
		} else if ((props & LEFT) > 0) {
			l = pw - r;
		}

		if ((props & BOTTOM) > 0) {
			b = ph - t;
		} else if ((props & TOP) > 0) {
			t = ph - b;
		}
			
		left = l;
		right = r;
		top = t;
		bottom = b;
	}
	
	public long checkInterpolatedProperties(long props)
	{
		if (!dirty() || next==null) return props;
		DotItem a = (DotItem) next, b = (DotItem) a.next;
		
		if ((props & XY) == 0 && (a.left != b.left || a.right != b.right
			|| a.top != b.top || a.bottom != b.bottom))
		{
			props |= XY;
		}
		if ((props & ALPHA) == 0 && a.alpha != b.alpha) props |= ALPHA;
		if ((props & WIDTH) == 0 && a.width != b.width) props |= WIDTH;
		if ((props & HEIGHT) == 0 && a.height != b.height) props |= HEIGHT;
		if ((props & FILL) == 0 && a.fill != b.fill) props |= FILL;
		if ((props & STROKE) == 0 && a.stroke != b.stroke) props |= STROKE;
		if ((props & SHAPE) == 0 && a.shape != b.shape) props |= SHAPE;
		if ((props & SIZE) == 0 && a.size != b.size) props |= SIZE;
		
		return props;
	}
	
}
