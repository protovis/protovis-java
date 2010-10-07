package pv.mark;

public class Variable {

	private Object _value;
	private Class<?> _type;
	private int _version = 0;
	
	public Object value() { return _value; }
	
	public Class<?> type() { return _type; }
	public int version() { return _version; }
	
	public void value(Object o) {
		if (o == _value) return;
		if (o != null && o.equals(_value)) return;
		// TODO type check?
		_value = o;
		_version++;
	}
	
	public Variable(Object value) {
		this(value, value==null ? Object.class : value.getClass());
	}
	
	public Variable(Object value, Class<?> type) {
		_value = value;
		_type = type;
	}
	
}
