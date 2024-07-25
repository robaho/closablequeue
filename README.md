This is a simple unbounded FIFO queue that offers "close" semantics.

It is designed to be used with try-with-resources and ephemeral virtual threads to ensure proper clean-up of threads.

It only supports some of the Queue interface methods, so it is not a drop-in replacement, but most of ommitted methods are probably not useful in a high-volume VT environment.

TODO: use read/write lock to improve the concurrency between readers and writers
TODO: possible implement the rest of the BlockingQueue interface methods to make it a drop-in replacement.
