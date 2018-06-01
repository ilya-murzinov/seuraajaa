# seuraajaa

Simple server for event stream processing built with [Monix](https://monix.io/).

Fox technical requirements see [instructions](instructions/instructions.md)

## Startup and tests

Just execute `sbt run`. Configuration (event source/clients ports) can be changed in `package object seuraajaa`.

If you want, you can also package a fat jar using `sbt assembly`.

There are 2 sets of tests: unit and functional.

To run unit tests simply execute `sbt test`, to run functional tests execute `functional-tests/test`.
Functional tests (actually there is only one) are in separate module because they are not polished enough and sometimes
fail when running multiple times in a row.

## Design decisions

Solution uses only 2 runtime dependencies: [Fastparse](https://github.com/lihaoyi/fastparse) and [Monix](https://monix.io/).

*Fastparse* has pretty similar functionality to the built-in `scala-parser-combinators`,
but has much better API and also is much faster. It is used for deserializing events.

*Monix* is a library for asynchronous, reactive and event-based programming.
It provides extremely useful abstractions like `Task`, `MVar` and `Observable` for composing asynchronous
computations with some shared state in a purely functional way.

It worth mentioning that custom implementation of immutable pairing heap is used instead of built-in mutable `PriorityQueue`.
`Heap` is not only much more convenient to work with, but is also much faster.
See `Heap` class for implementation and `HeapBenchmark` for benchmark against `PriorityQueue`.

Results of `benchmarks/jmh:run -i 5 -wi 5 -f 2 -t 1 seuraajaa.benchmarks.HeapBenchmark`:

```
[info] Benchmark                     Mode  Cnt      Score     Error  Units
[info] HeapBenchmark.heap           thrpt   10  13617.391 ± 491.485  ops/s
[info] HeapBenchmark.priorityQueue  thrpt   10   9167.334 ± 293.789  ops/s
```

For the sake of simplicity number of production-grade solutions deliberately skipped,
such as logging and configuration libraries.

## Overall performance

For the default configuration results are following:

```
[INFO] Sent: Broadcast -> 998, Follow -> 1498865, Private -> 1499442, StatusUpdate -> 5502175, Unfollow -> 1498520, total -> 10000000
[INFO] ...events sent, event source connection closed
[INFO] ========================================================
[INFO] waiting for remaining clients notifications...
[INFO] ========================================================
[INFO] \o/ ALL NOTIFICATIONS RECEIVED \o/ (in 255 seconds)
[INFO] ========================================================

real	4m15.699s
user	4m42.202s
sys	0m43.760s
```
