package pv.mark.eval;

import pv.mark.MarkEvent;
import pv.scene.Item;

public interface EventHandler {

	void handle(MarkEvent event, Item item);
	
}
