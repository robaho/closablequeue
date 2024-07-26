This is a simple unbounded FIFO queue that offers "close" semantics.

It is designed to be used with try-with-resources and ephemeral virtual threads to ensure proper clean-up of threads.

It only supports some of the Queue interface methods, so it is not a drop-in replacement, but most of ommitted methods are probably not useful in a high-volume VT environment.

TODO: use read/write lock to improve the concurrency between readers and writers

TODO: possibly implement the rest of the BlockingQueue interface methods to make it a drop-in replacement.

## usage

The code will most likely be structured similar to:

```java
try(var queue=new ClosableQueue<T>()) {
   Thread.startVirtualThread(newConsumer(queue));
  ... put() items into queue from source/generation ...
}
```

and the consumer:

```java
for(T e, e=queue.take();) {
  ... do something with e ...
}
```

or possibly more efficiently:

```java
ArrayList<T> elements = new ArrayList();
while(queue.drainToBlocking(elements)>0) {
  ... for each e in elements do ...
  elements.clear();
}
```

The `take()` in consumer will throw an `IllegalStateException` if the producer closes the queue and all elements from the queue have been processed.

See [ClosableQueue](lib/src/main/java/robaho/queue/ClosableQueue.java)

## single consumer queue

The library also includes a highly efficient closable queue specifically designed for the case of a single active reader - which is expected to be most of time when using ephemeral virtual thread queues.

```java
try(var queue=new SingleConsumerQueue<T>()) {
   Thread.startVirtualThread(newConsumer(queue));
  ... put() items into queue from source/generation ...
}
```

See [SingleConsumerQueue](lib/src/main/java/robaho/queue/SingleConsumerQueue.java)
