package robaho.queue;

import java.util.Collection;
import java.util.LinkedList;
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
    private boolean closed;
    protected final LinkedList<T> list = new LinkedList();

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
     * check if queue is closed and if so throw QueueClosedException
     */
    protected void checkClosed() {
        if(closed) throw new QueueClosedException();
    }
    /**
     * Add an element to the queue.
     * @throws QueueClosedException if the queue is closed.
     */
    public void put(T e) {
        lock.lock();
        try {
            checkClosed();
            list.add(e);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }
    /**
     * Add all elements from a Collection to the queue.
     * @throws QueueClosedException if the queue is closed.
     */
    public void putAll(Collection<? extends T> c) {
        lock.lock();
        try {
            checkClosed();
            list.addAll(c);
            if(c.size()>1) condition.signalAll(); else condition.signal();
        } finally {
            lock.unlock();
        }
    }
    /**
     * Remove earliest element from the queue and return it.
     * @return the element or null if the queue is empty.
     * @throws QueueClosedException if the queue is closed.
     */
    public T poll() {
        lock.lock();
        try {
            checkClosed();
            return list.poll();
        } finally {
            lock.unlock();
        }
    }
    /**
     * returns the earliest element from the queue but does not remove it.
     * @return the element or null if the queue is empty.
     * @throws QueueClosedException if the queue is closed.
     */
    public T peek() {
        lock.lock();
        try {
            checkClosed();
            return list.peek();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Drain all of the elements of the queue up to maxElements into the provided collection. If the queue is empty, the method returns immediately.
     * @param c is the non-null Collection to receive the elements.
     * @param maxElements is the maximum number of elements to drain.
     * @throws QueueClosedException if the queue is closed.
     */
    public int drainTo(Collection<? super T> c, int maxElements) {
        lock.lock();
        try {
            checkClosed();
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
