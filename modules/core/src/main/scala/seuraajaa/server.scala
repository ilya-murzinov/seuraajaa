package seuraajaa

import java.net._

import monix.eval._
import monix.reactive.Observable
import seuraajaa.models._
import seuraajaa.parser._

import scala.concurrent.duration.Duration
import scala.io.BufferedSource

class Server private[Server](config: Config) {
  private[this] val handler = new Handler(socketNotifier)
  private[this] val eventSourceSocket = bind(config.eventSourcePort)
  private[this] val clientsSocket = bind(config.clientPort)

  val run: Task[Unit] = {
    val initialState = HandlerState.initial

    val acceptClient = Task.eval {
      val socket = clientsSocket.accept()
      val id = new BufferedSource(socket.getInputStream).getLines.next
      Some(id.toLong -> socket)
    }.onErrorHandle(_ => None)

    val acceptEventSource = Task.eval {
      val inputStream = eventSourceSocket.accept().getInputStream
      Some(new BufferedSource(inputStream).getLines)
    }.onErrorHandle(_ => None)

    def clientSubscriber(clients: MVar[Clients]) =
      Observable.fromIterator(Iterator.continually(()))
        .doOnSubscribe(() =>
          println(s"Client subscriber started, waiting for connections on ${config.clientPort}..."))
        .mapTask(_ => acceptClient)
        .filter(_.isDefined)
        .map(_.get)
        .mapTask { case (id, s) =>
          for {
            map <- clients.take
            _ <- clients.put(map + (id -> s))
          } yield ()
        }
        .completedL

    def eventSourceProcessor(clients: MVar[Clients]) =
      Observable.fromIterator(Iterator.continually(()))
        .doOnSubscribe(() =>
          println(s"Event source processor started, waiting for connections on ${config.eventSourcePort}..."))
        .mapTask(_ => acceptEventSource)
        .filter(_.isDefined)
        .map(_.get)
        .flatMap(it =>
          Observable.fromIterator(it)
            .map(parse)
            .filter(_.isDefined)
            .map(_.get)
            .scanTask(Task.pure(initialState))((state, event) => handler.handle(event, state, clients)))
        .completedL

    for {
      clients <- MVar(Map[ClientId, Socket]())
      c = clientSubscriber(clients).executeOn(clientScheduler)
      e = eventSourceProcessor(clients).executeOn(eventSourceScheduler)
      _ <- Task.gatherUnordered(Seq(c, e))
    } yield ()
  }

  val shutdown: Task[Unit] = Task.eval(List(clientsSocket, eventSourceSocket).foreach(_.close()))

  private[this] def bind(port: Int): ServerSocket = {
    val socket = new ServerSocket()
    socket.setReuseAddress(true)
    socket.bind(new InetSocketAddress(port))
    socket
  }
}

object Server {
  import monix.execution.Scheduler.Implicits.global

  def apply(config: Config): Task[Server] = Task.eval(new Server(config))

  def main(args: Array[String]): Unit = (for {
    server <- Server(Config())
    _ <- server.run
  } yield ()).runSyncUnsafe(Duration.Inf)
}