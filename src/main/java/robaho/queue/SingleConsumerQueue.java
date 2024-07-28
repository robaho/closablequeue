package robaho.queue;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * an unbounded FIFO queue with "close" semantics. For efficiency, it only supports a single reader. This queue is designed for virtual thread hand-off type scenarios.
 */
public class SingleConsumerQueue<T> extends AbstractClosableQueue<T>{
    private final AtomicReference<Thread> waiter = new AtomicReference<>();
    private static class Node<T> {
        final T element;
        volatile Node next;
        Node(T element) {
            this.element=element;
        }
    }
    private final AtomicReference<Node<T>> tail = new AtomicReference();
    private final AtomicReference<Node<T>> head = new AtomicReference();

    private static final int SPIN_WAITS       = 1 <<  7;   // max calls to onSpinWait
    private static final Node CLOSED = new Node(null);

    @Override
    public void put(T e) {
        Node node = new Node(e);
        while(true) {
            Node _tail = tail.get();
            if(_tail==CLOSED) throw new QueueClosedException();
            if(tail.compareAndSet(_tail,node)) {
                if(_tail!=null) {
                    _tail.next=node;
                }
                head.compareAndSet(null,node);
                LockSupport.unpark(waiter.get());
                return;
            } else {
                Thread.onSpinWait();
            }
        }
    }

    @Override
    public void putAll(Collection<? extends T> c) {
        for(var e : c) {
            put(e);
        }
    }

    @Override
    public T poll() {
        if(!waiter.compareAndSet(null,Thread.currentThread())) throw new IllegalStateException("queue has an active reader");
        try {
            var _head = head.get();
            if(_head==CLOSED) throw new QueueClosedException();
            if(_head!=null) {
                head.set(_head.next);
                return _head.element;
            } else {
                return null;
            }
        } finally {
            waiter.set(null);
        }
    }

    /**
     * returns the earliest element from the queue but does not remove it.
     * @return the element or null if the queue is empty.
     * @throws QueueClosedException if the queue is closed.
     */
    @Override
    public T peek() {
        if(!waiter.compareAndSet(null,Thread.currentThread())) throw new IllegalStateException("queue has an active reader");
        try {
            var _head = head.get();
            if(_head==CLOSED) throw new QueueClosedException();
            if(_head!=null) {
                return _head.element;
            } else {
                return null;
            }
        } finally {
            waiter.set(null);
        }
    }
    /**
     * Drain all of the elements of the queue up to maxElements into the provided collection. If the queue is empty, the method returns immediately.
     * @param c is the non-null Collection to receive the elements.
     * @param maxElements is the maximum number of elements to drain.
     * @throws QueueClosedException if the queue is closed.
     */
    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        T element;
        int count=0;
        while((element=poll())!=null && count<maxElements) {
            c.add(element);
            count++;
        }
        return count;
    }

    @Override
    public void close() {
        while(true) {
            Node _tail = tail.get();
            if(_tail==CLOSED) return;
            if(tail.compareAndSet(_tail,CLOSED)) {
                if(_tail!=null){
                    _tail.next=CLOSED;
                }
                head.compareAndSet(null,CLOSED);
                LockSupport.unpark(waiter.get());
                return;
            }
        }
    }
    @Override
    public int drainToBlocking(Collection<? super T> c) throws InterruptedException {
        var e = take();
        c.add(e);
        int count=1;
        while((e=poll())!=null) {
            c.add(e);
            count++;
        }
        return count;
    }
    @Override
    public T take() throws InterruptedException {
        if(!waiter.compareAndSet(null,Thread.currentThread())) throw new IllegalStateException("queue has an active reader");
        try {
            int waits=0;
            while (true) { 
                var _head = head.get();
                if(_head==CLOSED) throw new QueueClosedException();
                if(_head!=null) {
                    head.set(_head.next);
                    return _head.element;
                }
                if(++waits<SPIN_WAITS) {
                    Thread.onSpinWait();
                    continue;
                }
                waits=0;
                LockSupport.park(this);
            }
        } finally {
            waiter.set(null);
        }
    }
}