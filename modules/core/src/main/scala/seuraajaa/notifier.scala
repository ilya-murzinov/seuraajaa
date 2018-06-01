package seuraajaa

import java.io.OutputStream
import java.net.Socket

import seuraajaa.models._
import monix.eval.Task

import scala.util.control.NonFatal

trait Notifier {
  def notifyAllClients(payload: String, clients: Clients): Task[Unit]
  def notifyClients(payload: String, clients: Clients, ids: Set[ClientId]): Task[Unit]
  def notifyClient(payload: String, clients: Clients, id: ClientId): Task[Unit]
}

object socketNotifier extends Notifier {
  def notifyClients(payload: String, clients: Clients, ids: Set[ClientId]): Task[Unit] =
    Task.eval(ids.foreach(id => clients.get(id).fold(())(write(payload, _))))

  def notifyClient(payload: String, clients: Clients, id: ClientId): Task[Unit] =
    notifyClients(payload, clients, Set(id))

  def notifyAllClients(payload: String, clients: Clients): Task[Unit] =
    Task.eval(clients.values.foreach(write(payload, _)))

  private[this] def write(event: String, s: Socket): Unit =
    writer.write(event, s.getOutputStream)
}

object writer {
  def write(event: String, os: OutputStream): Unit = try {
    os.write(s"$event\n".getBytes)
    os.flush()
  } catch { case NonFatal(e) =>
    println(s"Error writing event '$event': ${e.getMessage}")
  }
}