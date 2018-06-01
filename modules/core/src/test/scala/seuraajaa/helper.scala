package seuraajaa

import seuraajaa.models._

object helper {
  val eventsSeq = Seq(
    Follow(1, "follow", 42, 31),
    Broadcast(2, "broadcast 2"),
    Follow(3, "follow", 67, 31),
    StatusUpdate(4, "status of 31", 31),
    PrivateMsg(5, "private message from 23 to 72", 23, 72),
    Unfollow(6, "unfollow", 42, 31),
    Broadcast(7, "broadcast 7"),
    StatusUpdate(8, "status of 31 2", 31),
    Unfollow(9, "unfollow", 67, 31),
    StatusUpdate(10, "status of 31 3", 31)
  )

  val expectedEvents: Map[ClientId, Set[SeqNum]] =
    Map(
      31L -> Set(1, 2, 3, 7),
      42L -> Set(2, 4, 7),
      67L -> Set(2, 4, 7, 8),
      72L -> Set(2, 5, 7),
      50L -> Set(2, 7)
    )
}