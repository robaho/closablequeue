package robaho.queue;

import org.junit.platform.commons.annotation.Testable;

@Testable
class SingleConsumerQueueTest extends AbstractClosableQueueTest{
    @Override
    protected AbstractClosableQueue<Integer> createQueue() {
        return new SingleConsumerQueue<>();
    }
}
