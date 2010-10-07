package pv.mark.property;

import pv.scene.Item;
import pv.style.Fill;
import pv.style.Font;
import pv.style.Stroke;

public abstract class AbstractProperty implements Property {

	protected boolean _dirty = true;
	protected Class<?> _rtype = null;
	
	protected AbstractProperty() {}
	protected AbstractProperty(Class<?> rtype) { _rtype = rtype; }
	
	public boolean dirty() {
		return _dirty;
	}
	
	public void dirty(boolean d) {
		_dirty = d;
	}
	
	public Class<?> returnType() {
		return _rtype;
	}
		
	public Type type() {
		return Type.COMPILED;
	}
		
	// -- default return values -----------------------------------------------
	
	public Fill fill(Item item) {
		throw new UnsupportedOperationException();
	}

	public Font font(Item item) {
		throw new UnsupportedOperationException();
	}
	
	public boolean bool(Item item) {
		throw new UnsupportedOperationException();
	}
	
	public double number(Item item) {
		throw new UnsupportedOperationException();
	}

	public String string(Item item) {
		throw new UnsupportedOperationException();
	}
	
	public Object object(Item item) {
		throw new UnsupportedOperationException();
	}

	public Stroke stroke(Item item) {
		throw new UnsupportedOperationException();
	}

}
