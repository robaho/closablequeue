package robaho.queue;

import java.util.Collection;
import java.util.LinkedList;

public abstract class AbstractClosableQueue<T> implements AutoCloseable {
    private boolean closed;
    protected final LinkedList<T> list = new LinkedList();

    public static class QueueClosedException extends IllegalStateException {
        QueueClosedException() {
            super("queue is closed");
        }
    }

    @Override
    public void close() {
        lock();
        try {
            closed=true;
            wakeupReaders();
        } finally {
            unlock();
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
     * @throws IllegalStateException if the queue is closed.
     */
    public void put(T e) {
        lock();
        try {
            checkClosed();
            list.add(e);
            wakeupReader();
        } finally {
            unlock();
        }
    }
    /**
     * Add all elements from a Collection to the queue.
     * @throws IllegalStateException if the queue is closed.
     */
    public void putAll(Collection<? extends T> c) {
        lock();
        try {
            checkClosed();
            list.addAll(c);
            if(c.size()>1) wakeupReaders(); else wakeupReader();
        } finally {
            unlock();
        }
    }
    /**
     * Remove earliest element from the queue and return it.
     * @return the element or null if the queue is empty.
     * @throws IllegalStateException if the queue is closed.
     */
    public T poll() {
        lock();
        try {
            checkClosed();
            return list.poll();
        } finally {
            unlock();
        }
    }
    /**
     * returns the earliest element from the queue but does not remove it.
     * @return the element or null if the queue is empty.
     * @throws IllegalStateException if the queue is closed.
     */
    public T peek() {
        lock();
        try {
            checkClosed();
            return list.peek();
        } finally {
            unlock();
        }
    }
    /**
     * returns the earliest element from the queue but does not remove it.
     * @return the element or null if the queue is empty.
     * @throws IllegalStateException if the queue is closed.
     */
    public T[] toArray(T[] array) {
        lock();
        try {
            checkClosed();
            return list.toArray(array);
        } finally {
            unlock();
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
        lock();
        try {
            checkClosed();
            int count=0;
            for(T e;(e=list.poll())!=null && count < maxElements;count++) {
                c.add(e);
            }
            return count;
        } finally {
            unlock();
        }
    }

    protected abstract void lock();
    protected abstract void unlock();
    protected abstract void wakeupReaders();
    protected abstract void wakeupReader();
    public abstract T take() throws InterruptedException;
    public abstract int drainToBlocking(Collection<? super T> collection) throws InterruptedException;
}