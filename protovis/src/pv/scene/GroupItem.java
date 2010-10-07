package pv.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pv.mark.constants.MarkType;
import pv.mark.eval.EventHandler;
import pv.util.Objects;
import pv.util.Rect;

public class GroupItem extends Item {
	
	public String type;
	public List<Item> items = new ArrayList<Item>(1);
	public Map<String,List<EventHandler>> handlers;
	public GroupItem proto = null;
	public Rect bounds = new Rect();
	
	public long props = 0;
	public boolean segmented;
	public String interpolate;
	public int depth = 0;

	public GroupItem() {
	}
	
	public GroupItem(String type) {
		this.type = type;
	}
	
	public void add(Item item) { items.add(item); }
	public void set(int index, Item item) { items.set(index, item); }
	public Item item(int index) { return items.get(index); }
	public List<Item> items() { return items; }
	public int size() { return items.size(); }
	
	public List<EventHandler> handlers(String type) {
		return handlers==null ? null : handlers.get(type);
	}
	
	public boolean interactive() {
		return type==MarkType.Panel || (handlers != null && handlers.size() > 0);
	}
	
	@Override
	public void discard(int index) {
		if (index < 0) {
			super.discard(-1);
			index = 0;
		}
		for (int i=items.size(); --i >= index;) {
			items.remove(i).discard(-1);
		}
	}
	
	public Rect bounds(Rect b)
	{
		b.set(bounds);
		return b;
	}
	
	public void computeBounds()
	{
		bounds.set(left, top, 0, 0);
		int len = items.size(), i=0;
		Item item = null;
		
		if (len > 0) {
			while (item == null && i < len) item = items.get(i++);
			if (item == null) return; // TODO prevent from happening
			item.bounds(bounds);
			bounds.set(bounds.x+left, bounds.y+top, bounds.w, bounds.h);
		}
		if (len > 1) {
			Rect b = Objects.Rect.get();
			for (; i<len; ++i) {
				item = items.get(i);
				if (item == null) continue;
				item.bounds(b);
				b.set(b.x+left, b.y+top, b.w, b.h);
				Rect.union(bounds, b, bounds);
			}
			Objects.Rect.reclaim(b);
		}
	}
	
	protected boolean inBounds(double x, double y)
	{
		return bounds.contains(x, y);
	}
}
