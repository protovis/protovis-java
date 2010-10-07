package pv.render.awt.java2D;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.imageio.ImageIO;

import pv.util.IOLib;

public class Images {

	private int _maxThreads = 8;
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
	
	public ImageEntry get(String location) {
		ImageEntry e = _map.get(location);
		if (e == null) {
			_map.put(location, e = new ImageEntry(location));
			load(e);
		}
		return e;
	}
	
	public boolean remove(String location) {
		return (_map.remove(location) != null);
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
		
		public ImageEntry(String location) {
			this.location = location;
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
					e.loaded = true;
				} catch (Exception ex) {
					e.image = null;
					e.loaded = false;
				}
			}
			
		}
	}
	
}
