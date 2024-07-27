package robaho.queue;

import java.util.Collection;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A concurrent unbounded FIFO blocking queue that supports "close" semantics. all elements added to the queue prior to close()
 * are available to readers. Note, only some methods of the Queue interface are implemented.
 * @see java.util.Queue
 */
public class ClosableQueue<T> extends AbstractClosableQueue<T> {
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    @Override
    protected void lock() {
        lock.lock();
    }
    @Override
    protected void unlock() {
        lock.unlock();
    }
    @Override
    protected void wakeupReaders() {
        condition.signalAll();
    }
    @Override
    protected void wakeupReader() {
        condition.signal();
    }

    /**
     * Drain all of the elements of the queue up to maxElements into the provided collection. If the queue is empty, the method returns immediately.
     * @param c is the non-null Collection to receive the elements.
     * @param maxElements is the maximum number of elements to drain.
     * @throws QueueClosedException if the queue is closed.
     */
    @Override
    public int drainToBlocking(Collection<? super T> c) throws InterruptedException {
        lock.lock();
        try {
            while (true) { 
                checkClosed();
                if(list.isEmpty()) {
                    condition.await();
                } else {
                    int count=0;
                    for(T e;(e=list.poll())!=null;count++) {
                        c.add(e);
                    }
                    return count;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove earliest element from the queue and return it, blocking until an element is available.
     * @return the element.
     * @throws QueueClosedException if the queue is closed while waiting
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    @Override
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while(true) {
                T t = list.poll();
                if(t!=null) return t;
                checkClosed();
                condition.await();
            }
        } finally {
            lock.unlock();
        }
    }
}
