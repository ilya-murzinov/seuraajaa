package seuraajaa

import java.io.{InputStreamReader, LineNumberReader}
import java.net.Socket

import monix.eval.Task
import monix.reactive.Observable
import seuraajaa.models._

import scala.concurrent.duration.DurationInt

class Client(id: ClientId, port: Int) {
  lazy val socket: Socket = {
    val s = new Socket("localhost", port)
    s.setSoTimeout(1.seconds.toMillis.toInt)
    s
  }

  private[this] lazy val reader =
    new LineNumberReader(new InputStreamReader(socket.getInputStream))

  val register: Task[Unit] = Task.eval {
    val stream = socket.getOutputStream
    writer.write(id.toString, stream)
  }

  def events(n: Int): Task[Seq[Event]] =
    Observable
      .fromLinesReader(reader)
      .take(n)
      .map(parser.parse)
      .filter(_.isDefined)
      .map(_.get)
      .toListL

  val shutdown: Task[Unit] = Task.eval {
    socket.shutdownInput()
    socket.shutdownOutput()
    socket.close()
  }.onErrorHandle(println)
}

class EventSource(port: Int) {
  private[this] val stream = new Socket("localhost", port).getOutputStream

  def emit(event: Event): Task[Unit] = Task.eval(writer.write(event match {
    case Follow(seqNum, _, from, to) => s"$seqNum|F|$from|$to"
    case Unfollow(seqNum, _, from, to) => s"$seqNum|U|$from|$to"
    case Broadcast(seqNum, _) => s"$seqNum|B"
    case PrivateMsg(seqNum, _, from, to) => s"$seqNum|P|$from|$to"
    case StatusUpdate(seqNum, _, from) => s"$seqNum|S|$from"
  }, stream))

  // last message don't get received without this :(
  val flush: Task[Unit] = Task.eval(stream.write('\n'))
}

object EventSource {
  def apply(port: Int): Task[EventSource] = Task.eval(new EventSource(port))
}