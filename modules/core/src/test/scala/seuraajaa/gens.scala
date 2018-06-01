package seuraajaa

import models._
import org.scalacheck.Gen

object gens {
  val triple: Gen[(Long, Long, Long)] = for {
    t1 <- Gen.posNum[Long]
    t2 <- Gen.posNum[Long]
    t3 <- Gen.posNum[Long]
  } yield (t1, t2, t3)

  val tuple: Gen[(Long, Long)] = for {
    t1 <- Gen.posNum[Long]
    t2 <- Gen.posNum[Long]
  } yield (t1, t2)

  val genFollowEvent: Gen[Follow] = for {
    seqNum <- Gen.posNum[Long]
    payload <- Gen.alphaStr
    from <- Gen.posNum[Long]
    to <- Gen.posNum[Long]
  } yield Follow(seqNum, payload, from, to)

  val genUnfollowEvent: Gen[Unfollow] = for {
    seqNum <- Gen.posNum[Long]
    payload <- Gen.alphaStr
    from <- Gen.posNum[Long]
    to <- Gen.posNum[Long]
  } yield Unfollow(seqNum, payload, from, to)

  val genPrivateMsq: Gen[PrivateMsg] = for {
    seqNum <- Gen.posNum[Long]
    payload <- Gen.alphaStr
    from <- Gen.posNum[Long]
    to <- Gen.posNum[Long]
  } yield PrivateMsg(seqNum, payload, from, to)

  val genBroadcastEvent: Gen[Broadcast] = for {
    seqNum <- Gen.posNum[Long]
    payload <- Gen.alphaStr
  } yield Broadcast(seqNum, payload)

  val genStatusUpdate: Gen[StatusUpdate] = for {
    seqNum <- Gen.posNum[Long]
    payload <- Gen.alphaStr
    from <- Gen.posNum[Long]
  } yield StatusUpdate(seqNum, payload, from)

  val genEvent: Gen[Event] =
    Gen.oneOf(
      genFollowEvent,
      genUnfollowEvent,
      genPrivateMsq,
      genBroadcastEvent,
      genStatusUpdate)

  def genSequentialEvents(start: SeqNum, number: Int): Gen[Seq[Event]] =
    for {
      events <- Gen.listOfN(number, genEvent)
    } yield events.zip((start to (start + number - 1)).reverse).map {
      case (Follow(_, p, f, t), i) => Follow(i, p, f, t)
      case (Unfollow(_, p, f, t), i) => Unfollow(i, p, f, t)
      case (PrivateMsg(_, p, f, t), i) => PrivateMsg(i, p, f, t)
      case (Broadcast(_, p), i) => Broadcast(i, p)
      case (StatusUpdate(s, p, f), i) => StatusUpdate(i, p, f)
    }
}