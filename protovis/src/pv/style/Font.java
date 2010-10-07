package pv.style;

import java.util.HashMap;
import java.util.Map;

import pv.util.Objects;

public class Font {

	private static final byte OPT_BOLD       = 0x01;
	private static final byte OPT_ITALIC     = 0x02;
	private static final byte OPT_SMALLCAPS = 0x04;
	
	public static final String BOLD = "bold";
	public static final String ITALIC = "italic";
	public static final String SMALLCAPS = "small-caps";
	
	private static final Map<String,Font> s_cache = new HashMap<String,Font>();
	
	// ------------------------------------------------------------------------
	
	private final String _name;
	private final double _size;
	private final byte _options;
	
	public String name() { return _name; }
	public double size() { return _size; }
	public boolean bold() { return (_options & OPT_BOLD) > 0; }
	public boolean italic() { return (_options & OPT_ITALIC) > 0; }
	public boolean smallcaps() { return (_options & OPT_SMALLCAPS) > 0; }
	
	private Font(String name, double size, byte options) {
		_name = name;
		_size = size;
		_options = options;
	}
	
	private static String key(String name, double size, boolean bold,
		boolean italic, boolean smallcaps)
	{
		StringBuffer sb = Objects.StringBuffer.get();
		if (italic) sb.append(ITALIC).append(" ");
		if (smallcaps) sb.append(SMALLCAPS).append(" ");
		if (bold) sb.append(BOLD).append(" ");
		sb.append(name);
		String key = sb.toString();
		Objects.StringBuffer.reclaim(sb);
		return key;
	}
	
	private static String key(String name, double size, byte opt)
	{
		StringBuffer sb = Objects.StringBuffer.get();
		if ((opt & OPT_ITALIC) > 0) sb.append(ITALIC).append(" ");
		if ((opt & OPT_SMALLCAPS) > 0) sb.append(SMALLCAPS).append(" ");
		if ((opt & OPT_BOLD) > 0) sb.append(BOLD).append(" ");
		sb.append(name);
		String key = sb.toString();
		Objects.StringBuffer.reclaim(sb);
		return key;
	}
	
	// parse
	// ((italic) )?((small-caps) )?((bold) )? (\d+)(\.*) (\.*)
	public static Font font(String spec) {
		if (spec == null || spec.length() == 0)
			return null;
		
		Font f = s_cache.get(spec);
		if (f != null) return f;
		
		String name; double size = 10;
		boolean bold = false, italic = false, smallcaps = false;
		
		// parse font specification
		String[] tok = spec.split(" ");
		int len = tok.length - 1; if (len < 0) return null;
		name = tok[len--];
		if (len >= 0) {
			size = Double.parseDouble(tok[len--]);
		}
		while (len >= 0) {
			String s = tok[len--];
			if (BOLD.equals(s)) {
				bold = true;
			} else if (ITALIC.equals(s)) {
				italic = true;
			} else if (SMALLCAPS.equals(s)) {
				smallcaps = true;
			} else {
				throw new IllegalArgumentException("Unrecognized font format");
			}
		}
		
		// build font instance
		String key = key(name, size, bold, italic, smallcaps);
		byte options = 0;
		if (bold) options |= OPT_BOLD;
		if (italic) options |= OPT_ITALIC;
		if (smallcaps) options |= OPT_SMALLCAPS;
		f = new Font(name, size, options);
		s_cache.put(key, f);
		return f;
	}
	
	public static Font font(String name, double size) {
		return font(name, size, false, false);
	}
	
	public static Font font(String name, double size, boolean bold) {
		return font(name, size, bold, false);
	}
	
	public static Font font(String name, double size, boolean bold, boolean italic) {
		return font(name, size, bold, italic, false);
	}
	
	public static Font font(String name, double size, boolean bold, boolean italic, boolean smallcaps) {
		String key = key(name, size, bold, italic, smallcaps);
		Font f = s_cache.get(key);
		if (f != null) return f;
		
		byte options = 0;
		if (bold) options |= OPT_BOLD;
		if (italic) options |= OPT_ITALIC;
		if (smallcaps) options |= OPT_SMALLCAPS;
		f = new Font(name, size, options);
		
		s_cache.put(key, f);
		return f;
	}
	
	private static Font font(String name, double size, byte opt) {
		String key = key(name, size, opt);
		Font f = s_cache.get(key);
		if (f != null) return f;
		
		f = new Font(name, size, opt);
		s_cache.put(key, f);
		return f;
	}
	
	public static Font interpolate(float f, Font a, Font b) {
		String name; byte opt = 0;
		if (f < 0.5f) {
			name = a._name;
			opt = a._options;
		} else {
			name = b._name;
			opt = b._options;
		}
		double size = a._size + f * (b._size - a._size);
		return font(name, size, opt);
	}
	
}
