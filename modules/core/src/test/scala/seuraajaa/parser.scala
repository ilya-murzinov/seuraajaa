package seuraajaa

import common._
import gens._
import org.scalacheck.Gen

class ParserSpec extends Spec {
  import parser._
  import models._

  property("should parse Follow event") {
    forAll(triple) { case (seq, from, to) =>
      val raw = toRaw("F", seq, from, to)
      parse(raw) shouldBe Some(Follow(seq, raw, from, to))
    }
  }

  property("should parse Unfollow event") {
    forAll(triple) { case (seq, from, to) =>
      val raw = toRaw("U", seq, from, to)
      parse(raw) shouldBe Some(Unfollow(seq, raw, from, to))
    }
  }

  property("should parse Broadcast event") {
    forAll(Gen.posNum[Long]) { seq =>
      val raw = toRaw("B", seq)
      parse(raw) shouldBe Some(Broadcast(seq, raw))
    }
  }

  property("should parse PrivateMsg event") {
    forAll(triple) { case (seq, from, to) =>
      val raw = toRaw("P", seq, from, to)
      parse(raw) shouldBe Some(PrivateMsg(seq, raw, from, to))
    }
  }

  property("should parse StatusUpdate event") {
    forAll(tuple) { case (seq, from) =>
      val raw = toRaw("S", seq, from)
      parse(raw) shouldBe Some(StatusUpdate(seq, raw, from))
    }
  }

  property("should not parse incorrect string") {
    forAll { s: String => // no way it has the correct format lol :D
      parse(s) shouldBe None
    }
  }

  private[this] def toRaw(tpe: String, parts: Long*) = {
    val l = parts.toList
    (l.head +: tpe +: l.tail).mkString("|")
  }
}