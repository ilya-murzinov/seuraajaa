package seuraajaa

import models._
import internal._
import monix.eval.Task

import scala.annotation.tailrec

case class HandlerState(
  followers: Followers,
  nextSeqNum: SeqNum,
  queue: Heap[Event])

object HandlerState {
  val initial: HandlerState = HandlerState(Map(), 1, Heap.empty)
}

class Handler(notifier: Notifier) {
  def handle(event: Event,
             pState: HandlerState,
             clients: Clients): Task[HandlerState] = {
    @tailrec
    def sequentialEvents(event: Event,
                         queue: Heap[Event],
                         seqNum: SeqNum,
                         acc: Seq[Event]): (Seq[Event], Heap[Event], SeqNum) = {
      if (event.seqNum == seqNum && !queue.isEmpty)
        sequentialEvents(queue.min, queue.remove, seqNum + 1, acc :+ event)
      else if (event.seqNum == seqNum)
        (acc :+ event, queue, seqNum + 1)
      else
        (acc, queue.insert(event), seqNum)
    }

    val (events, queue, seqNum) = sequentialEvents(event, pState.queue, pState.nextSeqNum, Seq())

    val processSequentialEvents: Task[Followers] = events.foldLeft(Task.pure(pState.followers)) { (s, e) =>
      s.flatMap(followers => processEvent(e, followers, clients))
    }

    processSequentialEvents.map(fs => pState.copy(followers = fs, queue = queue, nextSeqNum = seqNum))
  }

  private[this] def processEvent(event: Event,
                                 followers: Followers,
                                 clients: Clients): Task[Followers] =
    event match {
      case Follow(_, payload, from, to) =>
        notifier.notifyClient(payload, clients, to).map(_ => addFollower(followers, from, to))
      case Unfollow(_, _, from, to) =>
        Task.pure(removeFollower(followers, from, to))
      case Broadcast(_, payload) =>
        notifier.notifyAllClients(payload, clients).map(_ => followers)
      case PrivateMsg(_, payload, _, to) =>
        notifier.notifyClient(payload, clients, to).map(_ => followers)
      case StatusUpdate(_, payload, from) =>
        notifier.notifyClients(payload, clients, followers.getOrElse(from, Set.empty)).map(_ => followers)
    }

  private[this] def addFollower(followers: Followers, from: ClientId, to: ClientId): Followers =
    followers + (to -> (followers.getOrElse(to, Set()) + from))

  private[this] def removeFollower(followers: Followers, from: ClientId, to: ClientId): Followers =
    followers.get(to).fold(followers)(s => s - from match {
      case updated if updated.isEmpty => followers - to
      case updated => followers + (to -> updated)
    })
}