package robaho.queue;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * an unbounded FIFO queue with "close" semantics. For efficiency, it only supports a single reader. This queue is designed for virtual thread hand-off type scenarios.
 */
public class SingleConsumerQueue<T> extends AbstractClosableQueue<T> {
    private AtomicReference<Thread> waiter = new AtomicReference<>();
    // since the queue is unbounded a spin lock works
    private SpinLock lock = new SpinLock();

    private static class SpinLock {
        private final AtomicBoolean lock = new AtomicBoolean();
        public void lock() {
            while(!lock.compareAndSet(false,true)){}
        }
        public void unlock() {
            lock.set(false);
        }
    }

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
        LockSupport.unpark(waiter.get());
    }
    @Override
    protected void wakeupReader() {
        wakeupReaders();
    }

    /**
     * Drain all of the elements of the queue up to maxElements into the provided collection. If the queue is empty, the method returns immediately.
     * @param c is the non-null Collection to receive the elements.
     * @param maxElements is the maximum number of elements to drain.
     * @throws IllegalStateException if the queue is closed.
     */
    @Override
    public int drainToBlocking(Collection<? super T> c) throws InterruptedException {
        try {
            if(!waiter.compareAndSet(null,Thread.currentThread())) throw new IllegalStateException("queue has an active reader");
            try {
                while (true) { 
                    lock.lock();
                    if(list.isEmpty()) {
                        checkClosed();
                        lock.unlock();
                        LockSupport.park(lock);
                    } else {
                        int count=0;
                        for(T e;(e=list.poll())!=null;count++) {
                            c.add(e);
                        }
                        return count;
                    }
                }
            } finally {
                waiter.set(null);
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
    @Override
    public T take() throws InterruptedException {
        try {
            if(!waiter.compareAndSet(null,Thread.currentThread())) throw new IllegalStateException("queue has an active reader");
            try {
                while (true) { 
                    lock.lock();
                    T e = list.poll();
                    if(e!=null) return e;
                    checkClosed();
                    lock.unlock();
                    LockSupport.park(lock);
                }
            } finally {
                waiter.set(null);
            }
        } finally {
            lock.unlock();
        }
    }



}