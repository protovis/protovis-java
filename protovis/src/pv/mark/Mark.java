package pv.mark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pv.mark.constants.Events;
import pv.mark.constants.Interpolate;
import pv.mark.constants.MarkType;
import pv.mark.constants.Shape;
import pv.mark.constants.TextAlign;
import pv.mark.constants.TextBaseline;
import pv.mark.eval.DynamicEventHandler;
import pv.mark.eval.Evaluator;
import pv.mark.eval.EventHandler;
import pv.mark.property.ConstantProperty;
import pv.mark.property.DynamicProperty;
import pv.mark.property.Property;
import pv.mark.property.VariableProperty;
import pv.scene.GroupItem;
import pv.scene.Item;
import pv.style.Easing;
import pv.style.Fill;
import pv.style.Font;
import pv.style.Stroke;

public class Mark {

	// -- static --------------------------------------------------------------
	
	protected static Set<String> SET_LEVEL = new HashSet<String>(
		Arrays.asList("data","datatype","depth","segmented","interpolate"));
	protected static Set<String> KEY_LEVEL = new HashSet<String>(
		Arrays.asList("key", "sourceNodes", "targetNodes",
			"sourceNodeKey", "targetNodeKey", "sourceKey", "targetKey"));
	
	public static final Map<String,Set<String>> PROP_VALUES
		= new HashMap<String,Set<String>>();
	
	protected static Map<String,Definition> MARK_TYPES
		= new HashMap<String,Definition>();
	
	static {		
		Set<String> textBaseline = new HashSet<String>();
		textBaseline.add(TextBaseline.Bottom);
		textBaseline.add(TextBaseline.Middle);
		textBaseline.add(TextBaseline.Top);
		registerProperty("textBaseline", textBaseline);
		
		Set<String> textAlign = new HashSet<String>();
		textAlign.add(TextAlign.Center);
		textAlign.add(TextAlign.Left);
		textAlign.add(TextAlign.Right);
		registerProperty("textAlign", textAlign);
		
		Set<String> shapes = new HashSet<String>();
		shapes.add(Shape.Circle);
		shapes.add(Shape.Square);
		shapes.add(Shape.Triangle);
		shapes.add(Shape.Cross);
		shapes.add(Shape.X);
		shapes.add(Shape.Diamond);
		shapes.add(Shape.Point);
		registerProperty("shape", shapes);
		
		Set<String> interpolate = new HashSet<String>();
		interpolate.add(Interpolate.Linear);
		interpolate.add(Interpolate.StepAfter);
		interpolate.add(Interpolate.StepBefore);
		registerProperty("interpolate", interpolate);
		
		register(MarkType.Area,  new Definition.Area());
		register(MarkType.Bar,   new Definition.Bar());
		register(MarkType.Dot,   new Definition.Dot());
		register(MarkType.Image, new Definition.Image());
		register(MarkType.Label, new Definition.Label());
		register(MarkType.Panel, new Definition.Panel());
		register(MarkType.Link,  new Definition.Link());
		register(MarkType.Line,  new Definition.Line());
		register(MarkType.Rule,  new Definition.Rule());
		register(MarkType.Wedge, new Definition.Wedge());
	}
	
	public static void register(String markType, Definition def) {
		MARK_TYPES.put(markType, def);
	}
	
	public static Definition definition(String markType) {
		return MARK_TYPES.get(markType);
	}
	
	public static void registerProperty(String name, Set<String> values) {
		PROP_VALUES.put(name, values);
	}
	
	public static boolean isValid(String propertyName, String value) {
		Set<String> set = PROP_VALUES.get(propertyName);
		return set==null ? false : set.contains(value) ? true : false;
	}
	
	public static Mark create() {
		return new Mark();
	}
	
	// -- Members -------------------------------------------------------------
		
	protected LinkedHashMap<String,Property> _props = 
		new LinkedHashMap<String,Property>();
	protected LinkedHashMap<String,Property> _vars =
		new LinkedHashMap<String,Property>();
	protected HashMap<String,List<EventHandler>> _handlers =
		new HashMap<String,List<EventHandler>>();

	protected String _marktype;
	protected Definition _defn;
	protected Scene _scene;
	protected Panel _panel;
	protected Evaluator _eval;
	protected int _treeIndex, _panelSize = 0;
	
	protected PropertySet _pset = new PropertySet();
	
	protected boolean _hasProto = false;
	protected List<Mark> _proto = new ArrayList<Mark>();
	protected List<Mark> _marks = new ArrayList<Mark>();
	
	public int treeIndex() { return _treeIndex; }
	public Mark treeIndex(int idx) { _treeIndex = idx; return this; }
	
	public int panelSize() { return _panelSize; }
	
	public String markType() { return _marktype; }
	public Class<?> itemType() { return _defn==null ? null : _defn.itemType(); }
	
	public Scene scene() { return _scene; }
	public Panel panel() { return _panel; }
	public Mark  proto() { return _proto.size() > 1 ? _proto.get(1) : null; }
	
	public PropertySet propertySet() { return _pset; }
	public Evaluator evaluator() { return _eval; }
	public Mark evaluator(Evaluator eval) { _eval = eval; return this; }
	
	public Map<String,List<EventHandler>> handlers() {
		return Collections.unmodifiableMap(_handlers);
	}
	
	public Iterable<Mark> children() { return _marks; }
	
	// -- Mark construction ---------------------------------------------------
	
	protected Mark() {
		this(null);
	}
	
	protected Mark(String marktype) {
		// TODO check type
		_marktype = marktype;
		if (marktype != null) {
			_defn = definition(marktype);
			_proto.add(_defn.defaults());
		}
	}
	
	public Mark add(String type) {
		Mark m = (type == MarkType.Panel ? new Panel() : new Mark(type));
		if (_marktype != MarkType.Panel) {
			m._proto.add(this);
			m._hasProto = true;
		}
		m._panel = (_marktype==MarkType.Panel ? (Panel)this : _panel);
		m._scene = _scene;
		_marks.add(m);
		return m;
	}
	
	public Mark extend(Mark mark) {
		_proto.add(mark);
		return this;
	}
	
	public Item items() {
		return null;
	}
	
	public void update() {
		// TODO?
	}
	
	public PropertySet bind() {
		LinkedHashMap<String, Property> props,
			set = new LinkedHashMap<String, Property>(),
			ins = new LinkedHashMap<String, Property>(),
			key = null;
		PropertySet pset = new PropertySet();
		Map<String,List<EventHandler>> handlers = new HashMap<String,List<EventHandler>>();
		
		List<Mark> chain = new ArrayList<Mark>();
		chain.addAll(_proto);
		chain.add(this);
		
		// traverse the prototype chain to collect all bound properties
		// currently the super-prototype chain only examines the first ancestor
		// TODO optimize to enable sub-chain caching and sharing?
		for (int i=chain.size(); --i>=0;) {
			Mark p = chain.get(i);
			
			// collect relevant event handlers
			if (!(_hasProto && i==1)) {
				for (Map.Entry<String,List<EventHandler>> e : p.handlers().entrySet())
				{
					List<EventHandler> list = handlers.get(e.getKey());
					if (list == null) {
						list = new ArrayList<EventHandler>();
						handlers.put(e.getKey(), list);
					}
					list.addAll(e.getValue());
				}
			}
			
			// collect property definitions
			for (; p != null; p = p.proto()) {
				for (Map.Entry<String,Property> e : p.properties()) {
					String name = e.getKey();
					// get the correct property set
					props = ins;
					if (SET_LEVEL.contains(name)) {
						props = set;
					} else if (KEY_LEVEL.contains(name)) {
						if (key == null) key = new LinkedHashMap<String, Property>();
						props = key;
					}
					// check set for presence of property definition
					if (!props.containsKey(e.getKey())) {
						props.put(e.getKey(), e.getValue());
					}
				}
				if (p == this) break;
			}
		}
		
		// ensure data property is the last in the list
		Property data = set.remove("data");
		if (data != null) pset.data = data;
		
		// TODO incremental check for dirty property bindings?
		pset.handlers = handlers;
		pset.group = set;
		pset.instance = ins;
		pset.vars = _vars; // TODO inherit variables?
		pset.keys = key;
		pset.enter = _pset.enter;
		pset.exit = _pset.exit;
		pset.dirty = !pset.equals(_pset);
		_pset = pset;
		return pset;
	}
	
	protected int setTreeIndex(int idx) {
		this._treeIndex = ++idx;
		if (_marktype == MarkType.Panel) {
			int nidx = -1;
			for (int midx=0; midx<_marks.size(); ++midx) {
				nidx = _marks.get(midx).setTreeIndex(nidx);
			}
			this._panelSize = nidx + 1;
		} else {
			for (int midx=0; midx<_marks.size(); ++midx) {
				idx = _marks.get(midx).setTreeIndex(idx);
			}
		}
		return idx;
	}
	
	
	// -- Event Handlers ------------------------------------------------------
	
	public Mark event(String name, EventHandler handler) {
		List<EventHandler> list = _handlers.get(name);
		if (list == null) {
			_handlers.put(name, (list = new ArrayList<EventHandler>()));
		}
		list.add(handler);
		return this;
	}
	public Mark mouseEnter(EventHandler handler) { return event(Events.mouseEnter, handler); }
	public Mark mouseExit(EventHandler handler) { return event(Events.mouseExit, handler); }
	public Mark mousePress(EventHandler handler) { return event(Events.mousePress, handler); }
	public Mark mouseRelease(EventHandler handler) { return event(Events.mouseRelease, handler); }
	public Mark mouseClick(EventHandler handler) { return event(Events.mouseClick, handler); }
	public Mark mouseMove(EventHandler handler) { return event(Events.mouseMove, handler); }
	public Mark mouseDrag(EventHandler handler) { return event(Events.mouseDrag, handler); }
	public Mark mouseWheel(EventHandler handler) { return event(Events.mouseWheel, handler); }
	public Mark keyPress(EventHandler handler) { return event(Events.keyPress, handler); }
	public Mark keyRelease(EventHandler handler) { return event(Events.keyRelease, handler); }
	public Mark keyType(EventHandler handler) { return event(Events.keyType, handler); }
	public Mark onBuild(EventHandler handler) { return event(Events.build, handler); }
	public Mark sceneUpdate(EventHandler handler) { _scene.event(Events.update, handler); return this; }
	
	public Mark event(String name, String code) {
		return event(name, DynamicEventHandler.create(code));
	}
	public Mark mouseEnter(String code) { return event(Events.mouseEnter, code); }
	public Mark mouseExit(String code) { return event(Events.mouseExit, code); }
	public Mark mousePress(String code) { return event(Events.mousePress, code); }
	public Mark mouseRelease(String code) { return event(Events.mouseRelease, code); }
	public Mark mouseClick(String code) { return event(Events.mouseClick, code); }
	public Mark mouseMove(String code) { return event(Events.mouseMove, code); }
	public Mark mouseDrag(String code) { return event(Events.mouseDrag, code); }
	public Mark mouseWheel(String code) { return event(Events.mouseWheel, code); }
	public Mark keyPress(String code) { return event(Events.keyPress, code); }
	public Mark keyRelease(String code) { return event(Events.keyRelease, code); }
	public Mark keyType(String code) { return event(Events.keyType, code); }
	public Mark onBuild(String code) { return event(Events.build, code); }
	public Mark sceneUpdate(String code) { _scene.event(Events.update, code); return this; }
	
	// -- Properties ----------------------------------------------------------
	
	public Mark def(String name, Object value) {
		// TODO: ensure no name collision
		if (value instanceof String) {
			String v = (String) value;
			if (DynamicProperty.isDynamicProperty(v)) {
				_vars.put(name, dynamic(v));
			} else {
				_vars.put(name, constant(v));
			}
		} else if (value instanceof Property) {
			_vars.put(name, (Property) value);
		} else if (value instanceof Variable) {
			_vars.put(name, variable((Variable) value));
		} else {
			_vars.put(name, variable(new Variable(value)));
		}
		return this;
	}
	
	// ---
	
	public Mark datatype(Class<?> c) { return set("datatype", constant(c)); }
	
	public Mark data(Property p) { return set("data", p); }
	public Mark data(Object o) { return set("data", constant(o)); }
	public Mark data(String s) { return set("data", dynamic(s)); }
	
	public Mark init(Property p) { return set("init", p); }
	public Mark init(String s) { return set("init", dynamic(s)); }
	
	public Mark depth(Property p) { return set("depth", p); }
	public Mark depth(int x) { return set("depth", constant(x, int.class)); }
	public Mark depth(String s) { return set("depth", dynamic(s, int.class)); }
	
	public Mark cache(Property p) { return set("cache", p); }
	public Mark cache(boolean b) { return set("cache", constant(b, boolean.class)); }
	public Mark cache(String s) { return set("cache", dynamic(s, boolean.class)); }
	
	public Mark segmented(Property p) { return set("segmented", p); }
	public Mark segmented(boolean b) { return set("segmented", constant(b, boolean.class)); }
	public Mark segmented(String s) { return set("segmented", dynamic(s, boolean.class)); }
	
	public Mark interpolate(Property p) { return set("interpolate", p); }
	public Mark interpolate(String s) {
		if (DynamicProperty.isDynamicProperty(s)) {
			return set("interpolate", dynamic(s, String.class));
		} else if (isValid("interpolate", s)) {
			return set("interpolate", constant(s));
		} else {
			throw new IllegalArgumentException("Illegal interpolate: "+s);
		}
	}
	
	// ---
	
	public Mark key(Property p) { return set("key", p); }
	public Mark key(String s) { return set("key", dynamic(s, Object.class)); }
	
	public Mark visible(Property p) { return set("visible", p); }
	public Mark visible(boolean b) { return set("visible", constant(b, boolean.class)); }
	public Mark visible(String s) { return set("visible", dynamic(s, boolean.class)); }
	
	public Mark left(Property p) { return set("left", p); }
	public Mark left(double x) { return set("left", constant(x, double.class)); }
	public Mark left(int x) { return set("left", constant(x, int.class)); }
	public Mark left(String s) { return set("left", dynamic(s, double.class)); }
	
	public Mark right(Property p) { return set("right", p); }
	public Mark right(double x) { return set("right", constant(x, double.class)); }
	public Mark right(int x) { return set("right", constant(x, int.class)); }
	public Mark right(String s) { return set("right", dynamic(s, double.class)); }
	
	public Mark top(Property p) { return set("top", p); }
	public Mark top(double x) { return set("top", constant(x, double.class)); }
	public Mark top(int x) { return set("top", constant(x, int.class)); }
	public Mark top(String s) { return set("top", dynamic(s, double.class)); }
	
	public Mark bottom(Property p) { return set("bottom", p); }
	public Mark bottom(double x) { return set("bottom", constant(x, double.class)); }
	public Mark bottom(int x) { return set("bottom", constant(x, int.class)); }
	public Mark bottom(String s) { return set("bottom", dynamic(s, double.class)); }
	
	public Mark width(Property p) { return set("width", p); }
	public Mark width(double x) { return set("width", constant(x, double.class)); }
	public Mark width(int x) { return set("width", constant(x, int.class)); }
	public Mark width(String s) { return set("width", dynamic(s, double.class)); }
	
	public Mark height(Property p) { return set("height", p); }
	public Mark height(double x) { return set("height", constant(x, double.class)); }
	public Mark height(int x) { return set("height", constant(x, int.class)); }
	public Mark height(String s) { return set("height", dynamic(s, double.class)); }
	
	// ---
	
	public Mark delay(Property p) { return set("delay", p); }
	public Mark delay(double x) { return set("delay", constant(x, double.class)); }
	public Mark delay(String s) { return set("delay", dynamic(s, double.class)); }
	
	public Mark ease(Property p) { return set("ease", p); }
	public Mark ease(Easing e) { return set("ease", constant(e, Easing.class)); }
	public Mark ease(String s) { return set("ease", dynamic(s, Easing.class)); }
	
	// ---
	
	public Mark size(Property p) { return set("size", p); }
	public Mark size(double x) { return set("size", constant(x, double.class)); }
	public Mark size(int x) { return set("size", constant(x, int.class)); }
	public Mark size(String s) { return set("size", dynamic(s, double.class)); }
	
	// ---
	
	public Mark url(Property p) { return set("url", p); }
	public Mark url(String s) {
		boolean d = DynamicProperty.isDynamicProperty(s);
		return set("url", d ? dynamic(s, String.class) : constant(s, String.class));
	}
	
	// ---
	
	public Mark innerRadius(Property p) { return set("innerRadius", p); }
	public Mark innerRadius(double x) { return set("innerRadius", constant(x, double.class)); }
	public Mark innerRadius(int x) { return set("innerRadius", constant(x, int.class)); }
	public Mark innerRadius(String s) { return set("innerRadius", dynamic(s, double.class)); }
	
	public Mark outerRadius(Property p) { return set("outerRadius", p); }
	public Mark outerRadius(double x) { return set("outerRadius", constant(x, double.class)); }
	public Mark outerRadius(int x) { return set("outerRadius", constant(x, int.class)); }
	public Mark outerRadius(String s) { return set("outerRadius", dynamic(s, double.class)); }
	
	public Mark startAngle(Property p) { return set("startAngle", p); }
	public Mark startAngle(double x) { return set("startAngle", constant(x, double.class)); }
	public Mark startAngle(int x) { return set("startAngle", constant(x, int.class)); }
	public Mark startAngle(String s) { return set("startAngle", dynamic(s, double.class)); }
	
	public Mark endAngle(Property p) { return set("endAngle", p); }
	public Mark endAngle(double x) { return set("endAngle", constant(x, double.class)); }
	public Mark endAngle(int x) { return set("endAngle", constant(x, int.class)); }
	public Mark endAngle(String s) { return set("endAngle", dynamic(s, double.class)); }
	
	public Mark angle(Property p) { return set("angle", p); }
	public Mark angle(double x) { return set("angle", constant(x, double.class)); }
	public Mark angle(int x) { return set("angle", constant(x, int.class)); }
	public Mark angle(String s) { return set("angle", dynamic(s, double.class)); }
	
	// ---
	
	public Mark alpha(Property p) { return set("alpha", p); }
	public Mark alpha(double x) { return set("alpha", constant(x, double.class)); }
	public Mark alpha(String s) { return set("alpha", dynamic(s, double.class)); }
	
	public Mark fill(Property p) { return set("fill", p); }
	public Mark fill(Fill f) { return set("fill", constant(f, Fill.class)); }
	public Mark fill(String s) {
		boolean d = DynamicProperty.isDynamicProperty(s);
		return set("fill", d ? dynamic(s, Fill.class) : constant(Fill.fill(s), Fill.class));
	}
	
	public Mark stroke(Property p) { return set("stroke", p); }
	public Mark stroke(Stroke s) { return set("stroke", constant(s, Stroke.class)); }
	public Mark stroke(String s) {
		boolean d = DynamicProperty.isDynamicProperty(s);
		return set("stroke", d ? dynamic(s, Stroke.class) : constant(Stroke.stroke(s), Stroke.class));
	}

	public Mark shape(Property p) { return set("shape", p); }
	public Mark shape(String s) {
		if (DynamicProperty.isDynamicProperty(s)) {
			return set("shape", dynamic(s, String.class));
		} else if (isValid("shape", s)) {
			return set("shape", constant(s));
		} else {
			throw new IllegalArgumentException("Illegal shape: "+s);
		}
	}
	
	public Mark font(Property p) { return set("font", p); }
	public Mark font(Font f) { return set("font", constant(f, Font.class)); }
	public Mark font(String s) {
		boolean d = DynamicProperty.isDynamicProperty(s);
		return set("font", d ? dynamic(s, Font.class) : constant(Font.font(s), Font.class));
	}
	
	public Mark text(Property p) { return set("text", p); }
	public Mark text(String s) {
		boolean d = DynamicProperty.isDynamicProperty(s);
		return set("text", d ? dynamic(s, String.class) : constant(s, String.class));
	}
	
	public Mark textAlign(Property p) { return set("textAlign", p); }
	public Mark textAlign(String s) {
		if (DynamicProperty.isDynamicProperty(s)) {
			return set("textAlign", dynamic(s, String.class));
		} else if (isValid("textAlign", s)) {
			return set("textAlign", constant(s));
		} else {
			throw new IllegalArgumentException("Illegal textAlign: "+s);
		}
	}
	
	public Mark textBaseline(Property p) { return set("textAlign", p); }
	public Mark textBaseline(String s) {
		if (DynamicProperty.isDynamicProperty(s)) {
			return set("textBaseline", dynamic(s, String.class));
		} else if (isValid("textBaseline", s)) {
			return set("textBaseline", constant(s));
		} else {
			throw new IllegalArgumentException("Illegal textBaseline: "+s);
		}
	}
	
	public Mark textAngle(Property p) { return set("textAngle", p); }
	public Mark textAngle(double x) { return set("textAngle", constant(x, double.class)); }
	public Mark textAngle(int x) { return set("textAngle", constant(x, int.class)); }
	public Mark textAngle(String s) { return set("textAngle", dynamic(s, double.class)); }
	
	public Mark textMargin(Property p) { return set("textMargin", p); }
	public Mark textMargin(double x) { return set("textMargin", constant(x, double.class)); }
	public Mark textMargin(int x) { return set("textMargin", constant(x, int.class)); }
	public Mark textMargin(String s) { return set("textMargin", dynamic(s, double.class)); }

	// ---
	
	public Mark sourceNodes(Property p) { return set("sourceNodes", p); }
	public Mark sourceNodes(String s) { return set("sourceNodes", dynamic(s)); }
	public Mark sourceNodes(Mark m) { return set("sourceNodes", constant(m)); }
	public Mark sourceNodes(GroupItem g) { return set("sourceNodes", constant(g)); }
	
	public Mark targetNodes(Property p) { return set("targetNodes", p); }
	public Mark targetNodes(String s) { return set("targetNodes", dynamic(s)); }
	public Mark targetNodes(Mark m) { return set("targetNodes", constant(m)); }
	public Mark targetNodes(GroupItem g) { return set("targetNodes", constant(g)); }
	
	public Mark nodes(Property p) {
		set("sourceNodes", p); return set("targetNodes", p);
	}
	public Mark nodes(String s) { return nodes(dynamic(s)); }
	public Mark nodes(Mark m) { return nodes(constant(m)); }
	public Mark nodes(GroupItem g) { return nodes(constant(g)); }
	
	public Mark sourceNodeKey(Property p) { return set("sourceNodeKey", p); }
	public Mark sourceNodeKey(String s) { return set("sourceNodeKey", dynamic(s)); }
	
	public Mark targetNodeKey(Property p) { return set("targetNodeKey", p); }
	public Mark targetNodeKey(String s) { return set("targetNodeKey", dynamic(s)); }
	
	public Mark nodeKey(Property p) {
		set("sourceNodeKey", p); return set("targetNodeKey", p);
	}
	public Mark nodeKey(String s) { return nodeKey(dynamic(s)); }
	
	public Mark sourceKey(Property p) { return set("sourceKey", p); }
	public Mark sourceKey(String s) { return set("sourceKey", dynamic(s)); }
	
	public Mark targetKey(Property p) { return set("targetKey", p); }
	public Mark targetKey(String s) { return set("targetKey", dynamic(s)); }
	
	// ---
	
	public Mark enter(Mark props) {
		_pset.enter.clear();
		_pset.enter.put("alpha", dynamic("{{0}}", double.class));
		if (props != null) {
			for (Map.Entry<String,Property> entry : props.properties()) {
				_pset.enter.put(entry.getKey(), entry.getValue());
			}
		}
		return this;
	}
	public Mark exit(Mark props) {
		_pset.exit.clear();
		_pset.exit.put("alpha", dynamic("{{0}}", double.class));
		if (props != null) {
			for (Map.Entry<String,Property> entry : props.properties()) {
				_pset.exit.put(entry.getKey(), entry.getValue());
			}
		}
		return this;
	}
	
	// ---
	
	private static Property constant(Object o) {
		return o==null ? null : ConstantProperty.create(o);
	}
	
	private static Property constant(Object o, Class<?> rtype) {
		return o==null ? null : ConstantProperty.create(o, rtype);
	}
	
	private static Property dynamic(String s) {
		return s==null ? null : DynamicProperty.create(s, Object.class);
	}
	
	private static Property dynamic(String s, Class<?> rtype) {
		return s==null ? null : DynamicProperty.create(s, rtype);
	}
	
	private static Property variable(Object o) {
		return o==null ? null : new VariableProperty(o);
	}
	
	public Property property(String name) {
		return _props.get(name);
	}
	
	public Mark set(String name, String s, Class<?> type) {
		Property p = null;
		if (DynamicProperty.isDynamicProperty(s)) {
			p = dynamic(s, type);
		} else {
			p = constant(s, String.class);
		}
		return set(name, p);
	}
	
	public Mark set(String name, String s) {
		Property p = null;
		if (DynamicProperty.isDynamicProperty(s)) {
			p = dynamic(s);
		} else {
			p = constant(s, String.class);
		}
		return set(name, p);
	}
	
	public Mark set(String name, Property p) {
		_props.put(name, p);
		return this;
	}
	
	public Property remove(String name) {
		return _props.remove(name);
	}
	
	public Iterable<Map.Entry<String, Property>> properties() {
		return _props.entrySet();
	}
	
	// -----
	
	public static class PropertySet {
		public Property data;
		public Map<String,Property> group;
		public Map<String,Property> instance;
		public Map<String,Property> vars;
		public Map<String,Property> keys;
		public Map<String,Property> enter;
		public Map<String,Property> exit;
		public Map<String,List<EventHandler>> handlers;
		public boolean dirty = true;
		
		public PropertySet() {
			Property zero = dynamic("{{0}}", double.class);
			enter = new HashMap<String,Property>();
			enter.put("alpha", zero);
			exit = new HashMap<String,Property>();
			exit.put("alpha", zero);
		}
		
		public boolean equals(PropertySet pset) {
			if (pset == null || pset.group == null || pset.instance == null ||
				group.size() != pset.group.size() ||
				instance.size() != pset.instance.size())
				return false;
			
			if (data != pset.data) {
				return false;
			}
			for (Map.Entry<String, Property> e : group.entrySet()) {
				if (e.getValue() != pset.group.get(e.getKey()))
					return false;
			}
			for (Map.Entry<String, Property> e : instance.entrySet()) {
				if (e.getValue() != pset.instance.get(e.getKey()))
					return false;
			}
			for (Map.Entry<String, Property> e : vars.entrySet()) {
				if (e.getValue() != pset.vars.get(e.getKey()))
					return false;
			}
			if (keys != pset.keys) {
				if (keys == null || pset.keys == null)
					return false;
				for (Map.Entry<String, Property> e : keys.entrySet()) {
					if (e.getValue() != pset.keys.get(e.getKey()))
						return false;
				}
			}
			if (enter != pset.enter) {
				if (enter == null || pset.enter == null)
					return false;
				for (Map.Entry<String, Property> e : enter.entrySet()) {
					if (e.getValue() != pset.enter.get(e.getKey()))
						return false;
				}
			}
			if (exit != pset.exit) {
				if (exit == null || pset.exit == null)
					return false;
				for (Map.Entry<String, Property> e : exit.entrySet()) {
					if (e.getValue() != pset.exit.get(e.getKey()))
						return false;
				}
			}
			return true;
		}
		
		@SuppressWarnings("unchecked")
		public List<Map<String,Property>> asList() {
			return Arrays.asList(group, instance, enter, exit);
		}
	}
}
