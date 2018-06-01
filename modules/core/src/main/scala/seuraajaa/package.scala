import java.net.Socket

import seuraajaa.models._
import monix.execution.Scheduler

package object seuraajaa {
  case class Config(
    eventSourcePort: Int = 9090,
    clientPort: Int = 9099)

  type Followers = Map[ClientId, Set[ClientId]]
  type Clients = Map[ClientId, Socket]

  val eventSourceScheduler: Scheduler = Scheduler.io(name = "event-source-scheduler")
  val clientScheduler: Scheduler = Scheduler.io(name = "client-scheduler")
}
