package pv.mark;

import java.util.HashSet;
import java.util.Set;

import pv.mark.constants.Interpolate;
import pv.mark.constants.Shape;
import pv.mark.constants.TextAlign;
import pv.mark.constants.TextBaseline;
import pv.scene.DotItem;
import pv.scene.ImageItem;
import pv.scene.Item;
import pv.scene.LabelItem;
import pv.scene.PanelItem;
import pv.scene.LinkItem;
import pv.scene.RuleItem;
import pv.scene.WedgeItem;
import pv.style.Fill;
import pv.style.Font;
import pv.style.Stroke;

public interface Definition {

	Mark defaults();
	Class<?> itemType();
	boolean isSupported(String propertyName);
	
	public static class AbstractDefinition implements Definition
	{
		protected Mark _defaults = Mark.create()
			.datatype(Object.class)
			.data("{{data}}")
			.key("{{index}}")
			.visible(true)
			.alpha(1);
		
		protected Set<String> _legalProperties = new HashSet<String>();
		public AbstractDefinition() {
			_legalProperties.add("data");
			_legalProperties.add("visible");
			_legalProperties.add("alpha");
			_legalProperties.add("left");
			_legalProperties.add("top");
			_legalProperties.add("right");
			_legalProperties.add("bottom");
			_legalProperties.add("width");
			_legalProperties.add("height");
			_legalProperties.add("fill");
			_legalProperties.add("stroke");
		}
		public Mark defaults() { return _defaults; }
		public Class<?> itemType() { return Item.class; }
		public boolean isSupported(String propertyName) {
			return _legalProperties.contains(propertyName);
		}
	}

	public static class Area extends AbstractDefinition {
		public Area() {
			_defaults
				.interpolate(Interpolate.Linear)
				.width(0)
				.height(0)
				.stroke(Stroke.none)
				.fill(Fill.solid(0x0000aa, 1));
		}
	}
	
	public static class Bar extends AbstractDefinition
	{	
		public Bar() {
			_defaults
				.width(4)
				.height(4)
				.stroke(Stroke.none)
				.fill(Fill.solid(0x0000aa, 1));
		}
	}
	
	public static class Dot extends AbstractDefinition
	{
		public Dot() {
			_defaults
				.size(16)
				.shape(Shape.Circle)
				.stroke(Stroke.solid(1))
				.set("radius","{{Math.sqrt(item.size)}}");
			_legalProperties.add("size");
			_legalProperties.add("shape");
			_legalProperties.add("radius");
		}
		public Class<?> itemType() { return DotItem.class; }
	}
	
	public static class Image extends AbstractDefinition
	{
		public Image() {
			_defaults
				.width(-1)
				.height(-1);
			_legalProperties.add("url");
		}
		public Class<?> itemType() { return ImageItem.class; }
	}
	
	public static class Label extends AbstractDefinition
	{
		public Label() {
			_defaults
				.text("{{String.valueOf(data)}}") /*identity*/
				.textAlign(TextAlign.Left)
				.textBaseline(TextBaseline.Top)
				.textAngle(0)
				.textMargin(3)
				.font(Font.font("Helvetica", 12))
				.fill(Fill.solid(0));
			_legalProperties.add("text");
			_legalProperties.add("textAlign");
			_legalProperties.add("textBaseline");
			_legalProperties.add("textAngle");
			_legalProperties.add("textMargin");
			_legalProperties.add("font");
		}
		public Class<?> itemType() { return LabelItem.class; }
	}
	
	public static class Panel extends AbstractDefinition
	{
		public Panel() {
			_defaults
				.left(0)
				.right(0)
				.top(0)
				.bottom(0)
				.width(0)
				.height(0)
				.stroke(Stroke.none)
				.fill(Fill.none);
		}
		public Class<?> itemType() { return PanelItem.class; }
	}
	
	public static class Link extends AbstractDefinition
	{
		public Link() {
			_defaults
				.depth(1)
				.set("sourceX", "{{item.source.left}}", double.class)
				.set("sourceY", "{{item.source.top}}", double.class)
				.set("targetX", "{{item.target.left}}", double.class)
				.set("targetY", "{{item.target.top}}", double.class)
				.visible("{{item.source.visible && item.target.visible}}")
				.nodes("{{item.proto}}")
				.nodeKey("{{data}}")
				.stroke(Stroke.solid(1, 0xcccccc));
			_legalProperties.add("sourceNodes");
			_legalProperties.add("targetNodes");
			_legalProperties.add("nodes");
			_legalProperties.add("sourceNodeKey");
			_legalProperties.add("targetNodeKey");
			_legalProperties.add("nodeKey");
			_legalProperties.add("sourceKey");
			_legalProperties.add("targetKey");
		}
		public Class<?> itemType() { return LinkItem.class; }
	}
	
	public static class Line extends AbstractDefinition
	{
		public Line() {
			_defaults
				.width(0)
				.height(0)
				.stroke(Stroke.solid(1, 0x0000aa))
				.interpolate(Interpolate.Linear);
		}
	}
	
	public static class Rule extends AbstractDefinition
	{
		public Rule() {
			_defaults
				.stroke(Stroke.solid(1, 0));
		}
		public Class<?> itemType() { return RuleItem.class; }
	}
	
	public static class Wedge extends AbstractDefinition
	{
		public Wedge() {
			_defaults
				.startAngle(Double.NaN)
				.endAngle(Double.NaN)
				.angle(Double.NaN)
				.innerRadius(0)
				.outerRadius(1)
				.stroke(Stroke.none)
				.fill(Fill.none);
			_legalProperties.add("startAngle");
			_legalProperties.add("endAngle");
			_legalProperties.add("angle");
			_legalProperties.add("innerRadius");
			_legalProperties.add("outerRadius");
		}
		public Class<?> itemType() { return WedgeItem.class; }
	}
}
