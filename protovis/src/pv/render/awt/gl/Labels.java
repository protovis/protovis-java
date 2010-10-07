package pv.render.awt.gl;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.media.opengl.GL;

import pv.render.awt.Fonts;
import pv.scene.LabelItem;
import pv.style.Font;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureData;
import com.sun.opengl.util.texture.TextureIO;

public class Labels {

	private Map<Long,LabelEntry> _map = new LinkedHashMap<Long,LabelEntry>() {
		private static final long serialVersionUID = -3097395989789016399L;
		private int _cacheSize = 10000; // TODO move to properties
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long,LabelEntry> eldest) {
	         return size() > _cacheSize;
	    }
	};
	private BufferedImage _img = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
	private Graphics2D _g = (Graphics2D)_img.getGraphics();
	
	// -----
	
	public Texture getTexture(LabelItem item, GL gl) {
		LabelEntry e = get(item);
		return (e==null || !e.loaded) ? null : e.texture(gl);
	}
	
	private static long key(String text, Font font) {
		return (((long)font.hashCode()) << 32) | text.hashCode();
	}
	
	public LabelEntry get(LabelItem item) {
		long key = key(item.text, item.font);
		LabelEntry e = _map.get(key);
		if (e == null) {
			_map.put(key, e = new LabelEntry(item.text, item.font));
			e.create(_g);
			e.loaded = true;
		}
		return e;
	}
	
	public boolean remove(LabelItem item) {
		LabelEntry e = _map.remove(item);
		if (e != null) {
			e.dispose();
			return true;
		} else {
			return false;
		}
	}
	
	public static final java.awt.Font font(Font f) {
		int style = java.awt.Font.PLAIN;
		if (f.bold()) style |= java.awt.Font.BOLD;
		if (f.italic()) style |= java.awt.Font.ITALIC;
		return Fonts.getFont(f.name(), style, f.size());
	}
	
	public static class LabelEntry {
		boolean loaded;
		String text;
		Font font;
		TextureData tdata = null;
		Texture texture = null;
		
		public LabelEntry(String text, Font font) {
			this.text = text;
			this.font = font;
		}
		
		public void create(Graphics2D gg) {
			java.awt.Font jfont = font(font);
			FontMetrics fm = gg.getFontMetrics(jfont);
			int w = fm.stringWidth(text);
			int h = fm.getHeight();
			BufferedImage img = new BufferedImage(w, h,
				BufferedImage.TYPE_4BYTE_ABGR_PRE);
			Graphics2D g = (Graphics2D) img.getGraphics();
			g.setFont(jfont);
			g.drawString(text, 0, fm.getAscent());
			tdata = TextureIO.newTextureData(img, false);
		}
		
		public Texture texture(GL gl) {
			if (texture == null) {
				texture = TextureIO.newTexture(tdata);
				texture.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
				texture.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
			}
			return texture;
		}
		
		public void dispose() {
			if (texture != null) {
				texture.dispose();
				texture = null;
			}
			loaded = false;
		}
	}
	
}
