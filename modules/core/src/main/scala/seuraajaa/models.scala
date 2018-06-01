package seuraajaa

object models {
  type ClientId = Long
  type SeqNum = Long

  sealed trait Event {
    val seqNum: SeqNum
    val payload: String
  }

  object Event {
    implicit val ord: Ordering[Event] = Ordering.by(- _.seqNum)
  }

  case class Follow(
    seqNum: SeqNum,
    payload: String,
    from: ClientId,
    to: ClientId) extends Event

  case class Unfollow(
    seqNum: SeqNum,
    payload: String,
    from: ClientId,
    to: ClientId) extends Event

  case class Broadcast(
    seqNum: SeqNum,
    payload: String) extends Event

  case class PrivateMsg(
    seqNum: SeqNum,
    payload: String,
    from: ClientId,
    to: ClientId) extends Event

  case class StatusUpdate(
    seqNum: SeqNum,
    payload: String,
    from: ClientId) extends Event
}