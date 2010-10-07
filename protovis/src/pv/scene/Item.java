package pv.scene;

import java.util.List;
import java.util.Map;

import pv.mark.constants.PropertyCodes;
import pv.mark.property.Property;
import pv.style.Easing;
import pv.style.Fill;
import pv.style.Stroke;
import pv.util.Rect;

public class Item implements PropertyCodes {

	public static final byte DIRTY = 0x1;
	public static final byte ZOMBIE = 0x2;
	public static final byte DEAD = 0x4;
	public static final byte BORN = 0x8;
	public static final byte MODIFIED = 0x10;
	
	public byte flags = DIRTY;	
	public int index;
	public boolean visible = true;
	public Object data;
	public GroupItem group;
	public Item next = null;
	
	public Easing ease;
	public double delay;

	public double left;
	public double right;
	public double top;
	public double bottom;
	public double width;
	public double height;
	public double alpha = 1;
	
	public Stroke stroke;
	public Fill fill;
	
	public Item parent() { return group.group; }
	public Item ancestor(int level) {
		Item item = this;
		while (--level >= 0) item = item.group.group;
		return item;
	}
	public int index(int level) { return ancestor(level).index; }
	public Object data(int level) { return ancestor(level).data; }
	
	public Item proto() {
		GroupItem p = group.proto;
		if (p == null) return null;
		return index >= p.items.size() ? null : p.items.get(index); 
	}
	public Item sibling() {
		return (index == 0 || group.items.size() < index)
			? null : group.items.get(index-1);
	}
	public Item cousin() {
		GroupItem p1 = group.group;
		if (p1 == null || p1.index==0
			|| p1.group.items.size() < p1.index) return null;
		GroupItem p0 = (GroupItem) p1.group.items.get(p1.index-1);
		GroupItem g0 = (GroupItem) p0.items.get(group.index);
		return g0.items.size() > index ? g0.items.get(index) : null;
	}
	public Item item(int index) { return null; }
	public List<Item> items() { return null; }
	
	public boolean interactive() { return group.interactive(); }
	
	public void populate(Item item) {
		//dirty = item.dirty;
		//visible = item.visible;
		alpha = item.alpha;
		left = item.left;
		right = item.right;
		top = item.top;
		bottom = item.bottom;
		width = item.width;
		height = item.height;
		stroke = item.stroke;
		fill = item.fill;
	}
	
	public final boolean dirty() { return (flags & DIRTY) > 0; }
	public final void dirty(boolean b) {
		if (b) flags |= DIRTY; else flags &= ~DIRTY;
	}
	
	public final boolean zombie() { return (flags & ZOMBIE) > 0; }
	public final void zombie(boolean b) {
		if (b) flags |= ZOMBIE; else flags &= ~ZOMBIE;
	}
	
	public final boolean dead() { return (flags & DEAD) > 0; }
	public final void dead(boolean b) {
		if (b) flags |= DEAD; else flags &= ~DEAD;
	}
	
	public final boolean born() { return (flags & BORN) > 0; }
	public final void born(boolean b) {
		if (b) flags |= BORN; else flags &= ~BORN;
	}
	
	public final boolean modified() { return (flags & MODIFIED) > 0; }
	public final void modified(boolean b) {
		if (b) flags |= MODIFIED; else flags &= ~MODIFIED;
	}
	
	public void discard(int index) {
		flags |= DIRTY;
		visible = false;
	}
	
	// -- Geometry Management -------------------------------------------------
	
	public Rect bounds(Rect bounds)
	{
		double s = stroke==null ? 0 : stroke.width();
		if (s > 1) s = s/2;
		if (s < 1) s = 1;
		bounds.set(left-s, top-s, width+s+s, height+s+s);
		return bounds;
	}
	
	protected boolean inBounds(double x, double y)
	{
		double s = stroke==null ? 0 : stroke.width();
		if (s > 1) s = s/2;
		if (s < 1) s = 1;
		return (x >= left-s && x <= left+width+s && y >= top-s && y <= top+height+s);
	}
	
	public boolean hit(double x, double y)
	{
		return inBounds(x, y);
	}
	
	// -- Build Implied Properties --------------------------------------------
			
	public static int checkProperties(Map<String,Property> props) {
		int bits = 0;
		if (!props.containsKey("left"))   bits |= LEFT;
		if (!props.containsKey("right"))  bits |= RIGHT;
		if (!props.containsKey("top"))    bits |= TOP;
		if (!props.containsKey("bottom")) bits |= BOTTOM;
		if (!props.containsKey("width"))  bits |= WIDTH;
		if (!props.containsKey("height")) bits |= HEIGHT;
		return bits;
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
		if ((props & FILL) == 0 && a.fill != b.fill) props |= FILL;
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
		double w = ((props & WIDTH) > 0)  ? 0 : width;
		double h = ((props & HEIGHT) > 0) ? 0 : height;
			
		if ((props & WIDTH) > 0) {
			w = pw - r - l;
		} else if ((props & RIGHT) > 0) {
			r = pw - w - l;
		} else if ((props & LEFT) > 0) {
			l = pw - w - r;
		}

		if ((props & HEIGHT) > 0) {
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
		if ((props & WIDTH) > 0) width = w;
		if ((props & HEIGHT) > 0) height = h;
	}
	
}
