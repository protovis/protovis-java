package pv.mark.eval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pv.mark.Mark;
import pv.mark.MarkEvent;
import pv.mark.Mark.PropertySet;
import pv.mark.constants.Events;
import pv.mark.constants.MarkType;
import pv.mark.property.ConstantProperty;
import pv.mark.property.Property;
import pv.scene.DotItem;
import pv.scene.GroupItem;
import pv.scene.ImageItem;
import pv.scene.Item;
import pv.scene.LabelItem;
import pv.scene.PanelItem;
import pv.scene.LinkItem;
import pv.scene.RuleItem;
import pv.scene.WedgeItem;
import pv.style.Easing;
import pv.util.Objects;

public class StaticEvaluator extends ItemEvaluator {

	protected Item _dummy = new Item();
	public boolean _hasKey = true;
	public int _props;
	
	public Class<?> _itemtype;
	public Class<?> _datatype;
	public PropertySet _pset;
	public ItemFactory _factory;
	
	public List<Pair> _pgroup = new ArrayList<Pair>();
	public List<Pair> _pinstance = new ArrayList<Pair>();
	public List<Pair> _penter = new ArrayList<Pair>();
	public List<Pair> _pexit = new ArrayList<Pair>();
	
	public StaticEvaluator(Mark mark) {
		this.mark = mark;
		_pset = mark.propertySet();
		_itemtype = mark.itemType();
		_datatype = (Class<?>) getConstant(_pset, "datatype");
		_factory = ItemFactory.get(mark.markType());
		_hasKey = _pset.keys.containsKey("key");
		init(mark.propertySet());
	}
	
	private void init(PropertySet pset) {
		if (pset.group != null) {
			for (Map.Entry<String,Property> e : pset.group.entrySet()) {
				PropertyEval pe = PropertyEval.get(e.getKey());
				if (pe == null || e.getValue() == null) continue;
				_pgroup.add(new Pair(pe, e.getValue()));
			}
		}
		if (pset.instance != null) {
			for (Map.Entry<String,Property> e : pset.instance.entrySet()) {
				PropertyEval pe = PropertyEval.get(e.getKey());
				if (pe == null || e.getValue() == null) continue;
				_pinstance.add(new Pair(pe, e.getValue()));
			}
		}
		if (pset.enter != null) {
			for (Map.Entry<String,Property> e : pset.enter.entrySet()) {
				PropertyEval pe = PropertyEval.get(e.getKey());
				if (pe == null || e.getValue() == null) continue;
				_penter.add(new Pair(pe, e.getValue()));
			}
		}
		if (pset.exit != null) {
			for (Map.Entry<String,Property> e : pset.exit.entrySet()) {
				PropertyEval pe = PropertyEval.get(e.getKey());
				if (pe == null || e.getValue() == null) continue;
				_pexit.add(new Pair(pe, e.getValue()));
			}
		}
	}
	
	public Class<?> datatype() {
		return _datatype;
	}
	
	private static Object getConstant(PropertySet pset, String name) {
		Property p = (Property) pset.group.get(name);
		return p != null && p instanceof ConstantProperty ?
			((ConstantProperty)p).value() : null;
	}
		
	protected Object getData(GroupItem item) {
		return _pset.data==null ? null : _pset.data.object(item);
	}
	
	public Object key(Item item) {
		return _hasKey ? _pset.keys.get("key").object(item) : item.index;
	}
	
	private void evalItem(Item item, List<Pair> list) {
		int len = list.size();
		for (int i=0; i<len; ++i) {
			list.get(i).eval(item);
		}
	}
	
	@SuppressWarnings("unchecked")
	public GroupItem build(Mark mark, GroupItem proto, PanelItem layer, boolean animate) {
		GroupItem group = getGroup(mark, proto, layer);
		group.modified(false);
		Iterable<?> _data_ = data(group);
				
		// BUILD LOOKUP TABLE
		Map<Object,Item> map = null;
		if (animate && _hasKey) {
			map = (Map<Object,Item>) Objects.Map.get();
			for (int i=0; i<group.size(); ++i) {
				Item item = group.item(i);
				map.put(key(item), item);
			}
			group.items.clear();
		}
	
		// GENERATE ITEMS
		int index = 0;
		for (Object datum : _data_) {
			Object data = datum;
			
			// GET SCENEGRAPH ITEM
			Item item = null;
			if (map != null) {
				_dummy.data = data;
				_dummy.index = index;
				item = map.remove(key(_dummy));
			}
			if (item != null) {
				group.add(item);
				item.index = index;
			} else if (group.items.size() > index) {
				item = group.item(index);
			} else {
				item = _factory.create();
				group.add(item);
				item.group = group;
				item.index = index;
				item.born(true);
			}
			item.zombie(false);
			item.dead(false);
			if (item.data != data) {
				item.data = data;
				group.modified(true);
			}
			
			if (animate) {
				Item prev = item.next, next;
				if (prev == null) {
					prev = (item.next = _factory.create()); prev.group = group;
					next = (prev.next = _factory.create()); next.group = group;
					prev.data = data;
					next.data = data;
				} else {
					next = prev.next;
				}
				prev.index = item.index;
				prev.populate(item);
			}
			
			index += 1;
		}
		if (index != group.size())
			group.dirty(true);
		
		// PRESIZE LAYER INSTANCES
		if (mark.markType() == MarkType.Panel) {
			int layerSize = mark.panelSize();
			for (int i=0; i<group.size(); ++i) {
				PanelItem item = (PanelItem) group.item(i);
				while (item.size() < layerSize) {
					item.add(null);
				}
			}
		}
		
		// PROCESS ZOMBIE ITEMS
		int idx = index;
		if (map != null) {
			for (Item item : map.values()) {
				if (item.dead()) {
					group.modified(true);
					continue;
				}
				item.zombie(true);
				if (item.next == null) {
					item.next = _factory.create();
					item.next.next = _factory.create();
					item.next.group = group;
					item.next.next.group = group;
				}
				item.next.populate(item);
				group.add(item);
				idx += 1;
			}
			Objects.Map.reclaim(map);
		} 
		
		// remove extra items if length changes
		group.discard(idx);
		
		// EVALUATE GROUP PROPERTIES
		{
			GroupItem item = group;
			item.handlers = mark.propertySet().handlers;
			evalItem(item, _pgroup);
			// FIRE BUILD EVENT
			MarkEvent.fire(MarkEvent.create(Events.build), item, item);
		}
		return group;
	}
		
	public void evaluate(GroupItem group, int start, int end, boolean animate) {
		for (int i=start; i<end; ++i) {
			Item item = group.item(i);
			boolean zombie = item.zombie();
			
			// EVALUATE PROPERTIES
			evalItem(item, _pinstance);
			if (zombie) {
				evalItem(item, _pexit);
			}
			item.buildImplied(_props);
			
			if (animate) {
				Item orig = item;
				item.next.next.populate(item);
				if (item.born()) {
					item.born(false);
					item.next.populate(item);
					item = item.next;
					evalItem(item, _penter);
					item.buildImplied(_props);
				} else if (!zombie) {
					item.populate(item.next); // ???
				}
				group.props = orig.checkInterpolatedProperties(group.props); 
			} else {
				item.born(false);
			}
		}
	}
	
	public static class Pair {
		PropertyEval eval;
		Property p;
		public Pair(PropertyEval eval, Property p) {
			this.eval = eval;
			this.p = p;
		}		
		public void eval(Item x) {
			eval.eval(x, p);
		}
	}
	
	public static class ItemFactory
	{	
		public static ItemFactory instance = new ItemFactory();
		public static Map<String, ItemFactory> map =
			new HashMap<String, ItemFactory>();
		static {
			map.put(MarkType.Dot,   new DotFactory());
			map.put(MarkType.Image, new ImageFactory());
			map.put(MarkType.Label, new LabelFactory());
			map.put(MarkType.Panel, new LayerFactory());
			map.put(MarkType.Link,  new LinkFactory());
			map.put(MarkType.Rule,  new RuleFactory());
			map.put(MarkType.Wedge, new WedgeFactory());
		}
		public static ItemFactory get(String markType) {
			ItemFactory f = map.get(markType);
			return f==null ? instance : f;
		}
		public Item create() { return new Item(); }
		
		public static class DotFactory extends ItemFactory {
			public Item create() { return new DotItem(); }
		}
		public static class ImageFactory extends ItemFactory {
			public Item create() { return new ImageItem(); }
		}
		public static class LabelFactory extends ItemFactory {
			public Item create() { return new LabelItem(); }
		}
		public static class LayerFactory extends ItemFactory {
			public Item create() { return new PanelItem(); }
		}
		public static class LinkFactory extends ItemFactory {
			public Item create() { return new LinkItem(); }
		}
		public static class RuleFactory extends ItemFactory {
			public Item create() { return new RuleItem(); }
		}
		public static class WedgeFactory extends ItemFactory {
			public Item create() { return new WedgeItem(); }
		}
	}

	public abstract static class PropertyEval {
		
		public static Map<String, PropertyEval> map =
			new HashMap<String, PropertyEval>();
		static {
			map.put("visible", new VisibleEval());
			map.put("alpha", new AlphaEval());
			map.put("fill", new FillEval());
			map.put("stroke", new StrokeEval());
			map.put("left", new LeftEval());
			map.put("right", new RightEval());
			map.put("top", new TopEval());
			map.put("bottom", new BottomEval());
			map.put("width", new WidthEval());
			map.put("height", new HeightEval());
			
			map.put("delay", new DelayEval());
			map.put("ease", new EasingEval());
			
			map.put("shape", new ShapeEval());
			map.put("size", new SizeEval());
			map.put("radius", new RadiusEval());
			
			map.put("url", new URLEval());
			
			map.put("font", new FontEval());
			map.put("text", new TextEval());
			map.put("textBaseline", new TextBaselineEval());
			map.put("textAlign", new TextAlignEval());
			map.put("textAngle", new TextAngleEval());
			map.put("textMargin", new TextMarginEval());
			
			map.put("cache", new CacheEval());
			
			map.put("angle", new AngleEval());
			map.put("startAngle", new StartAngleEval());
			map.put("endAngle", new EndAngleEval());
			map.put("innerRadius", new InnerRadiusEval());
			map.put("OuterRadius", new OuterRadiusEval());
			
			map.put("segmented", new SegmentedEval());
			map.put("interpolate", new InterpolateEval());
			map.put("depth", new DepthEval());
		}
		public static PropertyEval get(String name) {
			return map.get(name);
		}
		public abstract void eval(Item x, Property p);
		
		public static class VisibleEval extends PropertyEval {
			public void eval(Item x, Property p) { x.visible = p.bool(x); }
		}
		
		public static class AlphaEval extends PropertyEval {
			public void eval(Item x, Property p) { x.alpha = p.number(x); }
		}
		public static class FillEval extends PropertyEval {
			public void eval(Item x, Property p) { x.fill = p.fill(x); }
		}
		public static class StrokeEval extends PropertyEval {
			public void eval(Item x, Property p) { x.stroke = p.stroke(x); }
		}
		
		public static class LeftEval extends PropertyEval {
			public void eval(Item x, Property p) { x.left = p.number(x); }
		}
		public static class RightEval extends PropertyEval {
			public void eval(Item x, Property p) { x.right = p.number(x); }
		}
		public static class TopEval extends PropertyEval {
			public void eval(Item x, Property p) { x.top = p.number(x); }
		}
		public static class BottomEval extends PropertyEval {
			public void eval(Item x, Property p) { x.bottom = p.number(x); }
		}
		public static class WidthEval extends PropertyEval {
			public void eval(Item x, Property p) { x.width = p.number(x); }
		}
		public static class HeightEval extends PropertyEval {
			public void eval(Item x, Property p) { x.height = p.number(x); }
		}
		
		public static class DelayEval extends PropertyEval {
			public void eval(Item x, Property p) { x.delay = p.number(x); }
		}
		public static class EasingEval extends PropertyEval {
			public void eval(Item x, Property p) { x.ease = (Easing)p.object(x); }
		}
		
		public static class ShapeEval extends PropertyEval {
			public void eval(Item x, Property p) { ((DotItem)x).shape = p.string(x); }
		}
		public static class SizeEval extends PropertyEval {
			public void eval(Item x, Property p) { ((DotItem)x).size = p.number(x); }
		}
		public static class RadiusEval extends PropertyEval {
			public void eval(Item x, Property p) { ((DotItem)x).radius = p.number(x); }
		}
		
		public static class URLEval extends PropertyEval {
			public void eval(Item x, Property p) { ((ImageItem)x).url = p.string(x); }
		}
		
		public static class FontEval extends PropertyEval {
			public void eval(Item x, Property p) { ((LabelItem)x).font = p.font(x); }
		}
		public static class TextEval extends PropertyEval {
			public void eval(Item x, Property p) { ((LabelItem)x).text = p.string(x); }
		}
		public static class TextBaselineEval extends PropertyEval {
			public void eval(Item x, Property p) { ((LabelItem)x).textBaseline = p.string(x); }
		}
		public static class TextAlignEval extends PropertyEval {
			public void eval(Item x, Property p) { ((LabelItem)x).textAlign = p.string(x); }
		}
		public static class TextAngleEval extends PropertyEval {
			public void eval(Item x, Property p) { ((LabelItem)x).textAngle = p.number(x); }
		}
		public static class TextMarginEval extends PropertyEval {
			public void eval(Item x, Property p) { ((LabelItem)x).textMargin = p.number(x); }
		}
		
		public static class CacheEval extends PropertyEval {
			public void eval(Item x, Property p) { ((PanelItem)x).cache = p.bool(x); }
		}
		
		public static class AngleEval extends PropertyEval {
			public void eval(Item x, Property p) { ((WedgeItem)x).angle = p.number(x); }
		}
		public static class StartAngleEval extends PropertyEval {
			public void eval(Item x, Property p) { ((WedgeItem)x).startAngle = p.number(x); }
		}
		public static class EndAngleEval extends PropertyEval {
			public void eval(Item x, Property p) { ((WedgeItem)x).endAngle = p.number(x); }
		}
		public static class InnerRadiusEval extends PropertyEval {
			public void eval(Item x, Property p) { ((WedgeItem)x).innerRadius = p.number(x); }
		}
		public static class OuterRadiusEval extends PropertyEval {
			public void eval(Item x, Property p) { ((WedgeItem)x).outerRadius = p.number(x); }
		}
		
		public static class SegmentedEval extends PropertyEval {
			public void eval(Item x, Property p) { ((GroupItem)x).segmented = p.bool(x); }
		}
		public static class InterpolateEval extends PropertyEval {
			public void eval(Item x, Property p) { ((GroupItem)x).interpolate = p.string(x); }
		}
		public static class DepthEval extends PropertyEval {
			public void eval(Item x, Property p) { ((GroupItem)x).depth = (int)p.number(x); }
		}
		
		// TODO LINK
	}
}
