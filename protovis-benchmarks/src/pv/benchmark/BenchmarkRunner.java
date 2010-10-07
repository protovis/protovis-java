package pv.benchmark;

import java.io.PrintStream;

import pv.util.ThreadPool;

public class BenchmarkRunner {

	public static BenchmarkRunner instance = new BenchmarkRunner();
	
	private PrintStream out = System.out;
	private int iter = 6;
	private int minThreadCount = 5;
	private int maxThreadCount = 5;//Runtime.getRuntime().availableProcessors() + 2;
	
	public BenchmarkRunner() {
		System.out.println("AVAILABLE PROCESSSORS = " + Runtime.getRuntime().availableProcessors());
	}
	
	public void printStream(PrintStream ps) { out = ps; }
	public void iterations(int iters) { iter = iters; }
	
	public void run(String label, Benchmark b) {
		for (int nt=minThreadCount; nt<=maxThreadCount; ++nt) {
			ThreadPool.setThreadCount(nt);
			b.setup();
			for (int i=0; i<iter; ++i) {
				long[] t = b.run(1);
				double avg = average(t);
				out.println(nt + "\t" + (label==null?"":label+"-")+b.name() + "\t" + avg);
			}
			b.takedown();
		}
	}
	
	public static double average(long[] t) {
		double sum = 0;
		for (int i=0; i<t.length; ++i) {
			sum += t[i] / 1000.0;
		}
		return sum / t.length;
	}
	
}
