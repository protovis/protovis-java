package pv.mark.eval;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pv.mark.property.Property;
import pv.scene.GroupItem;
import pv.scene.Item;

public class MapCache {

	private static Map<Long, Map<Object,Item>> s_maps =
		new ConcurrentHashMap<Long, Map<Object,Item>>();
	
	public static Map<Object,Item> get(Property key, GroupItem group) {
		long k = (((long)key.hashCode()) << 32) | group.hashCode();
		return s_maps.get(k);
	}
	
	public static void put(Property key, GroupItem group, Map<Object,Item> map) {
		long k = (((long)key.hashCode()) << 32) | group.hashCode();
		s_maps.put(k, map);
	}
	
	public static Map<Object,Item> remove(Property key, GroupItem group) {
		long k = (((long)key.hashCode()) << 32) | group.hashCode();
		return s_maps.remove(k);
	}
	
	public static void clear() {
		s_maps.clear();
	}
	
}
