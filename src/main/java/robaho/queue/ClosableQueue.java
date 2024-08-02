package robaho.queue;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A concurrent unbounded FIFO blocking queue that supports "close" semantics. all elements added to the queue prior to close()
 * are available to readers. Note, only some methods of the Queue interface are implemented.
 * @see java.util.Queue
 */
public class ClosableQueue<T> extends AbstractClosableQueue<T> {
    /** Lock held by take, poll, etc */
    private final ReentrantLock takeLock = new ReentrantLock();
    /** Wait queue for waiting takes */
    private final Condition notEmpty = takeLock.newCondition();
    /** Lock held by put, offer, etc */
    private final ReentrantLock putLock = new ReentrantLock();

    private static class Node<T> {
        Node<T> next;
        T element;
        private Node(T e) {
            element=e;
        }
    }
    private static final Node CLOSED = new Node(null);
    private Node<T> head = new Node(null);
    private Node<T> tail = head;
    /** Current number of elements */
    private final AtomicInteger count = new AtomicInteger();

        /**
     * Links node at end of queue.
     *
     * @param node the node
     */
    private void enqueue(Node<T> node) {
        // assert putLock.isHeldByCurrentThread();
        // assert last.next == null;
        tail = tail.next = node;
    }

    /**
     * Removes a node from head of queue.
     *
     * @return the node
     */
    private T dequeue() {
        // assert takeLock.isHeldByCurrentThread();
        // assert head.item == null;
        Node<T> h = head;
        Node<T> first = h.next;
        h.next = h; // help GC
        head = first;
        T x = first.element;
        first.element = null;
        return x;
    }


    @Override
    public void close() {
        putLock.lock();
        try {
            enqueue(CLOSED);
            count.incrementAndGet();
        } finally {
            putLock.unlock();
        }
        signalNotEmpty();
    }
    /**
     * Add an element to the queue.
     * @throws QueueClosedException if the queue is closed.
     */
    public void put(T e) {
        int c;
        putLock.lock();
        try {
            if(tail==CLOSED) throw new QueueClosedException();
            tail=tail.next=new Node(e);
            c = count.getAndIncrement();
        } finally {
            putLock.unlock();
        }
        if(c==0) signalNotEmpty();
    }
    /**
     * Add all elements from a Collection to the queue.
     * @throws QueueClosedException if the queue is closed.
     */
    public void putAll(Collection<? extends T> collection) {
        int c;
        putLock.lock();
        try {
            if(tail==CLOSED) throw new QueueClosedException();
            int n=0;
            for(var e : collection) {
                enqueue(new Node(e));
                n++;
            }
            c = count.getAndAdd(n);
        } finally {
            putLock.unlock();
        }
        if(c==0) signalNotEmpty();
    }
    /**
     * Remove earliest element from the queue and return it.
     * @return the element or null if the queue is empty.
     * @throws QueueClosedException if the queue is closed.
     */
    public T poll() {
        takeLock.lock();
        try {
            if(head.next==CLOSED) throw new QueueClosedException();
            if(count.get()==0) return null;
            T e = dequeue();
            count.decrementAndGet();
            return e;
        } finally {
            takeLock.unlock();
        }
    }
    /**
     * returns the earliest element from the queue but does not remove it.
     * @return the element or null if the queue is empty.
     * @throws QueueClosedException if the queue is closed.
     */
    public T peek() {
        if (count.get() == 0)
            return null;
        takeLock.lock();
        try {
            return (count.get() > 0) ? head.next.element : null;
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * Drain all of the elements of the queue up to maxElements into the provided collection. If the queue is empty, the method returns immediately.
     * @param c is the non-null Collection to receive the elements.
     * @param maxElements is the maximum number of elements to drain.
     * @throws QueueClosedException if the queue is closed.
     */
    public int drainTo(Collection<? super T> c, int maxElements) {
        takeLock.lock();
        try {
            int n = 0;
            while(n<maxElements && count.get()>0) {
                if(head.next==CLOSED) break;
                c.add(dequeue());
                n++;
                count.decrementAndGet();
            }
            if(head==CLOSED && n==0) throw new QueueClosedException();
            return n;
        } finally {
            takeLock.unlock();
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
        takeLock.lock();
        try {
            int n=0;
            while(count.get()==0) notEmpty.await();
            while(count.get()>0) {
                if(head.next==CLOSED) break;
                c.add(dequeue());
                n++;
                count.decrementAndGet();
            }
            if(head==CLOSED && n==0) throw new QueueClosedException();
            return n;
        } finally {
            takeLock.unlock();
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
        takeLock.lock();
        try {
            while(count.get()==0) notEmpty.await();
            if(head.next==CLOSED) throw new QueueClosedException();
            T e = dequeue();
            count.getAndDecrement();
            return e;
        } finally {
            takeLock.unlock();
        }
    }

    private void signalNotEmpty() {
        takeLock.lock();
        try {
            notEmpty.signalAll();
        } finally {
            takeLock.unlock();
        }
    }
}
