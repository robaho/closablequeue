package robaho.queue.sample;

import robaho.queue.QueueClosedException;
import robaho.queue.SingleConsumerQueue;

public class SampleCorrect {

    private static final long start = System.currentTimeMillis();

    public static void main(String[] args)  {
        Thread.ofPlatform().start(SampleCorrect::thirdActor);
    }

    // every 2 seconds attempts to put something to queue, if possible
    private static void producer() {
        String name = ts();
        try(var queue = new SingleConsumerQueue()) {
            int counter = 0;
            Thread.startVirtualThread(() -> consumer(name,queue));
            while (true) { 
                sleep(2_000);
                System.out.println(ts() + " Producer: "+name+" enqueued message");
                queue.put("message");
                if(++counter>2) return; // this will close the queue and stop the consumer
            }
        }
    }

    // every 12 seconds attempts open new shop, if not exists
    private static void thirdActor() {
        Thread producer = null;
        while (true) {
            if(producer==null || !producer.isAlive()) {
                producer = Thread.startVirtualThread(SampleCorrect::producer);
            }
            sleep(12_000);
        }
    }

    // close the shop after second message received
    private static void consumer(String name,SingleConsumerQueue q) {
        try {
            while (true) {
                q.take();
                System.out.println(ts() + " Consumer " + name + ": message received");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (QueueClosedException expected) {
            System.out.println(ts() + " Consumer "+name+" ended");
            // our queue has been closed
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
