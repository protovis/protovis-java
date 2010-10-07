package pv.scene;

import java.util.Map;

import pv.mark.property.Property;
import pv.style.Font;

public class LabelItem extends Item {

	public Font font;
	public String text;
	public String textBaseline;
	public String textAlign;
	public double textAngle;
	public double textMargin;
	
	public void populate(Item item) {
		LabelItem litem = (LabelItem) item;
		font = litem.font;
		text = litem.text;
		textBaseline = litem.textBaseline;
		textAlign = litem.textAlign;
		textAngle = litem.textAngle;
		textMargin = litem.textMargin;
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
		if (text != null)
			text = text.intern();
	}
	
	public long checkInterpolatedProperties(long props)
	{
		if (!dirty() || next==null) return props;
		LabelItem a = (LabelItem) next, b = (LabelItem) a.next;
		
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
		
		if ((props & FONT) == 0 && a.font != b.font) props |= FONT;
		if ((props & TEXT) == 0 && a.text != b.text) props |= TEXT;
		if ((props & BASELINE) == 0 && a.textBaseline != b.textBaseline) props |= BASELINE;
		if ((props & ANGLE) == 0 && a.textAngle != b.textAngle) props |= ANGLE;
		if ((props & ALIGN) == 0 && a.textAlign != b.textAlign) props |= ALIGN;
		if ((props & MARGIN) == 0 && a.textMargin != b.textMargin) props |= MARGIN;
		
		return props;
	}
	
}
