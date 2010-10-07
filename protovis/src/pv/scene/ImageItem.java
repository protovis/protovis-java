package pv.scene;

import java.util.Map;

import pv.mark.property.Property;

public class ImageItem extends Item {

	public String url;
	
	public void populate(Item item) {
		ImageItem iitem = (ImageItem) item;
		url = iitem.url;
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
		double w = ((props & WIDTH) > 0)  ? 0 : width;
		double h = ((props & HEIGHT) > 0) ? 0 : height;
			
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
		width = w;
		height = h;
	}
	
	public long checkInterpolatedProperties(long props)
	{
		if (!dirty() || next==null) return props;
		ImageItem a = (ImageItem) next, b = (ImageItem) a.next;
		
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
		if ((props & URL) == 0 && a.url != b.url) props |= URL;
		
		return props;
	}
	
}
