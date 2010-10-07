package pv.animate;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Scheduler extends Thread {

	private static Scheduler _instance = null;
	public static Scheduler instance() {
		if (_instance == null) {
			synchronized (Scheduler.class) {
				if (_instance == null) {
					_instance = new Scheduler();
					_instance.setDaemon(true);
					_instance.start();
				}
			}
		}
		return _instance;
	}
	
	public static boolean isCurrentThread() {
		return Thread.currentThread() == _instance;
	}
	
	// ------------------------------------------------------------------------
	
	private Queue<Task> _queue = new ConcurrentLinkedQueue<Task>();
	private Queue<Task> _postq = new ConcurrentLinkedQueue<Task>();
	private Map<String,Task> _map = new ConcurrentHashMap<String,Task>();
	
	private Scheduler() {
		super("pv-scheduler");
	}
	
	public void waitOneCycle() {
		if (isCurrentThread()) return;
		if (_queue.isEmpty()) return;
		try {
			synchronized (_queue) {
				_queue.wait(/*2000*/);
			}
		} catch (Exception e) {}
	}
	
	public void run() {
		while (true) {
			long now = System.currentTimeMillis();
			long t = -1, s;
			boolean ranTask = false;
			
			// run tasks in sequence
			for (Iterator<Task> iter = _queue.iterator(); iter.hasNext();)
			{	
				Task task = iter.next();
				try {
					s = task.evaluate(now);
				} catch (Exception e) {
					e.printStackTrace();
					s = -1;
				}
				ranTask = true;
				
				if (s <= 0) {
					iter.remove();
					if (task.id() != null) {
						_map.remove(task.id());
					}
				} else {
					t = (t <= 0 ? s : (t < s ? t : s));
				}
			}
			synchronized (_queue) {
				_queue.notifyAll();
			}
			
			// if tasks were run, run the post-task queue
			if (ranTask) {
				for (Iterator<Task> iter = _postq.iterator(); iter.hasNext();)
				{	
					Task task = iter.next();
					try {
						s = task.evaluate(now);
					} catch (Exception e) {
						e.printStackTrace();
						s = -1;
					}
					if (s <= 0) iter.remove();
				}
			}
			
			// adjust timing to include execution time
			s = System.currentTimeMillis() - now;
			t = (s >= t ? 1 : (t-s));
			
			// sleep for requested time units
			try {
				synchronized (this) { 
					if (_queue.isEmpty()) {
						this.wait();
					} else {
						this.wait(t);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void add(Task task) {
		String id = task.id();
		if (id != null) {
			Task prev = _map.get(id);
			if (prev != null) {
				_queue.remove(prev);
			}
			_map.put(id, task);
		}
		synchronized (this) {
			_queue.add(task);
			notify();
		}
	}
	
	public void addPostTask(Task task) {
		_postq.add(task);
	}
	
	public Task cancel(String id) {
		Task task = _map.remove(id);
		return (task != null ? cancel(task) : null);
	}
	
	public Task cancel(Task task) {
		_queue.remove(task);
		return task;
	}
	
	public static interface Task {
		public String id();
		public long evaluate(long t);
	}
	
	public static class TaskAdapter implements Task {
		public String id() { return null; }
		public long evaluate(long t) { return -1; }
	}
	
}
