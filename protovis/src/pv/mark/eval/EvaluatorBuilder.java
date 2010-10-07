package pv.mark.eval;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;

import pv.mark.Mark;
import pv.mark.Mark.PropertySet;
import pv.mark.property.ConstantProperty;
import pv.mark.property.DynamicProperty;
import pv.mark.property.Property;
import pv.mark.property.VariableProperty;
import pv.scene.GroupItem;
import pv.scene.LinkItem;
import pv.util.Objects;
import pv.util.RuntimeCompiler;
import pv.util.RuntimeCompilerException;
import pv.util.TypeLib;

public class EvaluatorBuilder implements TemplateConstants {
	
	private static EvaluatorBuilder s_instance = null;
	public static EvaluatorBuilder instance() {
		if (s_instance == null) s_instance = new EvaluatorBuilder();
		return s_instance;
	}
	
	public boolean printSource() { return _printSource; }
	public void printSource(boolean b) { _printSource = b; }
	
	public boolean compile() { return _compile; }
	public void compile(boolean b) { _compile = b; }
	
	public boolean cache() { return _cache; }
	public void cache(boolean b) { _cache = b; }
	
	private int _id = 0;
	private boolean _printSource = false;
	private boolean _compile = true;
	private boolean _cache = true;
	private String _declStart = "\t";
	private String _bodyStart = "\t\t\t";
	private String _end = ";\n";
	private String _pkg = "pv.temp";
	
	private RuntimeCompiler<?> compiler = RuntimeCompiler.instance();
	private Map<String,Class<Evaluator>> cache = new HashMap<String,Class<Evaluator>>();
	
	public Evaluator build(Mark mark)
	{
		return _compile ? compile(mark) : instance(mark);
	}
	
	protected Evaluator instance(Mark mark)
	{
		Evaluator eval = new StaticEvaluator(mark);
		
		// if no errors occurred, configure the evaluator
		if (eval != null) {
			Integer hasProps = 0;
			Class<?> type = mark.itemType();
			try {
				Method m = type.getDeclaredMethod("checkProperties", Map.class);
				hasProps = (Integer) m.invoke(null, mark.propertySet().instance);
				Field f = eval.getClass().getField("_props");
				f.set(eval, hasProps);
			} catch (Exception ex) {
				// should never happen
				ex.printStackTrace();
			}
		}
		
		return eval;
	}
	
	@SuppressWarnings("unchecked")
	protected Evaluator compile(Mark mark)
	{
		PropertySet pset = mark.propertySet();
		Class<?> type = mark.itemType();
		Class<?> layer_datatype = getDataType(mark.panel());
		Class<?> proto_datatype = getDataType(mark.proto());
		Class<?> datatype = (Class<?>) getConstant(pset, "datatype");
		
		String pkg = "pv.scene.", itemtype = type.getName();
		if (itemtype.startsWith(pkg)) {
			itemtype = itemtype.substring(pkg.length());
		}
		boolean isLink = (type == LinkItem.class);
		String src = (isLink ? LINK_TEMPLATE : TEMPLATE);
		
		// --
		
		StringBuffer imports = Objects.StringBuffer.get();
		StringBuffer decl = Objects.StringBuffer.get();
		StringBuffer data = Objects.StringBuffer.get();
		StringBuffer varb = Objects.StringBuffer.get();
		StringBuffer insb = Objects.StringBuffer.get();
		StringBuffer setb = Objects.StringBuffer.get();
		StringBuffer entb = Objects.StringBuffer.get();
		StringBuffer extb = Objects.StringBuffer.get();
		StringBuffer hdlr = Objects.StringBuffer.get();
		
		Map<String, Object> members = (Map<String,Object>) Objects.Map.get();
		members.put("mark", mark);
		
		// Import data types as needed
		include(datatype, imports);
		include(layer_datatype, imports);
		
		// -- ITEM KEY --------------------------------------------------------
		
		StringBuffer keyb = Objects.StringBuffer.get();
		property("key", pset.keys.get("key"), "return ", "_", decl, keyb, members);
		src = src.replace(KEY, keyb.toString());
		src = src.replace(HAS_KEY, String.valueOf("{{index}}"!=keyb.toString()));
		Objects.StringBuffer.reclaim(keyb);
		
		// -- VARIABLES -------------------------------------------------------
		
		for (Map.Entry<String, Property> pair : pset.vars.entrySet())
		{
			String name = pair.getKey();			
			Property prop = pair.getValue();
			Class<?> rtype = TypeLib.getBoxedType(prop.returnType());
			String ctype = "Variable";
			String pname = "var_"+name;
			String rtypeName = TypeLib.isPrimitive(rtype) ? rtype.getName() : TypeLib.getTypeString(rtype);
			
			// emit declaration
			decl.append(_declStart)
				.append("public ").append(ctype).append(" ").append(pname);
			decl.append(_end);
			decl.append(_declStart)
				.append("public ").append(rtypeName).append(" ").append(name);
			decl.append(_end);
			
			String vsrc = VAR_TEMPLATE;
			vsrc = vsrc.replace(VARNAME, name);
			vsrc = vsrc.replace(VARTYPE, rtypeName);
			decl.append(vsrc);
			
			members.put(pname, ((VariableProperty)prop).value());
			
			// emit evaluation
			varb.append(_bodyStart)
				.append(name).append(" = (").append(rtypeName).append(") ")
				.append(pname).append(".value()")
				.append(_end);
		}
		
		// -- DATA ------------------------------------------------------------
		
		if (pset.data != null) {
			property("data", pset.data, "return ", "_", decl, data, members);
		} else {
			data.append("\t\treturn null;");
		}
		
		// -- LINK ITEMS ------------------------------------------------------
		
		if (isLink) {
			Map<String,Property> p = pset.keys;
			StringBuffer buf = Objects.StringBuffer.get();
			// source and target nodes
			property("sourceNodes", p.get("sourceNodes"), "return ", "_", decl, buf, members);
			src = src.replace(SOURCE_NODES, buf); buf.delete(0, buf.length());
			property("targetNodes", p.get("targetNodes"), "return ", "_", decl, buf, members);
			src = src.replace(TARGET_NODES, buf); buf.delete(0, buf.length());
			// node source key
			property("sourceNodeKey", p.get("sourceNodeKey"), "return ", "_", decl, buf, members);
			src = src.replace(NODE_SOURCE_KEY, buf); buf.delete(0, buf.length());
			// node target key
			property("targetNodeKey", p.get("targetNodeKey"), "return ", "_", decl, buf, members);
			src = src.replace(NODE_TARGET_KEY, buf); buf.delete(0, buf.length());
			// source key
			property("sourceKey", p.get("sourceKey"), "return ", "_", decl, buf, members);
			src = src.replace(LINK_SOURCE_KEY, buf); buf.delete(0, buf.length());
			// target key
			property("targetKey", p.get("targetKey"), "return ", "_", decl, buf, members);
			src = src.replace(LINK_TARGET_KEY, buf); buf.delete(0, buf.length());
			
			Class<?> source_datatype = getNodeDataType(pset, "sourceNodes", proto_datatype);
			Class<?> target_datatype = getNodeDataType(pset, "targetNodes", proto_datatype);
			include(source_datatype, imports);
			if (source_datatype != target_datatype) include(target_datatype, imports);
			src = src.replace(NODE_SOURCE_DATATYPE, source_datatype.getSimpleName());
			src = src.replace(NODE_TARGET_DATATYPE, target_datatype.getSimpleName());
			Objects.StringBuffer.reclaim(buf);
		}
		
		// -- GROUP and INSTANCE level properties -----------------------------
		
		for (Map<String,Property> props : pset.asList())
		{
			if (props == null) continue;
			
			boolean isGroup = (props == pset.group);
			_bodyStart = (isGroup ? "\t\t" : "\t\t\t");
			Class<?> itype = (isGroup ? GroupItem.class : type);
			StringBuffer body = (isGroup ? setb : 
				props==pset.instance ? insb :
				props==pset.enter ? entb : extb);
			String vp = "_";
			if (props==pset.enter) vp = "_enter_";
			if (props==pset.exit) vp = "_exit_";
			
			for (Map.Entry<String, Property> pair : props.entrySet())
			{
				String name = pair.getKey();
				try { itype.getField(name); }
				catch (Exception e) { continue; }
				property(name, pair.getValue(), null, vp, decl, body, members);
			}
		}
		
		// -- EVENT HANDLERS --------------------------------------------------
		
		int hid = 0;
		for (String name : pset.handlers.keySet()) {
			List<EventHandler> list = pset.handlers.get(name);
			for (EventHandler h : list) {
				if (h instanceof DynamicEventHandler) {
					String code = ((DynamicEventHandler)h).code();
					int id = hid++;
					String hsrc = TemplateConstants.HANDLER_TEMPLATE;
					hsrc = hsrc.replace(BODY, code);
					hsrc = hsrc.replace(ID, String.valueOf(id));
					hdlr.append(hsrc);
				}
			}
		}
		
		// -- ASSEMBLE TEMPLATE -----------------------------------------------
		
		src = src.replace(IMPORTS, imports);
		src = src.replace(ITEMTYPE, itemtype);
		src = src.replace(DATATYPE, datatype.getSimpleName());
		src = src.replace(PANEL_DATATYPE, layer_datatype.getSimpleName());
		src = src.replace(DECL, decl);
		src = src.replace(DATA, data);
		src = src.replace(VARS, varb);
		src = src.replace(INSTANCE, insb);
		src = src.replace(ENTER, entb);
		src = src.replace(EXIT, extb);
		src = src.replace(GROUP, setb);
		src = src.replace(HANDLERS, hdlr);
		
		Objects.StringBuffer.reclaim(imports);
		Objects.StringBuffer.reclaim(data);
		Objects.StringBuffer.reclaim(decl);
		Objects.StringBuffer.reclaim(varb);
		Objects.StringBuffer.reclaim(insb);
		Objects.StringBuffer.reclaim(setb);
		Objects.StringBuffer.reclaim(entb);
		Objects.StringBuffer.reclaim(extb);
		Objects.StringBuffer.reclaim(hdlr);
		
		
		// -- COMPILE AND CONFIGURE -------------------------------------------
		
		// look for compiled class in cache
		Class<Evaluator> evalClass = _cache ? cache.get(src) : null;
		Evaluator eval = null;

		// compile if no cached version is found
		if (evalClass == null)
		{
			// generate class name
			String key = src;
			String className = "Evaluator_"+(_id++);
			src = src.replace(CLASSNAME, className);
			if (_printSource) System.out.println(src);
			
			// compile
			try {
				evalClass = (Class<Evaluator>) compiler.compile(_pkg+"."+className, src);
				eval = evalClass.newInstance();
				if (_cache) cache.put(key, evalClass);
			} catch (RuntimeCompilerException ex) {
				DiagnosticCollector<?> diag = ex.getDiagnostics();
				for (Diagnostic<?> d : diag.getDiagnostics()) {
					System.err.println(d);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// create new instance
			try {
				eval = evalClass.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// if no errors occurred, configure the evaluator
		if (eval != null) {
			Integer hasProps = 0;
			try {
				Method m = type.getDeclaredMethod("checkProperties", Map.class);
				hasProps = (Integer) m.invoke(null, pset.instance);
			} catch (Exception ex) {
				// should never happen
				ex.printStackTrace();
			}
			members.put("_props", hasProps);
			if (!members.isEmpty()) RuntimeCompiler.configure(eval, members);
			
			// extract event handlers
			hid = 0;
			Class<?> evaltype = eval.getClass();
			for (String name : pset.handlers.keySet()) {
				List<EventHandler> list = pset.handlers.get(name);
				for (EventHandler h : list) {
					if (h instanceof DynamicEventHandler) {
						int id = hid++;
						Field hf;
						try {
							hf = evaltype.getDeclaredField("handler_"+id);
							EventHandler hc = (EventHandler) hf.get(eval);
							((DynamicEventHandler)h).handler(hc);
						} catch (Exception e) {
							// shouldn't happen
							e.printStackTrace();
						}
					}
				}
			}
		}
		Objects.Map.reclaim(members);
		return eval;
	}
	
	private static Class<?> getDataType(Mark mark) {
		if (mark != null) {
			Evaluator eval = mark.evaluator();
			if (eval != null)
				return eval.datatype();
		}
		return Object.class;
	}
	
	private static Class<?> getNodeDataType(PropertySet pset, String name, Class<?> proto) {
		Property p = (Property) pset.keys.get(name);
		if (p == null) return proto;
		
		if (p instanceof DynamicProperty) {
			DynamicProperty d = (DynamicProperty)p;
			String line = d.lines()[d.lines().length-1];
			if ("item.proto".equals(line)) {
				return proto;
			}
		}
		if (p instanceof ConstantProperty) {
			Object c = ((ConstantProperty)p).value();
			if (c instanceof Mark) {
				return getDataType((Mark) c);
			}
			if (c instanceof GroupItem) {
				// TODO this is crash-prone. Need GroupItem.datatype field?
				return ((GroupItem)c).item(0).data.getClass();
			}
		}
		return Object.class;
	}
	
	private static Object getConstant(PropertySet pset, String name) {
		Property p = (Property) pset.group.get(name);
		return p != null && p instanceof ConstantProperty ?
			((ConstantProperty)p).value() : null;
	}
	
	private void include(Class<?> type, StringBuffer imports) {
		Class<?> c = type.isArray() ? type.getComponentType() : type;
		if (!TypeLib.isPrimitive(c) && TypeLib.needsImport(c)) {
			String typeName = c.getName().replace("$", ".");
			imports.append("import ").append(typeName).append(_end);
		}
	}
	
	private void property(String name, Property prop, String prefix, String varPrefix,
		StringBuffer decl, StringBuffer body, Map<String,Object> members)
	{
		if (prop == null) {
			body.append(_bodyStart);
			if (prefix == null) {
				body.append("item.").append(name).append(" = null");
			} else {
				body.append(prefix).append("null");
			}
			body.append(_end);
			return;
		}
		
		Class<?> rtype = prop.returnType();
		String pname = varPrefix + name;
		
		switch (prop.type()) {
		case CONSTANT:
			// get constant type
			String ctype = TypeLib.getTypeString(rtype);
			
			// emit declaration
			decl.append(_declStart)
				.append("public ").append(ctype).append(" ").append(pname);
			if (TypeLib.isPrimitive(rtype)) {
				String value = prop.toString();
				decl.append(" = ").append(value).append(_end);
			} else {
				decl.append(_end);
				members.put(pname, ((ConstantProperty)prop).value());
			}
			
			// emit evaluation
			// TODO add dirty check?
			body.append(_bodyStart);
			if (prefix == null) {
				body.append("item.").append(name).append(" = ").append(pname);
			} else {
				body.append(prefix).append(pname);
			}
			body.append(_end);
			break;
			
		case COMPILED:
			// emit declaration
			decl.append(_declStart)
				.append("public Property ").append(pname)
				.append(_end);
			members.put(pname, prop);
			
			// get property method to invoke
			String method = TypeLib.getMethodString(rtype);
			
			// emit evaluation
			// TODO add dirty check
			body.append(_bodyStart);
			if (prefix == null) {
				body.append("item.").append(name).append(" = ");
			} else {
				body.append(prefix);
			}
			body.append(pname).append(".").append(method).append("(item)");
			body.append(_end);
			break;
			
		case DYNAMIC:
			// TODO register dependencies
			// TODO pre-process property string?
			String[] lines = ((DynamicProperty)prop).lines();
			int len = lines.length - 1;
			if (len > 0) {
				body.append(_bodyStart).append("{\n");
				for (int i=0; i<len; ++i) {
					body.append(_bodyStart).append("\t")
						.append(lines[i])
						.append(_end);
				}
			}
			body.append(_bodyStart).append(len>0 ? "\t" : "");
			if (prefix == null) {
				body.append("item.").append(name).append(" = ");
			} else {
				body.append(prefix);
			}
			body.append(lines[len])
				.append(_end);
			if (len > 0) body.append(_bodyStart).append("}\n");
			break;
		}
	}
	
}
