package pv.scene;

import java.util.Map;

import pv.mark.property.Property;


public class RuleItem extends Item {

	public static int checkProperties(Map<String,Property> props) {
		return Item.checkProperties(props);
	}
	
	public long checkInterpolatedProperties(long props)
	{
		if (!dirty() || next == null) return props;
		Item a = next, b = a.next;
		
		if ((props & XY) == 0 && (a.left != b.left || a.right != b.right
			|| a.top != b.top || a.bottom != b.bottom))
		{
			props |= XY;
		}
		if ((props & ALPHA) == 0 && a.alpha != b.alpha) props |= ALPHA;
		if ((props & WIDTH) == 0 && a.width != b.width) props |= WIDTH;
		if ((props & HEIGHT) == 0 && a.height != b.height) props |= HEIGHT;
		if ((props & STROKE) == 0 && a.stroke != b.stroke) props |= STROKE;
		
		return props;
	}
	
	public void buildImplied(int props)
	{
		double pw = group.group.width;
		double ph = group.group.height;
		double l = ((props & LEFT) > 0)   ? 0 : left;
		double r = ((props & RIGHT) > 0)  ? 0 : right;
		double t = ((props & TOP) > 0)    ? 0 : top;
		double b = ((props & BOTTOM) > 0) ? 0 : bottom;
		boolean ww = (props & WIDTH) > 0;
		boolean hh = (props & HEIGHT) > 0;
		
		/* Determine horizontal or vertical orientation. */
		if (!ww || (((props & LEFT) > 0) && ((props & RIGHT) > 0))
			|| (((props & RIGHT) == 0) && ((props & LEFT) == 0)))
		{
			height = 0;
			hh = false;
		} else {
			width = 0;
			ww = false;
		}
		
		double w = ((props & WIDTH) > 0)  ? 0 : width;
		double h = ((props & HEIGHT) > 0) ? 0 : height;
		
		if (ww) {
			w = pw - r - l;
		} else if ((props & RIGHT) > 0) {
			r = pw - w - l;
		} else if ((props & LEFT) > 0) {
			l = pw - w - r;
		}

		if (hh) {
			h = ph - t - b;
		} else if ((props & BOTTOM) > 0) {
			b = ph - h - t;
		} else if ((props & TOP) > 0) {
			t = ph - h - b;
		}
			
		left = l;
		right = r;
		top = t;
		bottom = b;
		if (ww) width = w;
		if (hh) height = h;
	}
}
