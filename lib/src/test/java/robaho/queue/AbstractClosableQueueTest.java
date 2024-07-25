package robaho.queue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractClosableQueueTest {
    abstract protected AbstractClosableQueue<Integer> createQueue();
    @Test void ensureAllElementsAreRead() throws InterruptedException {
        var queue = createQueue();
        queue.put(1);
        queue.put(2);
        queue.put(3);
        queue.close();

        assertEquals(1,queue.take());
        assertEquals(2,queue.take());
        assertEquals(3,queue.take());
    }
    @Test 
    void ensureAllElementsAreReadUsingThread() throws InterruptedException {
        AtomicInteger count = new AtomicInteger();
        Thread t;
        try(var queue = createQueue()) {
            t = Thread.startVirtualThread(() -> {
                while(true) {
                    try {
                        queue.take();
                        count.incrementAndGet();
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
            });
            queue.put(1);
            queue.put(2);
            queue.put(3);
        }
        t.join();
        assertEquals(3,count.get());
    }
    @Test void ensureEmptyReadThrowsOnClosedQueued() throws InterruptedException {
        var queue = createQueue();
        queue.close();

        assertThrows(IllegalStateException.class,() -> queue.take());
        assertThrows(IllegalStateException.class,() -> queue.take());
        assertThrows(IllegalStateException.class,() -> queue.take());
    }
    @Test void ensureWaitingEmptyReadThrowsOnClosedQueued() throws InterruptedException {
        var queue = createQueue();
        Lock lock = new ReentrantLock();
        Condition c = lock.newCondition();

        lock.lock();
        Thread t = Thread.startVirtualThread(() -> {
                while(true) {
                    try {
                        lock.lock();
                        try {
                            c.signal();
                        } finally {
                            lock.unlock();
                        }
                        queue.take();
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
            });
        c.await();
        // give some time for vt to enter take()
        Thread.sleep(Duration.ofSeconds(1));
        queue.close();
        assertTrue(t.join(Duration.ofSeconds(5)));
    }
    @Test void drainQueue() throws InterruptedException {
        var queue = createQueue();
        ArrayList<Integer> list = new ArrayList();
        queue.put(1);
        queue.put(2);
        queue.put(3);
        queue.drainTo(list);
        assertEquals(1,list.get(0));
        assertEquals(2,list.get(1));
        assertEquals(3,list.get(2));
    }
    @Test void drainQueueMaxElements() throws InterruptedException {
        var queue = createQueue();
        ArrayList<Integer> list = new ArrayList();
        queue.put(1);
        queue.put(2);
        queue.put(3);
        queue.drainTo(list,2);
        assertEquals(2,list.size());
        assertEquals(1,list.get(0));
        assertEquals(2,list.get(1));
    }
}
