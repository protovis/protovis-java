package pv.mark;

import java.util.List;

import pv.mark.eval.EventHandler;
import pv.scene.GroupItem;
import pv.scene.Item;

public class MarkEvent {

	public String type;
	public long when;
	public double x;
	public double y;
	public int screenX;
	public int screenY;
	public int button;
	public int clickCount;
	public int wheelRotation;
	
	public char keyChar;
	public int keyCode;
	public int modifiers;
	
	// helper methods
	// isShiftDown
	// isCtrlDown
	// isAltDown
	// isMetaDown
	
	public static void fire(MarkEvent e, Item item) {
		GroupItem group = item.group == null ? (GroupItem)item : item.group;
		fire(e, item, group);
	}
	
	public static void fire(MarkEvent e, Item item, GroupItem group) {
		List<EventHandler> handlers = group.handlers(e.type);
		if (handlers == null) return;
		for (EventHandler h : handlers) {
			h.handle(e, item);
		}
	}
	
	public static void fire(MarkEvent e, Scene scene) {
		List<EventHandler> handlers = scene.handlers().get(e.type);
		if (handlers == null) return;
		for (EventHandler h : handlers) {
			h.handle(e, scene.items());
		}
	}
	
	public static MarkEvent create(String type) {
		MarkEvent m = new MarkEvent();
		m.type = type;
		m.when = System.currentTimeMillis();
		return m;
	}
	
}
