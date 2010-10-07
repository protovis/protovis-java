package pv.mark.update;

import pv.animate.Transition;
import pv.mark.Mark;
import pv.mark.MarkEvent;
import pv.mark.Mark.PropertySet;
import pv.mark.constants.Events;
import pv.mark.constants.MarkType;
import pv.mark.eval.Evaluator;
import pv.mark.eval.EvaluatorBuilder;
import pv.mark.eval.LinkEvaluator;
import pv.scene.GroupItem;
import pv.scene.Item;
import pv.scene.PanelItem;

public class SerialUpdater extends MarkUpdater {

	private EvaluatorBuilder _compiler = EvaluatorBuilder.instance();
	
	public void update(Mark mark, GroupItem proto, PanelItem panel, Transition t) {
		try {
			build(mark, proto, panel, t);
			MarkEvent.fire(MarkEvent.create(Events.update), mark.scene());
			evaluate(mark, panel, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void build(Mark mark, GroupItem proto, PanelItem panel, Transition t)
	{
		PropertySet pset = mark.bind();
		Evaluator eval = mark.evaluator();
		if (pset.dirty) {
			eval = _compiler.build(mark);
			mark.evaluator(eval);
		}
		
		// create group for scenegraph elements
		GroupItem group = eval.build(mark, proto, panel, t!=null);
		if (group.type == MarkType.Link) {
			((LinkEvaluator) eval).buildGraph(group);
		}
		
		// recurse
		if (group.type == MarkType.Panel) {
			// if we're building a panel, build child marks now
			for (int i=0; i<group.size(); ++i) {
				Item item = group.item(i);
				if (item.visible) {
					PanelItem panelItem = (PanelItem) item;
					for (Mark child : mark.children()) {
						build(child, null, panelItem, t);
					}
					//panelItem.discard(nidx);
				}
			}
		} else {
			// if we're not a panel, build inheriting marks now
			// compute child mark values, add as children of item
			for (Mark child : mark.children()) {
				build(child, group, panel, t);
			}
			// remove extra items if length changes
			//panelItem.discard(pidx);
		}
	}
	
	private void evaluate(Mark mark, PanelItem panel, Transition t)
	{
		int index = mark.treeIndex();
		GroupItem group = (GroupItem) panel.item(index);
		Evaluator eval = mark.evaluator();
		
		// evaluate marks
		eval.evaluate(group, 0, group.size(), t!=null);
		// create animators if needed
		if (t != null) t.add(eval.transition(group));
		
		// recurse
		if (group.type == MarkType.Panel) {
			for (int i=0; i<group.size(); ++i) {
				Item item = group.item(i);
				if (item.visible) {
					PanelItem panelItem = (PanelItem) item;
					for (Mark child : mark.children()) {
						evaluate(child, panelItem, t);
					}
				}
			}
		} else {
			for (Mark child : mark.children()) {
				evaluate(child, panel, t);
			}
		}
	}
	
}
