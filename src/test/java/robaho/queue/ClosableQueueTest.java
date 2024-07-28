package robaho.queue;

import org.junit.platform.commons.annotation.Testable;

@Testable
class ClosableQueueTest extends AbstractClosableQueueTest{
    @Override
    protected AbstractClosableQueue<Integer> createQueue() {
        return new ClosableQueue<>();
    }
}
