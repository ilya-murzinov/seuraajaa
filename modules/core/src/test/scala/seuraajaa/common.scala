import monix.eval.Task
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.TimeLimits
import org.scalatest.prop.PropertyChecks

import scala.concurrent.duration.DurationLong

package object common {

  trait AsyncSupport {
    implicit class TaskOps[A](task: Task[A]) {
      import monix.execution.Scheduler.Implicits.global

      def run: A = task.runSyncUnsafe(1.second)
    }
  }

  trait Spec
    extends PropSpec
      with PropertyChecks
      with Matchers
      with MockFactory
      with AsyncSupport

  trait Test
    extends FlatSpec
      with Matchers
      with TimeLimits
      with MockFactory
      with AsyncSupport
}
