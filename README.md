This is a unbounded FIFO queue that offers "close" semantics.

It is designed to be used with try-with-resources and ephemeral virtual threads to ensure proper clean-up of threads.

It only supports some of the Queue interface methods, so it is not a drop-in replacement, but most of ommitted methods are probably not useful in a high-volume VT environment.

TODO: use read/write lock to improve the concurrency between readers and writers when not using SingleConsumerQueue

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

The `take()` in consumer will throw an `QueueClosedException` (subclass of `IllegalStateException`) if the queue is closed and all elements from the queue have been processed (i.e. queue is empty and closed).

Multiple producers and consumers are supported. Once the queue is closed, any `put()` related methods will fail with a `QueueClosedException`.

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

## performance

There are jmh benchmarks that test against the standard concurrent queues:

```
Benchmark                                                    Mode  Cnt     Score     Error  Units
ClosableQueueBenchmark.testClosableQueue                     avgt    9   142.820 ±  11.575  ns/op
ClosableQueueBenchmark.testSingleConsumerQueue               avgt    9    71.104 ±   2.867  ns/op
ClosableQueueBenchmark.testLinkedBlockingQueue               avgt    9   168.452 ±  11.434  ns/op
ClosableQueueBenchmark.testLinkedTransferQueue               avgt    9    64.612 ±   0.924  ns/op
ClosableQueueBenchmark.testLinkedTransferQueueUsingTransfer  avgt    9  1687.000 ± 496.429  ns/op
```

## maven

```xml
<dependency>
  <groupId>io.github.robaho</groupId>
  <artifactId>closablequeue</artifactId>
  <version>1.0.8</version>
</dependency>
```
