package seuraajaa

import common._
import seuraajaa.models._
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.duration.DurationInt
import scala.util.Random
import scala.util.control.NonFatal

class FunctionalTest extends Test {
  import monix.execution.Scheduler.Implicits.global

  val serverScheduler: Scheduler = Scheduler.io(name = "test-server")
  val clientScheduler: Scheduler = Scheduler.io(name = "test-client")

  "server" should "work" in {
    val config = Config()
    val server = Server(config)

    def client(id: ClientId) = id -> new Client(id, config.clientPort)
    val clients = helper.expectedEvents.keys.map(client).toMap

    val eventsSeq = Random.shuffle(helper.eventsSeq)

    val flow = for {
      eventSource <- Task.eval(new EventSource(config.eventSourcePort))
      future = server.run.runAsync(serverScheduler)
      clientTask = for {
        _ <- Task.eval(clients.values.foreach(_.register()))
        _ <- Task.eval(eventsSeq.foreach(eventSource.emit))
        _ <- Task.eval(eventSource.flush())
        _ <- Task.gatherUnordered(clients.map { case (id, c) =>
          Task.eval {
            val expectedEvents = helper.expectedEvents(id)
            val events = c.events(expectedEvents.size)
            events.map(_.seqNum) shouldBe expectedEvents.toList
          }
        })
      } yield ()
      cancel = Task.eval {
        try {
          future.cancel()
          server.shutdown()
          clients.values.foreach(_.shutdown())
        } catch {
          case NonFatal(e) => println(s"Can't shutdown! ${e.getMessage}")
        }
      }
      _ <- clientTask.onErrorFallbackTo(for {
        _ <- cancel
        _ = fail()
      } yield ())
      _ <- cancel
    } yield ()

    flow.onErrorHandle(e => fail(e)).runSyncUnsafe(1.second)
  }
}
