package lotos.examples

import java.util.concurrent.atomic.AtomicReference

import cats.effect.{ExitCode, IO, IOApp}
import lotos.model.{Consistency, Gen, TestConfig}
import lotos.testing.LotosTest
import lotos.testing.syntax._

import scala.concurrent.duration._

case class Node[T](value: T, next: Node[T] = null)

class LFStack[T] extends Serializable {
  val stack: AtomicReference[Node[T]] = new AtomicReference[Node[T]]()

  def push(item: T): Unit = {
    var oldHead: Node[T] = null
    var newHead          = Node(item)
    do {
      oldHead = stack.get();
      newHead = newHead.copy(next = oldHead)
    } while (!stack.compareAndSet(oldHead, newHead))
  }

  def pop: Option[T] = {
    var oldHead: Node[T] = null
    var newHead: Node[T] = null
    do {
      oldHead = stack.get()
      if (oldHead == null)
        return None

      newHead = oldHead.next
    } while (!stack.compareAndSet(oldHead, newHead))
    Some(oldHead.value)
  }
}

object LFStackTest extends IOApp {
  val stackSpec =
    spec(new LFStack[Int])
      .withMethod(method("push").param("item")(Gen.int(10)))
      .withMethod(method("pop"))

  val cfg = TestConfig(
    parallelism = 2,
    scenarioLength = 10,
    scenarioRepetition = 100,
    scenarioCount = 50
  )

  def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- LotosTest.forSpec(stackSpec, cfg, Consistency.linearizable)
    } yield ExitCode.Success
}
