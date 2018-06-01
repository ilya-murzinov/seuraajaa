package seuraajaa.internal

import common._

class HeapSpec extends Spec {
  property("heap should maintain put/get invariant") {
    forAll { i: Int =>
      val inserted = Heap.empty[Int].insert(i)
      inserted.min shouldBe i
      inserted.remove.isEmpty shouldBe true
    }
  }

  property("heap should sort a list") {
    forAll { l: List[Int] =>
      var heap = l.foldLeft(Heap.empty[Int])((h, i) => h.insert(i))
      var fromHeap = List[Int]()

      while (!heap.isEmpty) {
        fromHeap = heap.min +: fromHeap
        heap = heap.remove
      }

      fromHeap shouldBe l.sorted
      heap.isEmpty shouldBe true
    }
  }
}