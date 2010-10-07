package pv.mark.eval;

import pv.animate.Transition;
import pv.mark.Mark;
import pv.scene.GroupItem;
import pv.scene.PanelItem;

public interface Evaluator {
	
	Class<?> datatype();
	
	Mark mark();
	
	Iterable<?> data(GroupItem group);
	
	GroupItem build(Mark mark, GroupItem proto, PanelItem panel, boolean animate);
	
	void evaluate(GroupItem g, int start, int end, boolean animate);
	
	Transition transition(GroupItem g);
	
}
