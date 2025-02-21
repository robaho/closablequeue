package robaho.queue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
@Fork(3)
@Warmup(iterations = 1)
@Measurement(iterations = 3, time = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ClosableQueueBenchmark {
    @Benchmark
    @OperationsPerInvocation(1000000)
    public void testClosableQueue() throws InterruptedException {
        var queue = new ClosableQueue<Integer>();
        var thread = Thread.startVirtualThread(() -> {
            try {
                while(true) {
                    queue.take();
                }
            } catch (InterruptedException ex) {
                throw new Error("unexpected interrupt");
            } catch (QueueClosedException expected) {
            }
        });
        for(int i=0;i<1000000;i++) {
            queue.put(i);
        }
        queue.close();
        thread.join();
    }
    @Benchmark
    @OperationsPerInvocation(1000000)
    public void testSingleConsumerQueue() throws InterruptedException {
        var queue = new SingleConsumerQueue<Integer>();
        var thread = Thread.startVirtualThread(() -> {
                try {
                    while(true) {
                        queue.take();
                    }
                } catch (InterruptedException ex) {
                    throw new Error("unexpected interrupt");
                } catch (QueueClosedException expected) {
                }
            }); 
            
        for(int i=0;i<1000000;i++) {
            queue.put(i);
        }
        queue.close();
        thread.join();
    }

    @Benchmark
    @OperationsPerInvocation(1000000)
    public void testLinkedBlockingQueue() throws InterruptedException {
        var queue = new LinkedBlockingQueue<Integer>();
        var thread = Thread.startVirtualThread(() -> {
            try {
                int count=0;
                while(true) {
                    queue.take();
                    if(++count==1000000) return;
                }
            } catch (InterruptedException ex) {
                throw new Error("unexpected interrupt");
            }
        });
        for(int i=0;i<1000000;i++) {
            queue.put(i);
        }
        thread.join();
    }
    @Benchmark
    @OperationsPerInvocation(1000000)
    public void testLinkedTransferQueue() throws InterruptedException {
        var queue = new LinkedTransferQueue<Integer>();
        var thread = Thread.startVirtualThread(() -> {
            try {
                int count=0;
                while(true) {
                    queue.take();
                    if(++count==1000000) return;
                }
            } catch (InterruptedException ex) {
                throw new Error("unexpected interrupt");
            }
        });
        for(int i=0;i<1000000;i++) {
            queue.put(i);
        }
        thread.join();
    }
    @Benchmark
    @OperationsPerInvocation(1000000)
    public void testLinkedTransferQueueUsingTransfer() throws InterruptedException {
        var queue = new LinkedTransferQueue<Integer>();
        var thread = Thread.startVirtualThread(() -> {
            try {
                int count=0;
                while(true) {
                    queue.take();
                    if(++count==1000000) return;
                }
            } catch (InterruptedException ex) {
                throw new Error("unexpected interrupt");
            }
        });
        for(int i=0;i<1000000;i++) {
            queue.transfer(i);
        }
        thread.join();
    }

}