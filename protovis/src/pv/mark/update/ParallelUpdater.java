package pv.mark.update;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import pv.animate.Transition;
import pv.mark.Mark;
import pv.mark.MarkEvent;
import pv.mark.Mark.PropertySet;
import pv.mark.constants.Events;
import pv.mark.constants.MarkType;
import pv.mark.eval.Evaluator;
import pv.mark.eval.EvaluatorBuilder;
import pv.mark.eval.LinkEvaluator;
import pv.scene.GroupItem;
import pv.scene.Item;
import pv.scene.PanelItem;
import pv.util.ThreadPool;

public class ParallelUpdater extends MarkUpdater {

	protected EvaluatorBuilder _compiler = EvaluatorBuilder.instance();
	
	private Queue<Future<?>> _futures = new ConcurrentLinkedQueue<Future<?>>();
	private Queue<GraphTask> _links = new ConcurrentLinkedQueue<GraphTask>();
	
	public ParallelUpdater() {
	}
	
	private <T> Future<T> submit(Callable<T> task) {
		return ThreadPool.getThreadPool().submit(task);
	}
	
	// ------------------------------------------------------------------------
	
	public void update(Mark mark, GroupItem proto, PanelItem panel, Transition t)
	{
		//long t0 = System.currentTimeMillis();
		
		// bind properties
		bind(mark);
		
		// build scenegraph
		build(mark, proto, panel, t);
		waitForFutures();
		
		// build graph links
		if (!_links.isEmpty()) {
			try {
				ThreadPool.getThreadPool().invokeAll(_links);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				_links.clear();
			}
		}
		
		//long t1 = System.currentTimeMillis();
		
		// fire update event
		MarkEvent.fire(MarkEvent.create(Events.update), mark.scene());
		
		//long t2 = System.currentTimeMillis();
		
		// evaluate properties
		GroupItem g = (GroupItem) panel.item(mark.treeIndex()); 
		evaluate(mark, g, t);
		waitForFutures();
//		evaluateSerial(mark, layer, t);
		
		//long t3 = System.currentTimeMillis();
		//System.out.print("* "+(t1-t0)/1000f + "\t" + (t2-t1)/1000f + "\t" + (t3-t2)/1000f + "\t");
	}
	
	// ------------------------------------------------------------------------
	
	// -- handle operations ----
	
	private void bind(Mark mark) {
		// bind properties and compile if needed
		PropertySet pset = mark.bind();
		if (pset.dirty) {
			mark.evaluator(_compiler.build(mark));
		}
		for (Mark child : mark.children())
			bind(child);
	}
	
	private void build(Mark mark, GroupItem proto, PanelItem panel, Transition t) {
		_futures.add(submit(buildTask(mark, proto, panel, t)));
	}
		
	private void evaluate(Mark mark, GroupItem item, Transition t) {
		int size = item.size();
		int blockSize = Math.max(1, 1 + size / ThreadPool.getThreadCount());
		int numTasks =  Math.max(1, (int)Math.ceil(((double)size)/blockSize));
		AtomicInteger count = new AtomicInteger(0);
		
		for (int i=0; i<size; i+=blockSize) {
			_futures.add(submit(
				evalTask(mark, item, i, i+blockSize, t, count, numTasks)));
		}
	}
	
//	private void evaluateSerial(Mark mark, PanelItem panel, Transition t)
//	{
//		int index = mark.treeIndex();
//		GroupItem group = (GroupItem) panel.item(index);
//		Evaluator eval = mark.evaluator();
//		
//		// evaluate marks
//		eval.evaluate(group, 0, group.size(), t!=null);
//		// create animators if needed
//		if (t != null) t.add(eval.transition(group));
//		
//		// recurse
//		if (group.type == MarkType.Panel) {
//			for (int i=0; i<group.size(); ++i) {
//				Item item = group.item(i);
//				if (item.visible) {
//					PanelItem layerItem = (PanelItem) item;
//					for (Mark child : mark.children()) {
//						evaluateSerial(child, layerItem, t);
//					}
//				}
//			}
//		} else {
//			for (Mark child : mark.children()) {
//				evaluateSerial(child, panel, t);
//			}
//		}
//	}
	
	private void waitForFutures() {
		while (!_futures.isEmpty()) {
			try {
				_futures.remove().get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}
	
	// -- get task instances --------------------------------------------------
	
	private BuildTask buildTask(Mark mark, GroupItem proto, PanelItem panel, Transition t) {
		return new BuildTask(mark, proto, panel, t);
	}
	
	private EvaluateTask evalTask(Mark mark, GroupItem group, int start, int end,
		Transition t, AtomicInteger count, int numTasks)
	{
		return new EvaluateTask(mark, group, start, end, t, count, numTasks);
	}
	
	// ------------------------------------------------------------------------
	
	public class GraphTask implements Callable<GroupItem> {
		public Mark mark;
		public GroupItem group;
		
		public GraphTask(Mark mark, GroupItem group) {
			this.mark = mark;
			this.group = group;
		}

		public GroupItem call() throws Exception {
			LinkEvaluator eval = (LinkEvaluator) mark.evaluator();
			eval.buildGraph(group);
			return group;
		}
		
	}
	
	public class BuildTask implements Callable<GroupItem> {
		public Mark mark;
		public GroupItem proto;
		public PanelItem panel;
		public Transition trans;
		public int pidx;
		
		public BuildTask(Mark mark, GroupItem proto, PanelItem panel, Transition trans) {
			this.mark = mark;
			this.proto = proto;
			this.panel = panel;
			this.trans = trans;
		}
		
		public GroupItem call() throws Exception
		{
			// generate group item
			GroupItem group = mark.evaluator().build(mark, proto, panel, trans!=null);
			if (group.type == MarkType.Link) {
				_links.add(new GraphTask(mark, group));
			}
			
			// recurse
			if (group.type == MarkType.Panel) {
				// if we're building a panel, build child marks now
				for (int i=0; i<group.size(); ++i) {
					Item item = group.item(i);
					if (item.visible) {
						PanelItem layerItem = (PanelItem) item;
						for (Mark child : mark.children()) {
							build(child, null, layerItem, trans);
						}
						//layerItem.discard(nidx);
					}
				}
			} else {
				// if we're not a panel, build inheriting marks now
				// compute child mark values, add as children of item
				for (Mark child : mark.children()) {
					build(child, group, panel, trans);
				}
				// remove extra items if length changes
				//layerItem.discard(pidx);
			}
			
			return group;
		}
	}
	
	public class EvaluateTask implements Callable<GroupItem> {
		
		public Mark mark;
		public GroupItem group;
		public int start, end;
		public Transition trans;
		public AtomicInteger count;
		public int numTasks;
		
		public EvaluateTask(Mark mark, GroupItem group, int start, int end,
			Transition trans, AtomicInteger count, int numTasks)
		{
			this.mark = mark;
			this.group = group;
			this.start = start;
			this.end = end > group.size() ? group.size() : end;
			this.trans = trans;
			this.count = count;
			this.numTasks = numTasks;
		}	
		
		public GroupItem call() throws Exception {
			Evaluator eval = mark.evaluator();
			
			//long t0 = System.currentTimeMillis();
			eval.evaluate(group, start, end, trans!=null);
			//long t1= System.currentTimeMillis();
			//System.out.println("\t"+start+"-"+end+"("+numTasks+") - "+(t1-t0)/1000f);
			
			
			if (count.incrementAndGet() >= numTasks) {
				PanelItem panel = (PanelItem) group.group;
				// recurse
				if (group.type == MarkType.Panel) {
					for (int i=0; i<group.size(); ++i) {
						Item item = group.item(i);
						if (!item.visible) continue;
						PanelItem panelItem = (PanelItem) item;
						for (Mark child : mark.children()) {
							GroupItem g = (GroupItem) panelItem.item(child.treeIndex());
							evaluate(child, g, trans);
						}
					}
				} else {
					for (Mark child : mark.children()) {
						GroupItem g = (GroupItem) panel.item(child.treeIndex());
						evaluate(child, g, trans);
					}
				}
				if (trans != null) trans.add(eval.transition(group));
			}
			return group;
		}
	}
		
}
