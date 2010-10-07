package pv.mark.property;

import pv.scene.Item;
import pv.style.Fill;
import pv.style.Font;
import pv.style.Stroke;

public interface Property {

	public static final Property None = null;
	
	enum Type { CONSTANT, COMPILED, DYNAMIC, VARIABLE };
	
	Type type();
	Class<?> returnType();
	
	boolean dirty();
	void dirty(boolean d);
	
	boolean bool(Item item);
	double number(Item item);
	String string(Item item);
	Font font(Item item);
	Fill fill(Item item);
	Stroke stroke(Item item);
	Object object(Item item);
}
