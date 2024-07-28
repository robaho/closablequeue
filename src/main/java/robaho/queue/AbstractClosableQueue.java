package robaho.queue;

import java.util.Collection;

public abstract class AbstractClosableQueue<T> implements AutoCloseable {
    public static class QueueClosedException extends IllegalStateException {
        QueueClosedException() {
            super("queue is closed");
        }
    }

    /**
     * Close the queue. Any further put() operations after which will fail with an IllegalStateException.
     * Any read operations will succeed until the queue is empty, after which a QueueClosedException will be thrown.
     * Closing an already closed queue is a no-op.
     */
    public abstract void close();
    /**
     * Add an element to the queue.
     * @throws QueueClosedException if the queue is closed.
     */
    public abstract void put(T e);
    /**
     * Add all elements from a Collection to the queue.
     * @throws QueueClosedException if the queue is closed.
     */
    public abstract void putAll(Collection<? extends T> c);
    /**
     * Remove earliest element from the queue and return it.
     * @return the element or null if the queue is empty.
     * @throws QueueClosedException if the queue is closed.
     */
    public abstract T poll();
    /**
     * returns the earliest element from the queue but does not remove it.
     * @return the element or null if the queue is empty.
     * @throws QueueClosedException if the queue is closed.
     */
    public abstract T peek();
    /**
     * Remove earliest element from the queue and return it, blocking until an element is available.
     * @return the element.
     * @throws QueueClosedException if the queue is closed while waiting
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public abstract T take() throws InterruptedException;
    /**
     * Drain all of the elements of the queue into the provided collection. If the queue is empty, the method returns immediately.
     * @param c is the non-null Collection to receive the elements.
     * @throws QueueClosedException if the queue is closed.
     */
    public int drainTo(Collection<? super T> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }
    /**
     * Drain all of the elements of the queue up to maxElements into the provided collection. If the queue is empty, the method returns immediately.
     * @param c is the non-null Collection to receive the elements.
     * @param maxElements is the maximum number of elements to drain.
     * @throws QueueClosedException if the queue is closed.
     */
    public abstract int drainTo(Collection<? super T> c, int maxElements);
    public abstract int drainToBlocking(Collection<? super T> collection) throws InterruptedException;
}