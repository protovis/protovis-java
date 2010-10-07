package pv.util;

import java.util.HashMap;
import java.util.Map;

import pv.mark.constants.MarkType;
import pv.style.Fill;
import pv.style.Font;
import pv.style.Stroke;

public class TypeLib {

	private TypeLib() {
        // disallow instantiation
    }
	
	public static final Map<Class<?>,String> METHOD_MAP = new HashMap<Class<?>,String>();
	static {
		METHOD_MAP.put(boolean.class, "bool");
		METHOD_MAP.put(Boolean.class, "bool");
		METHOD_MAP.put(double.class, "number");
		METHOD_MAP.put(Double.class, "number");
		METHOD_MAP.put(int.class, "number");
		METHOD_MAP.put(Integer.class, "number");
		METHOD_MAP.put(long.class, "number");
		METHOD_MAP.put(Long.class, "number");
		METHOD_MAP.put(String.class, "string");
		METHOD_MAP.put(Fill.class, "fill");
		METHOD_MAP.put(Stroke.class, "stroke");
		METHOD_MAP.put(Font.class, "font");
	}
	
	public static final Map<Class<?>,String> TYPE_MAP = new HashMap<Class<?>,String>();
	static {
		TYPE_MAP.put(boolean.class, "boolean");
		TYPE_MAP.put(Boolean.class, "boolean");
		TYPE_MAP.put(float.class, "float");
		TYPE_MAP.put(Float.class, "float");
		TYPE_MAP.put(double.class, "double");
		TYPE_MAP.put(Double.class, "double");
		TYPE_MAP.put(int.class, "int");
		TYPE_MAP.put(Integer.class, "int");
		TYPE_MAP.put(long.class, "long");
		TYPE_MAP.put(Long.class, "long");
		TYPE_MAP.put(String.class, "String");
		TYPE_MAP.put(Fill.class, "Fill");
		TYPE_MAP.put(Stroke.class, "Stroke");
		TYPE_MAP.put(Font.class, "Font");
	}
	
	public static final Map<Class<?>,Class<?>> PRIMITIVE_MAP = new HashMap<Class<?>,Class<?>>();
	static {
		PRIMITIVE_MAP.put(boolean.class, Boolean.class);
		PRIMITIVE_MAP.put(Boolean.class, Boolean.class);
		PRIMITIVE_MAP.put(byte.class, Byte.class);
		PRIMITIVE_MAP.put(Byte.class, Byte.class);
		PRIMITIVE_MAP.put(short.class, Short.class);
		PRIMITIVE_MAP.put(Short.class, Short.class);
		PRIMITIVE_MAP.put(int.class, Integer.class);
		PRIMITIVE_MAP.put(Integer.class, Integer.class);
		PRIMITIVE_MAP.put(long.class, Long.class);
		PRIMITIVE_MAP.put(Long.class, Long.class);
		PRIMITIVE_MAP.put(float.class, Float.class);
		PRIMITIVE_MAP.put(Float.class, Float.class);
		PRIMITIVE_MAP.put(double.class, Double.class);
		PRIMITIVE_MAP.put(Double.class, Double.class);
	}
	
	public static final Map<String,String> ITEMTYPE_MAP = new HashMap<String,String>();
	static {
		ITEMTYPE_MAP.put(MarkType.Area, "Item");
		ITEMTYPE_MAP.put(MarkType.Bar, "Item");
		ITEMTYPE_MAP.put(MarkType.Dot, "DotItem");
		ITEMTYPE_MAP.put(MarkType.Image, "ImageItem");
		ITEMTYPE_MAP.put(MarkType.Label, "LabelItem");
		ITEMTYPE_MAP.put(MarkType.Panel, "LayerItem");
		ITEMTYPE_MAP.put(MarkType.Line, "Item");
		ITEMTYPE_MAP.put(MarkType.Rule, "Item");
		ITEMTYPE_MAP.put(MarkType.Wedge, "WedgeItem");
	}
	
	public static String getMethodString(Class<?> rtype) {
		if (rtype == null) return null;
		String method = METHOD_MAP.get(rtype);
		if (method == null) method = "string";
		return method;
	}
	
	public static String getTypeString(Class<?> rtype) {
		if (rtype == null) return null;
		String type = TYPE_MAP.get(rtype);
		if (type == null) {
			type = rtype.getName();
			type = type.replace("$", ".");
		}
		return type;
	}
	
	public static boolean isPrimitive(Class<?> type) {
		return PRIMITIVE_MAP.containsKey(type);
	}
	
	public static boolean isPrimitiveArray(Class<?> type) {
		return type.isArray() && 
			PRIMITIVE_MAP.containsKey(type.getComponentType());
	}
	
	public static boolean needsImport(Class<?> type) {
		if (type == null) return false;
		return !"java.lang".equals(type.getPackage().getName());
	}
	
	public static String getItemType(String markType) {
		return ITEMTYPE_MAP.get(markType);
	}

	public static Class<?> getBoxedType(Class<?> rtype) {
		Class<?> c = PRIMITIVE_MAP.get(rtype);
		return c==null ? rtype : c;
	}
	
}
