package pv.animate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pv.mark.constants.PropertyCodes;
import pv.scene.DotItem;
import pv.scene.GroupItem;
import pv.scene.ImageItem;
import pv.scene.Item;
import pv.scene.LabelItem;
import pv.scene.WedgeItem;

public interface Animator {

	void step(float f, Item x, Item a, Item b);

	public static class Init implements PropertyCodes
	{
		private static final Init instance = new Init();
		private static final Map<String,Init> map = new HashMap<String,Init>();
		
		public static final Init instance(String marktype) {
			Init init = map.get(marktype);
			if (init == null) init = instance;
			return init;
		}
		
		public void assign(GroupItem group, long props, List<Animator> list) {
			if ((props & ALPHA) > 0) list.add(Animator.Alpha.instance);
			if ((props & WIDTH) > 0) list.add(Animator.Width.instance);
			if ((props & HEIGHT) > 0) list.add(Animator.Height.instance);
			if ((props & XY) > 0) list.add(Animator.Linear.instance);
			if ((props & FILL) > 0) list.add(Animator.Fill.instance);
			if ((props & STROKE) > 0) list.add(Animator.Stroke.instance);
		}
		
		protected class Dot extends Init
		{
			public void assign(GroupItem group, long props, List<Animator> list) {
				if ((props & ALPHA) > 0) list.add(Animator.Alpha.instance);
				if ((props & WIDTH) > 0) list.add(Animator.Width.instance);
				if ((props & HEIGHT) > 0) list.add(Animator.Height.instance);
				if ((props & XY) > 0) list.add(Animator.Linear.instance);
				if ((props & FILL) > 0) list.add(Animator.Fill.instance);
				if ((props & STROKE) > 0) list.add(Animator.Stroke.instance);
				if ((props & SHAPE) > 0) list.add(Animator.Shape.instance);
				if ((props & SIZE) > 0) list.add(Animator.Size.instance);
			}
		}
		
		protected class Image extends Init
		{
			public void assign(GroupItem group, long props, List<Animator> list) {
				if ((props & ALPHA) > 0) list.add(Animator.Alpha.instance);
				if ((props & WIDTH) > 0) list.add(Animator.Width.instance);
				if ((props & HEIGHT) > 0) list.add(Animator.Height.instance);
				if ((props & XY) > 0) list.add(Animator.Linear.instance);
				if ((props & FILL) > 0) list.add(Animator.Fill.instance);
				if ((props & STROKE) > 0) list.add(Animator.Stroke.instance);
				if ((props & URL) > 0) list.add(Animator.URL.instance);
			}
		}
		
		protected class Label extends Init
		{
			public void assign(GroupItem group, long props, List<Animator> list) {
				if ((props & ALPHA) > 0) list.add(Animator.Alpha.instance);
				if ((props & WIDTH) > 0) list.add(Animator.Width.instance);
				if ((props & HEIGHT) > 0) list.add(Animator.Height.instance);
				if ((props & XY) > 0) list.add(Animator.Linear.instance);
				if ((props & FILL) > 0) list.add(Animator.Fill.instance);
				if ((props & STROKE) > 0) list.add(Animator.Stroke.instance);
				if ((props & FONT) > 0) list.add(Animator.Font.instance);
				if ((props & TEXT) > 0) list.add(Animator.Text.instance);
				if ((props & BASELINE) > 0) list.add(Animator.TextBaseline.instance);
				if ((props & ANGLE) > 0) list.add(Animator.TextAngle.instance);
				if ((props & ALIGN) > 0) list.add(Animator.TextAlign.instance);
				if ((props & MARGIN) > 0) list.add(Animator.TextMargin.instance);
			}
		}
		
		protected class Wedge extends Init
		{
			public void assign(GroupItem group, long props, List<Animator> list) {
				if ((props & ALPHA) > 0) list.add(Animator.Alpha.instance);
				if ((props & WIDTH) > 0) list.add(Animator.Width.instance);
				if ((props & HEIGHT) > 0) list.add(Animator.Height.instance);
				if ((props & XY) > 0) list.add(Animator.Linear.instance);
				if ((props & FILL) > 0) list.add(Animator.Fill.instance);
				if ((props & STROKE) > 0) list.add(Animator.Stroke.instance);
				if ((props & INNER) > 0) list.add(Animator.InnerRadius.instance);
				if ((props & OUTER) > 0) list.add(Animator.OuterRadius.instance);
				if ((props & ANGLE) > 0) list.add(Animator.Angle.instance);
			}
		}
		
	}
	
	// -- Animator Definitions ------------------------------------------------
	
	public static class Linear implements Animator {
		public static final Animator instance = new Linear();
		public void step(float f, Item x, Item a, Item b) {
			x.left = a.left + f*(b.left-a.left);
			x.top = a.top + f*(b.top-a.top);
			x.right = x.group.group.width - x.left - x.width;
			x.bottom = x.group.group.height - x.top - x.height;
		}
	}
	
	public static class Polar implements Animator {
		public static final Animator instance = new Polar();
		public void step(float f, Item x, Item a, Item b) {
			double cx = x.group.left, cy = x.group.top;
			double ax = a.left-cx, ay = a.top-cy;
			double bx = b.left-cx, by = b.top-cy;
			
			double ar = Math.sqrt(ax*ax + ay*ay);
			double br = Math.sqrt(bx*bx + by*by);
			double at = Math.atan2(ay, ax);
			double bt = Math.atan2(by, bx);
			double xr = ar + f*(br-ar);
			double xt = at + f*(bt-at);
			
			x.left = xr * Math.cos(xt);
			x.top  = xr * -Math.sin(xt);
			x.right = x.group.group.width - x.left - x.width;
			x.bottom = x.group.group.height - x.top - x.height;
		}
	}
	
	public static class Alpha implements Animator {
		public static final Animator instance = new Alpha();
		public void step(float f, Item x, Item a, Item b) {
			x.alpha = a.alpha + f*(b.alpha-a.alpha);
		}
	}
	
	public static class Width implements Animator {
		public static final Animator instance = new Width();
		public void step(float f, Item x, Item a, Item b) {
			x.width = a.width + f*(b.width-a.width);
		}
	}
	
	public static class Height implements Animator {
		public static final Animator instance = new Height();
		public void step(float f, Item x, Item a, Item b) {
			x.height = a.height + f*(b.height-a.height);
		}
	}
	
	public static class Fill implements Animator {
		public static final Animator instance = new Fill();
		public void step(float f, Item x, Item a, Item b) {
			x.fill = pv.style.Fill.interpolate(f, a.fill, b.fill);
		}
	}
	
	public static class Stroke implements Animator {
		public static final Animator instance = new Stroke();
		public void step(float f, Item x, Item a, Item b) {
			x.stroke = pv.style.Stroke.interpolate(f, a.stroke, b.stroke);
		}
	}
	
	public static class Shape implements Animator {
		public static final Animator instance = new Shape();
		public void step(float f, Item x, Item a, Item b) {
			((DotItem)x).shape = (f < 0.5f ? ((DotItem)a).shape : ((DotItem)b).shape);
		}
	}
	
	public static class Size implements Animator {
		public static final Animator instance = new Size();
		public void step(float f, Item xx, Item aa, Item bb) {
			DotItem x = (DotItem)xx, a = (DotItem)aa, b = (DotItem)bb;
			x.size = a.size + f*(b.size-a.size);
			x.radius = a.radius + f*(b.radius-a.radius);
		}
	}
	
	public static class URL implements Animator {
		public static final Animator instance = new URL();
		public void step(float f, Item x, Item a, Item b) {
			((ImageItem)x).url = (f < 0.5f ? ((ImageItem)a).url : ((ImageItem)b).url);
		}
	}
	
	public static class InnerRadius implements Animator {
		public static final Animator instance = new InnerRadius();
		public void step(float f, Item xx, Item aa, Item bb) {
			WedgeItem x = (WedgeItem)xx, a = (WedgeItem)aa, b = (WedgeItem)bb;
			x.innerRadius = a.innerRadius + f*(b.innerRadius-a.innerRadius);
		}
	}
	
	public static class OuterRadius implements Animator {
		public static final Animator instance = new OuterRadius();
		public void step(float f, Item xx, Item aa, Item bb) {
			WedgeItem x = (WedgeItem)xx, a = (WedgeItem)aa, b = (WedgeItem)bb;
			x.outerRadius = a.outerRadius + f*(b.outerRadius-a.outerRadius);
		}
	}
	
	public static class Angle implements Animator {
		public static final Animator instance = new Angle();
		public void step(float f, Item xx, Item aa, Item bb) {
			WedgeItem x = (WedgeItem)xx, a = (WedgeItem)aa, b = (WedgeItem)bb;
			x.startAngle = a.startAngle + f*(b.startAngle-a.startAngle);
			x.endAngle = a.endAngle + f*(b.endAngle-a.endAngle);
			x.angle = x.endAngle - x.startAngle;
		}
	}
	
	public static class Font implements Animator {
		public static final Animator instance = new Font();
		public void step(float f, Item xx, Item aa, Item bb) {
			LabelItem x = (LabelItem)xx, a = (LabelItem)aa, b = (LabelItem)bb;
			x.font = pv.style.Font.interpolate(f, a.font, b.font);
		}
	}
	
	public static class Text implements Animator {
		public static final Animator instance = new Text();
		public void step(float f, Item xx, Item aa, Item bb) {
			LabelItem x = (LabelItem)xx, a = (LabelItem)aa, b = (LabelItem)bb;
			x.text = f < 0.5f ? a.text : b.text;
		}
	}
	
	public static class TextBaseline implements Animator {
		public static final Animator instance = new TextBaseline();
		public void step(float f, Item xx, Item aa, Item bb) {
			LabelItem x = (LabelItem)xx, a = (LabelItem)aa, b = (LabelItem)bb;
			x.textBaseline = f < 0.5f ? a.textBaseline : b.textBaseline;
		}
	}
	
	public static class TextAlign implements Animator {
		public static final Animator instance = new TextAlign();
		public void step(float f, Item xx, Item aa, Item bb) {
			LabelItem x = (LabelItem)xx, a = (LabelItem)aa, b = (LabelItem)bb;
			x.textAlign = f < 0.5f ? a.textAlign : b.textAlign;
		}
	}
	
	public static class TextAngle implements Animator {
		public static final Animator instance = new TextAngle();
		public void step(float f, Item xx, Item aa, Item bb) {
			LabelItem x = (LabelItem)xx, a = (LabelItem)aa, b = (LabelItem)bb;
			x.textAngle = a.textAngle + f*(b.textAngle-a.textAngle);
		}
	}
	
	public static class TextMargin implements Animator {
		public static final Animator instance = new TextAngle();
		public void step(float f, Item xx, Item aa, Item bb) {
			LabelItem x = (LabelItem)xx, a = (LabelItem)aa, b = (LabelItem)bb;
			x.textMargin = a.textMargin + f*(b.textMargin-a.textMargin);
		}
	}
	
}
