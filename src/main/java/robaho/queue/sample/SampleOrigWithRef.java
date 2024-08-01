package robaho.queue.sample;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * producer/consumer that relies on the VT auto-cleanup
 */
public class SampleOrigWithRef {

    private static volatile BlockingQueue queue;

    private static final ReferenceQueue refQueue = new ReferenceQueue();
    private static final ReferenceQueue refQueue2 = new ReferenceQueue();
    private static final Set<Reference> refs = Collections.synchronizedSet(new HashSet());

    private static final long start = System.currentTimeMillis();

    public static void main(String[] args) throws InterruptedException  {
        Thread.ofPlatform().start(SampleOrigWithRef::thirdActor);
        Thread.ofPlatform().start(SampleOrigWithRef::producer);
        Thread.ofPlatform().start(() -> {
            while (true) { 
                try {
                    var ref = refQueue.remove();
                    System.out.println("queue collected: "+ref+", thread count "+Thread.getAllStackTraces().size());
                } catch (InterruptedException ex) {
                }
            }
        });
        Thread.ofPlatform().start(() -> {
            while (true) { 
                try {
                    var ref = refQueue2.remove();
                    System.out.println("consumer object collected: "+ref+", thread count "+Thread.getAllStackTraces().size());
                } catch (InterruptedException ex) {
                }
            }
        });
    }

    // every 2 seconds attempts to put something to queue, if possible
    private static void producer() {
        while (true) {
            BlockingQueue tmp = queue;
            sleep(2_000);
            if (tmp == null) {
                System.out.println(ts() + " Producer: no queue");
            } else {
                System.out.println(ts() + " Producer: enqueued message");
                tmp.add("message");
            }
        }
    }

    // every 12 seconds attempts open new shop, if not exists
    private static void thirdActor() {
        Thread consumer=null;
        while (true) {
            if (queue == null) {
                // enable this to terminate consumer properly
                if(consumer!=null) {
                    consumer.interrupt();
                    try {
                        consumer.join();
                    } catch (InterruptedException ex) {
                    }
                }
                BlockingQueue q = new LinkedBlockingQueue();
                refs.add(new PhantomReference(q,refQueue));
                String consumerName = ts();
                consumer = Thread.ofVirtual().start(() -> consumer(consumerName, q));
                // consumer = Thread.ofPlatform().start(() -> consumer(consumerName, q));
                queue = q;
                System.out.println(ts() + " ThirdActor: Queue created");
            } else {
                System.out.println(ts() + " ThirdActor: Queue already active");
            }
            System.gc();
            sleep(12_000);
            System.gc();
        }
    }

    private static class StatsCounter {
        int count=0;
        @Override
        public String toString() {
            return Integer.toString(count);
        }
    }

    // close the shop after second message received
    private static void consumer(String name, BlockingQueue q) {
        var stats = new StatsCounter();
        refs.add(new PhantomReference<>(stats,refQueue2));
        try {
            int counter = 0;
            while (true) {
                // the following line is the "magic"... if q is no longer referenced by elsewhere this
                // thread will disappear during the take() and any resources used released
                q.take();
                counter++;
                stats.count++;
                System.out.println(ts() + " Consumer " + name + ": message received");
                if (counter >= 2 && q == queue) {
                    System.out.println(ts() + " Consumer " + name + ": close shop");
                    queue = null;
                }
            }
        } catch (InterruptedException e) {
            System.out.println("consumer "+name+" interrupted");
        } finally {
            System.out.println("consumer ended, stats = "+stats);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String ts() {
        long now  = System.currentTimeMillis();
        return String.valueOf ((now - start) / 1000);
    }
}
