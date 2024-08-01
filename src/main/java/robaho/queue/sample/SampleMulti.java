package robaho.queue.sample;

import java.util.HashSet;
import java.util.Set;

import robaho.queue.QueueClosedException;
import robaho.queue.SingleConsumerQueue;

public class SampleMulti {

    private static final long start = System.currentTimeMillis();

    public static void main(String[] args)  {
        Thread.ofPlatform().start(SampleMulti::thirdActor);
    }

    // every 2 seconds attempts to put something to queue, if possible
    private static void producer(String name,SingleConsumerQueue queue) {
        try {
            while (!Thread.interrupted()) { 
                if(sleep(2_000)) return;
                System.out.println(ts() + " Producer: "+name+" enqueued message");
                queue.put("message");
            }
        } finally {
            System.out.println(ts() + " Producer: "+name+" ended");
        }
    }

    private static class ProducerSet {
        private final SingleConsumerQueue queue = new SingleConsumerQueue();
        private final Set<Thread> producers = new HashSet();
        private final String name = ts();
        private volatile boolean killed;
        private final Thread consumer;

        public ProducerSet() {
            for(int i=0;i<3;i++) {
                String producerName = name+"."+i;
                producers.add(Thread.startVirtualThread(() -> producer(producerName,queue)));
            }
            consumer = Thread.startVirtualThread(() -> consumer(this,name,queue));
        }
        public void kill() {
            killed=true;
            for(var p : producers) {
                p.interrupt();
            }
        }
        public boolean isAlive() {
            if(!killed) return true;
            // wait for producers to finish
            for(var thread : producers) {
                try {
                    thread.join();
                } catch (InterruptedException ignored) {
                }
            }
            queue.close();
            try {
                consumer.join();
            } catch (InterruptedException ignored) {
            }
            return false;
        }
    }

    // every 12 seconds attempts open new shop, if not exists
    private static void thirdActor() {
        ProducerSet producer = null;
        while (true) {
            if(producer==null || !producer.isAlive()) {
                producer = new ProducerSet();
            }
            sleep(12_000);
        }
    }

    // close the shop after second message received
    private static void consumer(ProducerSet ps,String name,SingleConsumerQueue q) {
        try {
            int counter=0;
            while (true) {
                q.take();
                System.out.println(ts() + " Consumer " + name + ": message received");
                if(counter++>2) ps.kill();
            }
        } catch (InterruptedException shouldNotHappen) {
            shouldNotHappen.printStackTrace();
        } catch (QueueClosedException expected) {
            System.out.println(ts() + " Consumer "+name+" ended");
        }
    }

    // return true if interrupted
    private static boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return false;
        } catch (InterruptedException e) {
            return true;
        }
    }

    private static String ts() {
        long now  = System.currentTimeMillis();
        return String.valueOf ((now - start) / 1000);
    }
}
