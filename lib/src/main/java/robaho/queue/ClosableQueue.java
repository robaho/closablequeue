package robaho.queue;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A concurrent unbounded FIFO blocking queue that supports "close" semantics. all elements added to the queue prior to close()
 * are available to readers. Note, only some methods of the Queue interface are implemented.
 * @see java.util.Queue
 */
public class ClosableQueue<T> implements AutoCloseable {
    private boolean closed;
    private final Lock lock  = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final LinkedList<T> list = new LinkedList();

    @Override
    public void close() {
        lock.lock();
        try {
            closed=true;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }
    /**
     * Add an element to the queue.
     * @throws IllegalStateException if the queue is closed.
     */
    public void put(T e) {
        lock.lock();
        try {
            if(closed) throw new IllegalStateException("queue is closed");
            list.add(e);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }
    /**
     * Remove earliest element from the queue and return it.
     * @return the element or null if the queue is empty.
     * @throws IllegalStateException if the queue is closed.
     */
    public T poll() {
        lock.lock();
        try {
            if(closed) throw new IllegalStateException("queue is closed");
            return list.poll();
        } finally {
            lock.unlock();
        }
    }
    /**
     * Remove earliest element from the queue and return it, blocking until an element is available.
     * @return the element.
     * @throws IllegalStateException if the queue is closed while waiting
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while(true) {
                T t = list.poll();
                if(t!=null) return t;
                if(closed) throw new IllegalStateException("queue is closed");
                condition.await();
            }
        } finally {
            lock.unlock();
        }
    }
}
