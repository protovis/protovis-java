package pv.benchmark;

public interface Benchmark {
	public String name();
	public void setup();
	public void takedown();
	public long[] run(int iterations);
}