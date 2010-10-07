package pv.animate;

import java.util.List;
import java.util.concurrent.Callable;

import pv.scene.GroupItem;
import pv.scene.Item;
import pv.style.Easing;
import pv.util.ObjectPool;
import pv.util.Objects;
import pv.util.ThreadPool;

public class GroupTransition extends ItemTransition {

	protected static final int PARALLEL_THRESHOLD = 4000;
	protected GroupItem group;
	protected double dt, dd;
	protected Easing ease;
	
	protected static ObjectPool<Task> _pool = new ObjectPool<Task>(100) {
		public Task create() { return new Task(); }
	};
		
	public GroupTransition(GroupItem group, List<Animator> animators)
	{
		this.group = group;
		this.interp = animators;
	}
	
	@SuppressWarnings("unchecked")
	public long step(double dt, double dd, Easing e) {
		this.dt = dt;
		this.dd = dd;
		this.ease = e;

		long next = -1;
		int len = group.size();
		boolean fork = len > PARALLEL_THRESHOLD && ThreadPool.getThreadCount() > 1;
		
		//long t0 = System.currentTimeMillis();
		if (fork) {
			// partition animation into tasks
			int blockSize = Math.max(1, 1 + len / ThreadPool.getThreadCount()); 
			List<Task> list = (List<Task>) Objects.List.get();
			for (int i=0; i<len; i+=blockSize) {
				int end = i + blockSize; if (end > len) end = len;
				list.add(task(i, end));
			}
			// invoke tasks on thread pool
			try {
				ThreadPool.getThreadPool().invokeAll(list);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
			// gather results and clean up
			for (Task t : list) {
				if (t.next > 0)
					next = (next > 0 ? Math.min(next, t.next) : t.next);
				_pool.reclaim(t);
			}
			Objects.List.reclaim(list);
		} else {
			for (int i=0; i<group.size(); ++i) {
				x = group.item(i);
				a = x.next;
				b = a.next;
				long time = super.step(dt, dd, e); 
				if (time > 0)
					next = (next > 0 ? Math.min(time, next) : time);
			}
			x = a = b = null;
		}
//		long t1 = System.currentTimeMillis();
//		if (len > PARALLEL_THRESHOLD)
//			System.out.println(fork + "\t" + (t1-t0)/1000f+"s");
		
		return next;
	}
	
	protected Task task(int start, int end) {
		Task t = _pool.get();
		t.start = start;
		t.end = end;
		t.gt = this;
		return t;
	}
	
	public static class Task implements Callable<Task> {
		GroupTransition gt;
		int start, end;
		long next;
		
		public Task call() throws Exception {
			next = -1;
			for (int i=start; i<end; ++i) {
				Item x = gt.group.item(i), a = x.next, b = a.next;
				long time = gt.interpolate(x, a, b, gt.dt, gt.dd, gt.ease);
				if (time > 0)
					next = (next > 0 ? Math.min(time, next) : time);
			}
			return this;
		}
	}
	
}
