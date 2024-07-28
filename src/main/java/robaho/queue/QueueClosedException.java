package robaho.queue;

public class QueueClosedException extends IllegalStateException {
    QueueClosedException() {
        super("queue is closed");
    }
}
