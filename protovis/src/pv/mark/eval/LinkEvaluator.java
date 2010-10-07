package pv.mark.eval;

import java.util.HashMap;
import java.util.Map;

import pv.mark.Mark;
import pv.mark.Mark.PropertySet;
import pv.mark.property.Property;
import pv.scene.GroupItem;
import pv.scene.Item;
import pv.scene.LinkItem;
import pv.util.Objects;

public abstract class LinkEvaluator extends ItemEvaluator {

	private Property _snk=null, _tnk=null, _sk=null, _tk=null;
	
	public Object sourceNodeKey(Item item) { return item.data; }
	public Object targetNodeKey(Item item) { return item.data; }
	public Object linkSourceKey(Item item, Object datum) { return datum; }
	public Object linkTargetKey(Item item, Object datum) { return datum; }

	public Object sourceNodes(GroupItem group) { return null; }
	public Object targetNodes(GroupItem group) { return null; }
	
	@SuppressWarnings("unchecked")
	public void buildGraph(GroupItem group)
	{		
		Object sn = sourceNodes(group);
		Object tn = targetNodes(group);
		GroupItem sources, targets;
		
		// get source nodes
		if (sn instanceof GroupItem) {
			sources = (GroupItem) sn;
		} else if (sn instanceof Mark) {
			Mark mark = (Mark) sn;
			sources = (GroupItem) group.group.item(mark.treeIndex());
		} else {
			sources = group.proto;
		}
		
		// get target nodes
		if (sn == tn) {
			targets = sources;
		} else if (tn instanceof GroupItem) {
			targets = (GroupItem) tn;
		} else if (tn instanceof Mark) {
			Mark mark = (Mark) tn;
			targets = (GroupItem) group.group.item(mark.treeIndex());
		} else {
			targets = group.proto;
		}
		
		// get key properties
		PropertySet pset = mark.propertySet();
		Property snk = pset.keys.get("sourceNodeKey");
		Property tnk = pset.keys.get("targetNodeKey");
		Property sk  = pset.keys.get("sourceKey");
		Property tk  = pset.keys.get("targetKey");

		// get maps
		Map<Object,Item> sourceMap = MapCache.get(snk, sources);
		Map<Object,Item> targetMap = MapCache.get(tnk, targets);
		
		boolean buildSourceMap = (sourceMap==null || snk!=_snk || sources.modified());
		boolean buildTargetMap = (targetMap==null || tnk!=_tnk || targets.modified());
		boolean linkGroup = (group.modified() || group.dirty() || sk!=_sk || tk!=_tk);
			
		// check for early exit
		if (!(buildSourceMap || buildTargetMap || linkGroup))
			return;
		
		// build source node map
		if (buildSourceMap) {
			if (sourceMap != null) {
				sourceMap.clear();
			} else {
				sourceMap = (HashMap<Object,Item>) Objects.Map.get();
				MapCache.put(snk, sources, sourceMap);
			}
			for (Item node : sources.items()) {
				sourceMap.put(sourceNodeKey(node), node);
			}
			sources.modified(false);
		}
		
		// build target node map
		if (buildTargetMap) {
			if (sourceMap == targetMap) {
				targetMap = null;
			}
			if (sources==targets && snk.equals(tnk)) {
				targetMap = sourceMap;
			} else {
				if (targetMap != null) {
					targetMap.clear();
				} else {
					targetMap = (HashMap<Object,Item>) Objects.Map.get();
					MapCache.put(tnk, targets, targetMap);
				}
				for (Item node : targets.items()) {
					targetMap.put(targetNodeKey(node), node);
				}
				targets.modified(false);
			}
		}
		
		// resolve link sources and targets
		if (buildSourceMap || buildTargetMap || linkGroup) {
			for (int i=0; i<group.size(); ++i) {
				LinkItem item = (LinkItem) group.item(i);
				item.source = sourceMap.get(linkSourceKey(item, item.data));
				item.target = targetMap.get(linkTargetKey(item, item.data));
			}
		}
		
		// keep key properties to track changes
		_snk = snk; _tnk = tnk; _sk = sk; _tk = tk;
	}

}
