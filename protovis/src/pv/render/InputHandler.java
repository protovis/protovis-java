package pv.render;

import java.util.List;

import pv.scene.GroupItem;
import pv.scene.Item;

public class InputHandler {

	public static Item pick(List<Item> list, double x, double y) {
		Item target = null;
		for (int i=list.size(); --i>=0;) {
			Item item = list.get(i);
			if (!item.visible) continue;
			if (item.hit(x,y)) {
				if (item instanceof GroupItem) {
					if (item.interactive()) {
						target = pick(item.items(), x-item.left, y-item.top);
					}
				} else {
					target = item;
				}
				// TODO pick panel?
				//if (target == null) {
				//	target = item;
				//}
			}
			if (target != null) break;
		}
		return target;
	}
	
}
