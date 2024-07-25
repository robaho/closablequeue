package robaho.queue;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
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
    /**
     * Close the queue. Any elements already in the queue are available to subsequent take() operations. Any take() when the
     * queue is closed will throw an IllegalStateException. Any put() after the queue is closed will also throw an IllegalStateException.
     */
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
     * returns the earliest element from the queue but does not remove it.
     * @return the element or null if the queue is empty.
     * @throws IllegalStateException if the queue is closed.
     */
    public T peek() {
        lock.lock();
        try {
            if(closed) throw new IllegalStateException("queue is closed");
            return list.peek();
        } finally {
            lock.unlock();
        }
    }
    /**
     * returns the earliest element from the queue but does not remove it.
     * @return the element or null if the queue is empty.
     * @throws IllegalStateException if the queue is closed.
     */
    public T[] toArray(T[] array) {
        lock.lock();
        try {
            if(closed) throw new IllegalStateException("queue is closed");
            return list.toArray(array);
        } finally {
            lock.unlock();
        }
    }
    /**
     * Drain all of the elements of the queue into the provided collection. If the queue is empty, the method returns immediately.
     * @param c is the non-null Collection to receive the elements.
     * @throws IllegalStateException if the queue is closed.
     */
    public int drainTo(Collection<? super T> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    /**
     * Drain all of the elements of the queue up to maxElements into the provided collection. If the queue is empty, the method returns immediately.
     * @param c is the non-null Collection to receive the elements.
     * @param maxElements is the maximum number of elements to drain.
     * @throws IllegalStateException if the queue is closed.
     */
    public int drainTo(Collection<? super T> c, int maxElements) {
        lock.lock();
        try {
            if(closed) throw new IllegalStateException("queue is closed");
            int count=0;
            for(T e;(e=list.poll())!=null && count < maxElements;count++) {
                c.add(e);
            }
            return count;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Drain all of the elements of the queue up to maxElements into the provided collection. If the queue is empty, the method returns immediately.
     * @param c is the non-null Collection to receive the elements.
     * @param maxElements is the maximum number of elements to drain.
     * @throws IllegalStateException if the queue is closed.
     */
    public int drainToBlocking(Collection<? super T> c) throws InterruptedException {
        lock.lock();
        try {
            while (true) { 
                if(closed) throw new IllegalStateException("queue is closed");
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
