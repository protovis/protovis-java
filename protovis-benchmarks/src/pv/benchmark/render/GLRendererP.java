package pv.benchmark.render;


import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.media.opengl.GL;

import pv.mark.constants.MarkType;
import pv.render.awt.gl.GLRenderer;
import pv.scene.GroupItem;
import pv.scene.PanelItem;
import pv.util.ObjectPool;
import pv.util.Objects;
import pv.util.ThreadPool;

public class GLRendererP extends GLRenderer {
	
	private static GLRendererP s_instance = null;
	public static GLRendererP instance() {
		if (s_instance == null) s_instance = new GLRendererP();
		return s_instance;
	}
	
	private static ObjectPool<RenderTask> s_begin =
		new ObjectPool<RenderTask>(200) {
			public RenderTask create() { return new RenderTask.LayerBegin(); }
			public void clear(RenderTask task) { task.clear(); }
		};
	private static ObjectPool<RenderTask> s_finish =
		new ObjectPool<RenderTask>(200) {
			public RenderTask create() { return new RenderTask.LayerFinish(); }
			public void clear(RenderTask task) { task.clear(); }
		};
	private static Map<String,ObjectPool<RenderTask>> s_tasks =
		new HashMap<String,ObjectPool<RenderTask>>();
	
	static {
		s_tasks.put(MarkType.Bar, new ObjectPool<RenderTask>(200) {
			public RenderTask create() { return new RenderTask.Bar(); }
			public void clear(RenderTask task) { task.clear(); }
		});
		s_tasks.put(MarkType.Link, new ObjectPool<RenderTask>(200) {
			public RenderTask create() { return new RenderTask.Link(); }
			public void clear(RenderTask task) { task.clear(); }
		});
		s_tasks.put(MarkType.Line, new ObjectPool<RenderTask>(200) {
			public RenderTask create() { return new RenderTask.Path(); }
			public void clear(RenderTask task) { task.clear(); }
		});
		s_tasks.put(MarkType.Dot, new ObjectPool<RenderTask>(200) {
			public RenderTask create() { return new RenderTask.Circle(); }
			public void clear(RenderTask task) { task.clear(); }
		});
	}
	
	public GLRendererP() {
	}
	
	public void init(GL gl, float[] trans)
	{
		this._zoom = (float) Math.sqrt(trans[0]*trans[0] + trans[4]*trans[4]);
		if (_zoom < 0.1f) _zoom = 0.1f;
	}
		
	public void render(GL gl, PanelItem layer) {
		draw(gl, layer);
	}
	
	private Queue<Future<RenderTask>> _tasks = 
		new ArrayDeque<Future<RenderTask>>();
	
	public void draw(GL gl, PanelItem layer) {
		// populate task queue
		_tasks.clear();
		populate(layer, _tasks);
		
		// while task queue is not empty
		while (!_tasks.isEmpty()) {
			Future<RenderTask> f = _tasks.poll();
			RenderTask t;
			try {
				t = f.get();
				t.render(gl);
				
				if (t instanceof RenderTask.LayerBegin) {
					s_begin.reclaim((RenderTask.LayerBegin) t);
				} else if (t instanceof RenderTask.LayerFinish) {
					s_finish.reclaim((RenderTask.LayerFinish) t);
				} else {
					s_tasks.get(t.group.type).reclaim(t);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static void populate(PanelItem layer, Queue<Future<RenderTask>> queue) {
		ExecutorService exec = ThreadPool.getThreadPool();
		
//		RenderTask.LayerBegin lb = new RenderTask.LayerBegin();
//		lb.init(layer, -1, -1);
//		queue.add(exec.submit(lb));
		queue.add(exec.submit(s_begin.get().init(layer, -1, -1)));
		
		List<GroupItem> items = preprocess(layer.items);
		for (int i=0; i<items.size(); ++i)
		{
			GroupItem group = items.get(i);
			if (group == null) continue; // XXX revisit to prevent
				
			if (group instanceof PanelItem)
			{
				populate((PanelItem)group, queue);
			}
			else if (group.type == MarkType.Panel)
			{
				List<GroupItem> layers = preprocess(group.items);
				for (int j=0; j < layers.size(); ++j) {
					populate((PanelItem)layers.get(j), queue);
				}
				Objects.List.reclaim(layers);
				// TODO handle bounds for group of layers
			}
			else if (group.visible)
			{
				//boolean isBar = group.type == MarkType.Bar;
				ObjectPool<? extends RenderTask> pool = s_tasks.get(group.type);
				int end = group.size(), step = end;
				for (int a=0, b=step; a<end; a=b, b=b+step) {
					//RenderTask t = isBar ? new RenderTask.Bar() : new RenderTask.Path();
					//t.init(group, a, b);
					//queue.add(exec.submit(t));
					queue.add(exec.submit(
						pool.get().init(group, a, (b > end ? end : b))));
				}
			}
		}
		Objects.List.reclaim(items);
		
//		RenderTask.LayerFinish lf = new RenderTask.LayerFinish();
//		lf.init(layer, -1, -1);
//		queue.add(exec.submit(lf));
		queue.add(exec.submit(s_finish.get().init(layer, -1, -1)));
	}
	
}