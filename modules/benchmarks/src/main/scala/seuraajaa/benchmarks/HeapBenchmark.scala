package seuraajaa.benchmarks

import java.util.concurrent.TimeUnit

import seuraajaa.internal.Heap
import org.openjdk.jmh.annotations._

import scala.collection.mutable

/**
  * benchmarks/jmh:run -i 5 -wi 5 -f 2 -t 1 seuraajaa.benchmarks.HeapBenchmark
  */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class HeapBenchmark {
  private[this] val list = (1 to 1000).reverse

  @Benchmark
  def heap: Heap[Int] = {
    val full = list.foldLeft(Heap.empty[Int]) { (h, e) => h.insert(e) }

    list.foldLeft(full)((h, _) => h.remove)
  }

  @Benchmark
  def priorityQueue: mutable.PriorityQueue[Int] = {
    val queue = new mutable.PriorityQueue[Int]()
    list.foreach(e => queue += e)
    list.foreach(_ => queue.dequeue())
    queue
  }
}