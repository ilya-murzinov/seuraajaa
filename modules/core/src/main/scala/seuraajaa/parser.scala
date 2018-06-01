package seuraajaa

import fastparse.all._
import models._

object parser {
  val number: P[Long] = P(CharIn('0' to '9').rep(1).!.map(_.toLong))

  val parser: Parser[(Long, Option[String], Option[Long], Option[Long])] =
    P(Start ~ number ~ "|" ~ AnyChar.!.? ~ "|".? ~ number.? ~ "|".? ~ number.? ~ End)

  def parse(raw: String): Option[Event] = parser.parse(raw) match {
    case Parsed.Success((seqNum, Some("F"), Some(from), Some(to)), _) => Some(Follow(seqNum, raw, from, to))
    case Parsed.Success((seqNum, Some("U"), Some(from), Some(to)), _) => Some(Unfollow(seqNum, raw, from, to))
    case Parsed.Success((seqNum, Some("B"), None, None), _) => Some(Broadcast(seqNum, raw))
    case Parsed.Success((seqNum, Some("P"), Some(from), Some(to)), _) => Some(PrivateMsg(seqNum, raw, from, to))
    case Parsed.Success((seqNum, Some("S"), Some(from), None), _) => Some(StatusUpdate(seqNum, raw, from))
    case _ => None
  }
}