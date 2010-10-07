package pv.scene;

import java.util.Map;

import pv.mark.property.Property;

public class PanelItem extends GroupItem {
	
	public boolean cache;
	public Object cacheData = null;

	// TODO populate cache info?
	
	public static int checkProperties(Map<String,Property> props) {
		return Item.checkProperties(props);
	}
	
	public boolean interactive() {
		return group==this ? true : group.interactive();
	}
}
