package pv.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ObjectPool<T> {

	protected Queue<T> _pool = new ConcurrentLinkedQueue<T>();
	protected AtomicInteger _size = new AtomicInteger(0);
	protected int _poolSize;
	
	public ObjectPool(int size) {
		_poolSize = size;
	}
	
	public T get() {
		T item = _pool.poll();
		if (item == null) {
			item = create();
		} else {
			_size.decrementAndGet();
		}
		return item;
	}
	
	public void reclaim(T item) {
		clear(item);
		if (_size.incrementAndGet() > _poolSize) {
			_size.decrementAndGet();
		} else {
			_pool.add(item);
		}
	}
	
	public abstract T create();
	public void clear(T item) {}
	
}
