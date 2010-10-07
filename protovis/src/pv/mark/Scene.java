package pv.mark;

import pv.animate.Parallel;
import pv.animate.Scheduler;
import pv.animate.Transition;
import pv.mark.update.MarkUpdater;
import pv.scene.Item;
import pv.scene.PanelItem;

public class Scene extends Panel {

	private PanelItem _items = new PanelItem();
	
	public Scene() {
		super();
		_scene = this;
		_items.group = _items; // XXX ?
	}
	
	@Override
	public Item items() {
		return _items;
	}
	
	public void clear() {
		_items.items.clear();
	}
	
	public void update() {
		update(0);
	}
	
	public void updateAndWait() {
		update(0);
		Scheduler.instance().waitOneCycle();
	}
	
	public Transition update(double duration)
	{
		return buildItems(duration);
	}
	
	public Transition updateAndWait(double duration) {
		Transition t = update(duration);
		Scheduler.instance().waitOneCycle();
		return t;
	}
	
	protected Transition buildItems(double duration) {
		setTreeIndex(-1);
		if (_items.size() == 0) _items.add(null);
		
		Parallel p = duration<=0 ? null : new Parallel(duration);
		MarkUpdater.instance().submit(this, null, _items, p);
		return p;
	}
	
	// -----
	
	public void repeatedUpdate(final boolean b) {
		if (b) {
			repeatedUpdate(0.015);
		} else {
			String id = "repeated-update-"+this.toString();
			Scheduler.Task t = Scheduler.instance().cancel(id);
			if (t instanceof UpdateTask) ((UpdateTask)t).cancel();
		}
	}
	
	public void repeatedUpdate(final double pause) {
		repeatedUpdate(pause, -1);
	}
	
	public void repeatedUpdate(final double pause, final int iterations)
	{
		long p = (long)(1000 * pause);
		Scheduler.instance().add(new UpdateTask(p, iterations, -1));
	}
	
	public void repeatedUpdate(final double pause, final double duration) {
		long p = (long)(1000 * pause);
		long end = System.currentTimeMillis() + (long)(1000*duration);
		Scheduler.instance().add(new UpdateTask(p, -1, end));
	}
	
	public class UpdateTask implements Scheduler.Task
	{
		private final String _id = "repeated-update-"+Scene.this.toString();
		private boolean _cancel = false;
		private long _pause = 15, _end = -1;
		private int _iter = -1;
		
		public UpdateTask(long pause, int iter, long end) {
			this._pause = pause;
			this._iter = iter;
			this._end = end;
		}
		
		public long evaluate(long t) {
			if (_cancel) return -1;
			update();
			if (_iter > 0 && --_iter == 0) return -1;
			if (_end > -1 && _end < t) return -1;
			return _pause;
		}

		public void cancel() { _cancel = true; }
		
		public String id() { return _id; }
	}

}
