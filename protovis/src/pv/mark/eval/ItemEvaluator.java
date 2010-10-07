package pv.mark.eval;

import java.util.Arrays;
import java.util.List;

import pv.animate.Animator;
import pv.animate.GroupTransition;
import pv.animate.Transition;
import pv.mark.Mark;
import pv.scene.GroupItem;
import pv.util.Objects;

public abstract class ItemEvaluator implements Evaluator {
	
	public Mark mark;
	
	public Class<?> datatype() {
		return Object.class;
	}
	
	public Mark mark() {
		return mark;
	}

	public Object key(Object datum, int index) {
		return index;
	}
	
	protected static GroupItem getGroup(Mark mark, GroupItem proto, GroupItem layer)
	{
		int index = mark.treeIndex();
		GroupItem group = (GroupItem) layer.items.get(index);
		
		if (group == null) {
			group = new GroupItem(mark.markType());
			group.group = layer;
			group.index = index;
			layer.items.set(index, group);
		} else {
			group.dirty(false);
		}
		
		group.proto = proto;
		group.props = 0;
		return group;
	}
	
	protected Object getData(GroupItem item) {
		return item.group.data;
	}
	
	public Iterable<?> data(GroupItem group) {
		Object dataObj = getData(group);
		Iterable<?> data = null;
		if (dataObj == null) {
			data = Arrays.asList((Object)null);
		} else if (dataObj instanceof Iterable<?>) {
			data = (Iterable<?>) dataObj;
		} else {
			data = Arrays.asList(dataObj);
		}
		return data;
	}
	
	@SuppressWarnings("unchecked")
	public Transition transition(GroupItem group) {
		List<Animator> interp = (List<Animator>) Objects.List.get();
		Animator.Init.instance(group.type).assign(group, group.props, interp);
		if (interp.size() > 0) {
			return new GroupTransition(group, interp);
		} else {
			Objects.List.reclaim(interp);
			return null;
		}
	}
	
}
