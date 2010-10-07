package pv.render.awt.gl;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;

import pv.util.IOLib;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureData;
import com.sun.opengl.util.texture.TextureIO;

public class Images {

	private static Images s_instance = new Images();
	public static Images instance() { return s_instance; }
	
	private int _maxThreads = 8; // TODO move to properties
	private int _threadCount = 0;
	private Map<String,ImageEntry> _map = new HashMap<String,ImageEntry>();
	private LinkedList<ImageEntry> _queue = new LinkedList<ImageEntry>();
	
	public Images() {
		
	}
	
	public Images(int maxThreads) {
		_maxThreads = maxThreads;
	}
	
	// -----
	
	public BufferedImage getImage(String location) {
		ImageEntry e = get(location);
		return (e==null || !e.loaded) ? null : e.image;
	}
	
	public TextureData getTextureData(String location) {
		ImageEntry e = get(location);
		return (e==null || !e.loaded) ? null : e.tdata;
	}
	
	public Texture getTexture(String location, GL gl) {
		ImageEntry e = get(location);
		return (e==null || !e.loaded) ? null : e.texture(gl);
	}
	
	public ImageEntry get(String location) {
		ImageEntry e = _map.get(location);
		if (e == null) {
			_map.put(location, e = new ImageEntry(location));
			load(e);
		}
		return e;
	}
	
	public boolean remove(String location) {
		ImageEntry e = _map.remove(location);
		if (e != null) {
			e.dispose();
			return true;
		} else {
			return false;
		}
	}
	
	private void load(ImageEntry e) {
		boolean newThread = false;
		synchronized (_queue) {
			_queue.add(e);
			newThread = (_threadCount < _maxThreads);
		}
		if (newThread) new Loader().start();
	}
	
	public static class ImageEntry {
		boolean loaded = false;
		String location = null;
		
		BufferedImage image = null;
		TextureData tdata = null;
		Texture texture = null;
		
		public ImageEntry(String location) {
			this.location = location;
		}
		
		public Texture texture(GL gl) {
			if (texture == null) {
				texture = TextureIO.newTexture(tdata);
				texture.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
				texture.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
			}
			return texture;
		}
		
		public void dispose() {
			if (texture != null) texture.dispose();
		}
	}
	
	public class Loader extends Thread {
		public void run() {
			while (true) {
				ImageEntry e = null;
				synchronized (_queue) {
					if (_queue.isEmpty()) {
						_threadCount--;
						return;
					}
					e = _queue.removeFirst();
				}
				try {
					URL url = IOLib.urlFromString(e.location);
					e.image = ImageIO.read(url);
					e.tdata = TextureIO.newTextureData(e.image, true);
					e.loaded = true;
				} catch (Exception ex) {
					e.image = null;
					e.tdata = null;
					e.loaded = false;
				}
			}
			
		}
	}
	
}
