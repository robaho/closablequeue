package robaho.queue.sample;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SampleOrig {

    private static volatile BlockingQueue queue;

    private static final long start = System.currentTimeMillis();

    public static void main(String[] args)  {
        Thread.ofPlatform().start(SampleOrig::thirdActor);
        Thread.ofPlatform().start(SampleOrig::producer);
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
        while (true) {
            if (queue == null) {
                BlockingQueue q = new LinkedBlockingQueue();
                String consumerName = ts();
                Thread.ofVirtual().start(() -> consumer(consumerName, q));
                queue = q;
                System.out.println(ts() + " ThirdActor: Queue created");
            } else {
                System.out.println(ts() + " ThirdActor: Queue already active");
            }
            sleep(12_000);
        }
    }

    // close the shop after second message received
    private static void consumer(String name, BlockingQueue q) {
        try {
            int counter = 0;
            while (true) {
                q.take();
                counter++;
                System.out.println(ts() + " Consumer " + name + ": message received");
                if (counter >= 2 && q == queue) {
                    System.out.println(ts() + " Consumer " + name + ": close shop");
                    queue = null;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
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
