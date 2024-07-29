package robaho.queue;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.concurrent.locks.LockSupport;

/**
 * an unbounded FIFO queue with "close" semantics. For efficiency, it only supports a single reader. This queue is designed for virtual thread hand-off type scenarios.
 */
public class SingleConsumerQueue<T> extends AbstractClosableQueue<T>{
    private volatile Thread waiter = null;
    private static class Node<T> {
        T element;
        volatile Node next;
        Node(T element) {
            this.element=element;
        }
    }
    private volatile Node<T> tail = new Node(null);
    private Node<T> head = tail;

    private static final int SPIN_WAITS       = 1 <<  7;   // max calls to onSpinWait
    private static final Node CLOSED = new Node(null);

    @Override
    public void put(T e) {
        Node node = new Node(e);
        while(true) {
            Node _tail = tail;
            if(_tail==CLOSED) throw new QueueClosedException();
            if(TAIL.compareAndSet(this,_tail,node)) {
                _tail.next=node;
                LockSupport.unpark(waiter);
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
        if(!WAITER.compareAndSet(this,null,Thread.currentThread())) throw new IllegalStateException("queue has an active reader");
        try {
            for (;;) {
                if(head==CLOSED) throw new QueueClosedException();
                if(head.element!=null) {
                    T element = head.element;
                    head.element = null;
                    if(head.next!=null) head = head.next;
                    return element;
                } else if(head.next!=null) {
                    head = head.next;
                } else {
                    return null;
                }
            }
        } finally {
            waiter=null;
        }
    }

    /**
     * returns the earliest element from the queue but does not remove it.
     * @return the element or null if the queue is empty.
     * @throws QueueClosedException if the queue is closed.
     */
    @Override
    public T peek() {
        if(!WAITER.compareAndSet(this,null,Thread.currentThread())) throw new IllegalStateException("queue has an active reader");
        try {
            var _head = head;
            for (;;) {
                if(_head==CLOSED) throw new QueueClosedException();
                if(_head.element!=null) return head.element;
                if(_head.next!=null) {
                    _head = _head.next;
                } else {
                    return null;
                }
            }
        } finally {
            waiter=null;
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
            Node _tail = tail;
            if(_tail==CLOSED) return;
            if(TAIL.compareAndSet(this,_tail,CLOSED)) {
                _tail.next=CLOSED;
                LockSupport.unpark(waiter);
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
        if(!WAITER.compareAndSet(this,null,Thread.currentThread())) throw new IllegalStateException("queue has an active reader");
        try {
            int waits=0;
            while (true) {
                if(head==CLOSED) throw new QueueClosedException();
                if(head.element!=null) {
                    T element = head.element;
                    head.element = null;
                    if(head.next!=null) head = head.next;
                    return element;
                } else {
                    if(head.next!=null) head = head.next;
                }
                if(++waits<SPIN_WAITS) {
                    Thread.onSpinWait();
                    continue;
                }
                waits=0;
                LockSupport.park(this);
            }
        } finally {
            waiter=null;
        }
    }

    // VarHandle mechanics
    private static final VarHandle TAIL;
    private static final VarHandle WAITER;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            TAIL = l.findVarHandle(SingleConsumerQueue.class, "tail",Node.class);
            WAITER = l.findVarHandle(SingleConsumerQueue.class, "waiter", Thread.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

}