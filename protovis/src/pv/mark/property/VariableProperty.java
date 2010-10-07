package pv.mark.property;

import pv.mark.Variable;
import pv.scene.Item;
import pv.style.Fill;
import pv.style.Font;
import pv.style.Stroke;

public class VariableProperty implements Property {

	private Variable _var;
	private int _version;
	
	public VariableProperty(Variable v) {
		_var = v;
		_version = _var.version();
	}
	
	public VariableProperty(Object o) {
		this((Variable)o);
	}
	
	public Variable value() {
		return _var;
	}
	
	public boolean dirty() {
		return _version != _var.version();
	}
	
	public void dirty(boolean d) {
		if (d == false) {
			_version = _var.version();
		}
	}
	
	public Class<?> returnType() {
		return _var.type();
	}

	public Type type() {
		return Type.VARIABLE;
	}
	
	// -- default return values -----------------------------------------------
	
	public Fill fill(Item item) {
		return (Fill) _var.value();
	}

	public Font font(Item item) {
		return (Font) _var.value();
	}
	
	public boolean bool(Item item) {
		return (Boolean) _var.value();
	}
	
	public double number(Item item) {
		return (Double) _var.value();
	}

	public String string(Item item) {
		return String.valueOf(_var.value());
	}
	
	public Object object(Item item) {
		return _var.value();
	}

	public Stroke stroke(Item item) {
		return (Stroke) _var.value();
	}
	
}
