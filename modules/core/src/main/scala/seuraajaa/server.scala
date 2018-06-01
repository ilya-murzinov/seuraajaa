package seuraajaa

import java.net._

import seuraajaa.models._
import seuraajaa.parser._
import monix.eval._
import monix.reactive.Observable

import scala.concurrent.duration.Duration
import scala.io.BufferedSource
import scala.util.control.NonFatal

class Server(config: Config) {
  private[this] val handler = new Handler(socketNotifier)
  private[this] val eventSourceSocket = bind(config.eventSourcePort)
  private[this] val clientsSocket = bind(config.clientPort)

  val run: Task[Unit] = {
    val initialState = HandlerState.initial

    val acceptClient = Task {
      try {
        val socket = clientsSocket.accept()
        val id = new BufferedSource(socket.getInputStream).getLines.next
        Some(id.toLong -> socket)
      } catch {
        case NonFatal(e) =>
          println(s"Error occurred when connecting client $e")
          None
      }
    }

    val acceptEventSource = Task {
      try {
        val inputStream = eventSourceSocket.accept().getInputStream
        Some(new BufferedSource(inputStream).getLines)
      } catch {
        case NonFatal(e) =>
          println(s"Error occurred when connecting event source $e")
          None
      }
    }

    def clientSubscriber(clients: MVar[Clients]) =
      Observable.fromIterator(Iterator.continually(()))
        .doOnSubscribe(() =>
          println(s"Client subscriber started, waiting for connections on ${config.clientPort}..."))
        .mapTask(_ => acceptClient)
        .filter(_.isDefined)
        .map(_.get)
        .mapTask {
          case (id, s) => for {
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
            .flatScan(initialState) { case (state, event) =>
              Observable.fromTask(for {
                map <- clients.take
                state <- handler.handle(event, state, map)
                _ <- clients.put(map)
              } yield state)
            })
        .completedL

    for {
      clients <- MVar(Map[ClientId, Socket]())
      c = clientSubscriber(clients).executeOn(clientScheduler)
      e = eventSourceProcessor(clients).executeOn(eventSourceScheduler)
      _ <- Task.gatherUnordered(Seq(c, e))
    } yield ()
  }

  def shutdown(): Unit = List(clientsSocket, eventSourceSocket).foreach(_.close())

  private[this] def bind(port: Int) = {
    val socket = new ServerSocket()
    socket.setReuseAddress(true)
    socket.bind(new InetSocketAddress(port))
    socket
  }
}

object Server {
  import monix.execution.Scheduler.Implicits.global

  def apply(config: Config) = new Server(config)

  def main(args: Array[String]): Unit =
    Server(Config()).run.runSyncUnsafe(Duration.Inf)
}