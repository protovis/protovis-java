package pv.util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ThreadPool {
	
	private static int _numThreads = Runtime.getRuntime().availableProcessors();
	private static ExecutorService _pool;
	
	private static ThreadGroup _tg = null;
	private static ThreadFactory _tf = new ThreadFactory() {
		private int id = 0;
		public Thread newThread(Runnable r) {
			Thread t = new Thread(_tg, r);
			t.setName("pv-worker-"+(id++));
			t.setDaemon(true);
			t.setPriority(Thread.MAX_PRIORITY);
			return t;
		}
	};
	
	public static void setThreadCount(int numThreads) {
		if (numThreads == _numThreads) return;
		_numThreads = numThreads;
		shutdown();
	}
	
	public static int getThreadCount() {
		return _numThreads;
	}
	
	public static ExecutorService getThreadPool() {
		return getThreadPool(_numThreads);
	}
	
	private static ExecutorService getThreadPool(int nThreads) {
		if (_pool == null) {
			synchronized (ThreadPool.class) {
				if (_pool == null) {
					_tg = new ThreadGroup("Protovis");
					_tg.setDaemon(true);
					_pool = Executors.newFixedThreadPool(nThreads, _tf);
				}
			}
		}
		return _pool;
	}
	
	public static void shutdown() {
		if (_pool != null) {
			synchronized (ThreadPool.class) {
				if (_pool != null) {
					_pool.shutdown();
					_pool = null;
					_tg = null;
				}
				
			}
		}
	}
	
	public static List<Runnable> shutdownNow() {
		List<Runnable> list = null;
		if (_pool != null) {
			synchronized (ThreadPool.class) {
				if (_pool != null) {
					list = _pool.shutdownNow();
					_pool = null;
					_tg = null;
				}
			}
		}
		return list;
	}
	
}