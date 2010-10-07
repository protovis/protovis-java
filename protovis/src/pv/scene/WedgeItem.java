package pv.scene;

import java.util.Map;

import pv.mark.property.Property;
import pv.util.Rect;

public class WedgeItem extends Item {

	public double angle;
	public double startAngle;
	public double endAngle;
	public double innerRadius;
	public double outerRadius;
	
	@Override
	public Rect bounds(Rect bounds)
	{
		double s = stroke==null ? 0 : stroke.width();
		if (s > 1) s = s/2;
		if (s < 1) s = 1;
		// OVERSHOOTS. TODO: better calculation
		bounds.set(left-s, top-s, width+s+s, height+s+s);
		return bounds;
	}

	public void populate(Item item) {
		WedgeItem witem = (WedgeItem) item;
		angle = witem.angle;
		startAngle = witem.startAngle;
		endAngle = witem.endAngle;
		innerRadius = witem.innerRadius;
		outerRadius = witem.outerRadius;
	}
	
	// ------------------------------------------------------------------------
		
	public static int checkProperties(Map<String,Property> props) {
		int bits = 0;
		if (!props.containsKey("left"))   bits |= LEFT;
		if (!props.containsKey("right"))  bits |= RIGHT;
		if (!props.containsKey("top"))    bits |= TOP;
		if (!props.containsKey("bottom")) bits |= BOTTOM;
		if (!props.containsKey("width"))  bits |= WIDTH;
		if (!props.containsKey("height")) bits |= HEIGHT;
		if (!props.containsKey("angle"))  bits |= ANGLE;
		if (!props.containsKey("endAngle")) bits |= END_ANGLE;
		return bits;
	}
	
	public void buildImplied(int props)
	{		
		super.buildImplied(props);
		if ((props & ANGLE) > 0) endAngle = startAngle + angle;
		if ((props & END_ANGLE) > 0) angle = endAngle - startAngle;
	}
	
	public long checkInterpolatedProperties(long props)
	{
		if (!dirty() || next==null) return props;
		WedgeItem a = (WedgeItem) next, b = (WedgeItem) a.next;
		
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
		
		if ((props & INNER) == 0 && a.innerRadius != b.innerRadius) props |= INNER;
		if ((props & OUTER) == 0 && a.outerRadius != b.outerRadius) props |= OUTER;
		if ((props & ANGLE) == 0 && (a.startAngle != b.startAngle
			|| a.endAngle != b.endAngle || a.angle != b.angle))
		{
			props |= ANGLE;
		}
		
		return props;
	}
	
}
