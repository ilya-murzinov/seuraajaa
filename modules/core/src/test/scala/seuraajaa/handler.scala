package seuraajaa

import java.net.Socket

import common._
import monix.eval.{MVar, Task}
import org.scalamock.scalatest.MockFactory
import seuraajaa.gens._
import seuraajaa.models._

trait HandlerTestSupport { self: MockFactory with AsyncSupport =>
  protected val notifier: Notifier = mock[Notifier]
  protected val handler: Handler = new Handler(notifier)

  protected def fixture(f: (HandlerState, Clients, MVar[Clients]) => Unit): Unit = {
    val state = HandlerState.initial
    val clients = Map[ClientId, Socket]()
    val clientsM = MVar(clients).run

    f(state, clients, clientsM)
  }
}

class HandlerSpec extends Spec with HandlerTestSupport {

  property("handle should enqueue event if seq num doesn't fit") {
    fixture { (state, _, clientsM) =>
      forAll(genEvent) { e =>
        whenever(e.seqNum != state.nextSeqNum) {
          val newState = handler.handle(e, state, clientsM).run
          newState.queue.min shouldBe e
          newState.nextSeqNum shouldBe state.nextSeqNum
        }
      }
    }
  }

  property("handle should handle follow event") {
    fixture { (state, clients, clientsM) =>
      forAll(genFollowEvent) { e =>
        (notifier.notifyClient _).expects(e.payload, clients, e.to).returning(Task.unit)

        val newState = handler.handle(e, state.copy(nextSeqNum = e.seqNum), clientsM).run
        newState.queue.isEmpty shouldBe true
        newState.nextSeqNum shouldBe e.seqNum + 1
        newState.followers shouldBe Map(e.to -> Set(e.from))
      }
    }
  }

  property("handle should handle unfollow event") {
    fixture { (state, clients, clientsM) =>
      forAll(genUnfollowEvent) { e =>
        val preparedState = state.copy(
          nextSeqNum = e.seqNum,
          followers = Map(e.to -> Set(e.from)))

        val newState = handler.handle(e, preparedState, clientsM).run

        newState.queue.isEmpty shouldBe true
        newState.nextSeqNum shouldBe e.seqNum + 1
        newState.followers shouldBe Map()
      }
    }
  }

  property("handle should handle status event") {
    fixture { (state, clients, clientsM) =>
      forAll(genStatusUpdate) { e =>
        forAll { followers: Set[ClientId] =>
          (notifier.notifyClients _).expects(e.payload, clients, followers).returning(Task.unit)

          val preparedState = state.copy(
            nextSeqNum = e.seqNum,
            followers = Map(e.from -> followers))

          val newState = handler.handle(e, preparedState, clientsM).run

          newState shouldBe preparedState.copy(nextSeqNum = e.seqNum + 1)
        }
      }
    }
  }

  property("handle should handle broadcast event") {
    fixture { (state, _, clientsM) =>
      forAll(genBroadcastEvent) { e =>
        forAll { allClients: Set[ClientId] =>
          (notifier.notifyAllClients _).expects(e.payload, *).returning(Task.unit)

          val preparedState = state.copy(nextSeqNum = e.seqNum)

          val newState = handler.handle(e, preparedState, clientsM).run

          newState shouldBe preparedState.copy(nextSeqNum = e.seqNum + 1)
        }
      }
    }
  }

  property("handle all sequential events") {
    fixture { (state, _, clientsM) =>
      forAll(genSequentialEvents(1, 10)) { events =>
        val initialState = state.copy(nextSeqNum = 1)

        (notifier.notifyAllClients _).expects(*, *).anyNumberOfTimes.returning(Task.unit)
        (notifier.notifyClient _).expects(*, *, *).anyNumberOfTimes.returning(Task.unit)
        (notifier.notifyClients _).expects(*, *, *).anyNumberOfTimes.returning(Task.unit)

        val finalState = events.foldLeft(initialState) { (s, e) =>
          handler.handle(e, s, clientsM).run
        }

        finalState.queue.isEmpty shouldBe true
        finalState.nextSeqNum shouldBe initialState.nextSeqNum + events.size
      }
    }
  }
}

class HandlerTest extends Test with HandlerTestSupport {
  import scala.util.Random._

  "handler" should "handle sequence of events" in {
    fixture { (state, clients, clientsM) =>
      // emulate property-based test to some extent
      (1 to 100).foreach { _ =>
        val eventsSeq = shuffle(helper.eventsSeq)

        (notifier.notifyAllClients _).expects("broadcast 2", clients).returning(Task.unit)
        (notifier.notifyAllClients _).expects("broadcast 7", clients).returning(Task.unit)
        (notifier.notifyClients _).expects("status of 31", clients, Set(42L, 67L)).returning(Task.unit)
        (notifier.notifyClients _).expects("status of 31 2", clients, Set(67L)).returning(Task.unit)
        (notifier.notifyClients _).expects("status of 31 3", clients, Set.empty[Long]).returning(Task.unit)
        (notifier.notifyClient _).expects("follow", clients, 31L).returning(Task.unit).repeated(2)
        (notifier.notifyClient _).expects("private message from 23 to 72", clients, 72L).returning(Task.unit)

        val initialState = state.copy(nextSeqNum = 1)

        val finalState = eventsSeq.foldLeft(initialState) { (s, e) =>
          handler.handle(e, s, clientsM).run
        }

        finalState.queue.isEmpty shouldBe true
        finalState.nextSeqNum shouldBe initialState.nextSeqNum + eventsSeq.size
      }
    }
  }
}