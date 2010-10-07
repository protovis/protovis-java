package pv.mark.property;


public class DynamicProperty extends AbstractProperty {

	public static final String PREFIX = "{{";
	public static final String SUFFIX = "}}";
	
	private String _code;
	private String[] _lines;

	private DynamicProperty(String code) {
		this(code, null);
	}
	
	private DynamicProperty(String code, Class<?> rtype) {
		super(rtype);
		_code = code;
		_lines = code.split(";");
		for (int i=0; i<_lines.length; ++i) {
			_lines[i] = _lines[i].trim().replace('\'', '"');
		}
	}
	
	public String[] lines() {
		return _lines;
	}
	
	@Override
	public Type type() {
		return Type.DYNAMIC;
	}
	
	@Override
	public String toString() {
		return _code;
	}
	
	@Override
	public int hashCode() {
		return _code.hashCode() ^ _rtype.hashCode();
	}
	
	@Override
	public boolean equals(Object a) {
		if (a instanceof DynamicProperty) {
			DynamicProperty d = (DynamicProperty) a;
			return d._code == _code && d._rtype == _rtype;
		} else {
			return false;
		}
	}
	
	public static DynamicProperty create(String code, Class<?> rtype) {
		if (!isDynamicProperty(code)) {
			// TODO error message
			throw new IllegalArgumentException("NEED ERR MSG: "+code);
		} else {
			code = code.substring(PREFIX.length(), code.length()-SUFFIX.length());
			return new DynamicProperty(code.intern(), rtype);
		}
	}
	
	public static boolean isDynamicProperty(String code) {
		return code.startsWith(PREFIX) && code.endsWith(SUFFIX);
	}

}
