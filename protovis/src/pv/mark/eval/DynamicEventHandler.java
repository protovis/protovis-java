package pv.mark.eval;

import pv.mark.MarkEvent;
import pv.scene.Item;

public class DynamicEventHandler implements EventHandler {

	private String _code;
	private EventHandler _handler;
	
	public String code() { return _code; }
	
	public void handler(EventHandler handler) { _handler = handler; }
	
	private DynamicEventHandler(String code) {
		_code = code;
	}
	
	@Override
	public void handle(MarkEvent event, Item item) {
		if (_handler != null) {
			_handler.handle(event, item);
		} else {
			System.err.println("ERROR: Uncompiled handler.");
		}
	}
	
	private static final String PREFIX = "{{";
	private static final String SUFFIX = "}}";
	
	public static DynamicEventHandler create(String code) {
		if (!isDynamicProperty(code)) {
			// TODO error message
			throw new IllegalArgumentException("NEED ERR MSG: "+code);
		} else {
			code = code.substring(PREFIX.length(), code.length()-SUFFIX.length()).trim();
			if (code.charAt(code.length()-1) != ';') {
				code = code + ";";
			}
			return new DynamicEventHandler(code.intern());
		}
	}
	
	private static boolean isDynamicProperty(String code) {
		return code.startsWith(PREFIX) && code.endsWith(SUFFIX);
	}

}
