package seuraajaa

import java.net.Socket

import seuraajaa.models._

import scala.concurrent.duration.DurationInt
import scala.io.BufferedSource

class Client(id: ClientId, port: Int) {
  lazy val socket: Socket = {
    val s = new Socket("localhost", port)
    s.setSoTimeout(1.seconds.toMillis.toInt)
    s
  }
  private[this] lazy val source = new BufferedSource(socket.getInputStream)

  def register(): Unit = {
    val stream = socket.getOutputStream
    writer.write(id.toString, stream)
  }

  // hacky but works
  def events(n: Int): Seq[Event] = {
    val lines = source.getLines
    (1 to n)
      .map(_ => lines.next())
      .map(parser.parse)
      .filter(_.isDefined)
      .map(_.get)
  }

  def shutdown(): Unit = {
    socket.shutdownInput()
    socket.shutdownOutput()
    socket.close()
  }
}

class EventSource(port: Int) {
  private[this] val stream = new Socket("localhost", port).getOutputStream

  def emit(event: Event): Unit = writer.write(event match {
    case Follow(seqNum, _, from, to) => s"$seqNum|F|$from|$to"
    case Unfollow(seqNum, _, from, to) => s"$seqNum|U|$from|$to"
    case Broadcast(seqNum, _) => s"$seqNum|B"
    case PrivateMsg(seqNum, _, from, to) => s"$seqNum|P|$from|$to"
    case StatusUpdate(seqNum, _, from) => s"$seqNum|S|$from"
  }, stream)

  // last message don't get received without this :(
  def flush(): Unit = stream.write('\n')
}