package seuraajaa.internal

abstract sealed class Heap[A]() {

  def min: A
  def isEmpty: Boolean
  def remove(implicit ord: Ordering[A]): Heap[A]

  def insert(x: A)(implicit ord: Ordering[A]): Heap[A] = merge(make(x), this)

  protected def make(x: A, subs: List[Heap[A]] = List[Heap[A]]()) = Branch(x, subs)

  protected def merge(x: Heap[A], y: Heap[A])(implicit ord: Ordering[A]): Heap[A] = (x, y) match {
    case (_, Leaf()) => x
    case (Leaf(), _) => y
    case (Branch(x1, subs1), Branch(x2, subs2)) =>
      if (ord.compare(x1, x2) > 0) Branch(x1, Branch(x2, subs2) :: subs1)
      else Branch[A](x2, Branch(x2, Branch(x1, subs1) :: subs2).subs)
  }

  protected def pairing(subs: List[Heap[A]])(implicit a: Ordering[A]): Heap[A] = subs match {
    case Nil => Leaf()
    case hd :: Nil => hd
    case h1 :: h2 :: tail => pairing(merge(h1, h2) :: tail)
  }
}

case class Leaf[A]() extends Heap[A] {
  def min: A = fail
  def isEmpty = true
  def subs: List[Heap[A]] = fail
  def remove(implicit ord: Ordering[A]): Heap[A] = fail

  private[this] def fail = throw new NoSuchElementException("An empty heap.")
}

case class Branch[A](min: A, subs: List[Heap[A]]) extends Heap[A] {
  def isEmpty = false
  def remove(implicit ord: Ordering[A]): Heap[A] = pairing(subs)
}

object Heap {
  def empty[A]: Heap[A] = Leaf()
}