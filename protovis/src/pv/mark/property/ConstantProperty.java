package pv.mark.property;

import java.lang.reflect.Modifier;

import pv.scene.Item;
import pv.style.Fill;
import pv.style.Font;
import pv.style.Stroke;

public class ConstantProperty implements Property {

	private Object _value;
	private Class<?> _rtype;
	
	private ConstantProperty(Object value, Class<?> rtype) {
		_value = value;
		_rtype = rtype;
	}
	
	public Object value() {
		return _value;
	}
	
	public boolean dirty() {
		return false;
	}
	
	public void dirty(boolean d) {
		// ignore
	}
	
	public Type type() {
		return Type.CONSTANT;
	}
	
	public Class<?> returnType() {
		return _rtype;
	}
	
	public String toString() {
		return _value.toString();
	}
	
	@Override
	public int hashCode() {
		return _value.hashCode() ^ _rtype.hashCode();
	}
	
	@Override
	public boolean equals(Object a) {
		if (a instanceof DynamicProperty) {
			ConstantProperty c = (ConstantProperty) a;
			return c._value == _value && c._rtype == _rtype;
		} else {
			return false;
		}
	}
	
	public static ConstantProperty create(Object value, Class<?> rtype) {
		return new ConstantProperty(value, rtype);
	}
	
	public static ConstantProperty create(Object value) {
		if (value == null) throw new RuntimeException(
			"Value can not be null without type declaration.");
		Class<?> rtype = value.getClass();
		// ensure type is visible
		while (!Modifier.isPublic(rtype.getModifiers())) {
			rtype = rtype.getSuperclass();
		}
		return new ConstantProperty(value, rtype);
	}

	// -- default return values -----------------------------------------------
	
	public boolean bool(Item item) {
		return (Boolean) _value;
	}

	public Fill fill(Item item) {
		return (Fill) _value;
	}

	public Font font(Item item) {
		return (Font) _value;
	}

	public double number(Item item) {
		return ((Number) _value).doubleValue();
	}

	public Object object(Item item) {
		return _value;
	}

	public String string(Item item) {
		return String.valueOf(_value);
	}

	public Stroke stroke(Item item) {
		return (Stroke) _value;
	}
	
}
