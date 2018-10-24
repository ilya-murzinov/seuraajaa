package seuraajaa.benchmarks

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import cats.instances.int._
import monix.reactive.{Observable, OverflowStrategy}
import org.openjdk.jmh.annotations._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/**
  * benchmarks/jmh:run -i 5 -wi 5 -f 2 -t 1 seuraajaa.benchmarks.MonixBenchmark
  */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class MonixBenchmark {
  import monix.execution.Scheduler.Implicits.global
  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  private[this] val list = 1 to 100

  @Benchmark
  def monixMerge: Int = {
    Observable.fromIterable(list)
      .map(_ => Observable.fromIterable(list))
      .merge
      .asyncBoundary(OverflowStrategy.BackPressure(10))
      .foldL
      .runSyncUnsafe(1.seconds)
  }

  @Benchmark
  def akkaMerge: Int = {
    val source: Source[Int, NotUsed] = Source(list)
    val f = list
      .map(_ => source)
      .fold(Source.empty)(_.merge(_))
      .runWith(Sink.fold(0)(_ + _))

    Await.result(f, 1.second)
  }

  @TearDown
  def shutdown(): Unit = {
    Await.result(system.terminate(), 5.seconds)
  }
}