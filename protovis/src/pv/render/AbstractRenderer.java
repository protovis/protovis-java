package pv.render;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import pv.scene.GroupItem;
import pv.scene.Item;
import pv.util.Objects;

public class AbstractRenderer {

	protected static final Comparator<GroupItem> _depthCmp = new Comparator<GroupItem>()
	{
		public int compare(GroupItem i1, GroupItem i2) {
			double d1 = i1.depth, d2 = i2.depth;
			return (d1 < d2 ? 1 : d1 > d2 ? -1 : 0);
		}
	};
	
	protected int _width, _height;
	
	public int width() { return _width; }
	public void width(int width) {
		_width = width;
	}
	public int height() { return _height; }
	public void height(int height) {
		_height = height;
	}
		
	// switch to radix sort?
	@SuppressWarnings("unchecked")
	public static List<GroupItem> preprocess(List<Item> items) {
		List<GroupItem> list = (List<GroupItem>)Objects.List.get();
		int len = items.size();
		for (int i=0; i<len; ++i) {
			GroupItem g = (GroupItem) items.get(i);
			if (g != null) list.add(g);
		}
		if (len > 1) {
			Collections.sort(list, _depthCmp);
		}
		return list;
	}
	
}
