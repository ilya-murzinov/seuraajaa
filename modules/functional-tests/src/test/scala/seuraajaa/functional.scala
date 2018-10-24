package seuraajaa

import common._
import monix.eval.Task
import monix.execution.Scheduler
import seuraajaa.models._

import scala.concurrent.duration.DurationInt
import scala.util.Random

class FunctionalTest extends Test {
  import monix.execution.Scheduler.Implicits.global

  val serverScheduler: Scheduler = Scheduler.io(name = "test-server")
  val clientScheduler: Scheduler = Scheduler.io(name = "test-client")

  "server" should "work" in {
    val config = Config()

    def client(id: ClientId) = id -> new Client(id, config.clientPort)
    val clients = helper.expectedEvents.keys.map(client).toMap

    val eventsSeq = Random.shuffle(helper.eventsSeq)

    val flow = for {
      server <- Server(config)
      eventSource <- EventSource(config.eventSourcePort)
      future = server.run.runAsync(serverScheduler)
      clientTask = for {
        _ <- Task.gatherUnordered(clients.values.map(_.register))
        _ <- Task.gatherUnordered(eventsSeq.map(eventSource.emit))
        _ <- eventSource.flush
        _ <- Task.gatherUnordered(clients.map { case (id, c) =>
          val expectedEvents = helper.expectedEvents(id)
          c.events(expectedEvents.size).map(_.map(_.seqNum) shouldBe expectedEvents.toList)
        })
      } yield ()
      cancel = for {
//        _ <- server.shutdown
        _ <- Task.eval(future.cancel())
        _ <- Task.gatherUnordered(clients.values.map(_.shutdown))
      } yield ()
      _ <- clientTask.onErrorFallbackTo(for {
        _ <- cancel
        _ = fail()
      } yield ())
      _ <- cancel
    } yield ()

    flow.onErrorHandle(e => fail(e)).runSyncUnsafe(1.second)
  }
}
