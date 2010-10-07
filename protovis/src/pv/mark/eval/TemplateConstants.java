package pv.mark.eval;

import pv.util.IOLib;

public interface TemplateConstants {

	public static final String CLASSNAME = "{{CLASSNAME}}";
	public static final String ITEMTYPE = "{{ITEMTYPE}}";
	public static final String DATATYPE = "{{DATATYPE}}";
	public static final String PANEL_DATATYPE = "{{PANEL_DATATYPE}}";
	public static final String PROTO_DATATYPE = "{{PROTO_DATATYPE}}";
	public static final String IMPORTS = "{{IMPORTS}}";
	public static final String DECL = "{{DECLARATIONS}}";
	public static final String DATA = "{{DATA}}";
	public static final String VARS = "{{VARS}}";
	public static final String INIT = "{{INIT}}";
	public static final String INSTANCE = "{{INSTANCE}}";
	public static final String ENTER = "{{ENTER}}";
	public static final String EXIT = "{{EXIT}}";
	public static final String GROUP = "{{GROUP}}";
	public static final String KEY = "{{KEY}}";
	public static final String HAS_KEY = "{{HAS_KEY}}";
	public static final String HANDLERS = "{{HANDLERS}}";
	
	public static final String SOURCE_NODES = "{{SOURCE_NODES}}";
	public static final String TARGET_NODES = "{{TARGET_NODES}}";
	public static final String LINK_SOURCE_KEY = "{{LINK_SOURCE_KEY}}";
	public static final String LINK_TARGET_KEY = "{{LINK_TARGET_KEY}}";
	public static final String NODE_SOURCE_KEY = "{{NODE_SOURCE_KEY}}";
	public static final String NODE_TARGET_KEY = "{{NODE_TARGET_KEY}}";
	public static final String NODE_SOURCE_DATATYPE = "{{NODE_SOURCE_DATATYPE}}";
	public static final String NODE_TARGET_DATATYPE = "{{NODE_TARGET_DATATYPE}}";
	
	public static final String BODY = "{{BODY}}";
	public static final String ID = "{{ID}}";
	public static final String VARNAME = "{{VAR_NAME}}";
	public static final String VARTYPE = "{{VAR_TYPE}}";
	
	public static final String TEMPLATE =
		IOLib.safeReadAsString("item_template.txt", TemplateConstants.class);
	public static final String LINK_TEMPLATE =
		IOLib.safeReadAsString("link_template.txt", TemplateConstants.class);
	
	public static final String HANDLER_TEMPLATE =
		"    public EventHandler handler_{{ID}} = new EventHandler() {\n" +
		"	    public void handle(MarkEvent event, Item item) {\n" +
		"		    int index = item.index;\n" +
		"		    Item proto = item.proto();\n" +
		"		    Item cousin = item.cousin();\n" +
		"		    Item parent = item.group.group;\n" +
		"		    {{BODY}}" +
		"	    }\n" +
		"    };\n";
	
	public static final String VAR_TEMPLATE =
		"    private {{CLASSNAME}} {{VAR_NAME}}({{VAR_TYPE}} a) {\n" +
		"        var_{{VAR_NAME}}.value(a);\n" +
		"        return this;\n" +
		"    }\n";
}
