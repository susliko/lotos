package lotos.internal.testing

import cats.Parallel
import cats.effect.{Concurrent, ContextShift}
import cats.implicits._
import lotos.internal.deepcopy._
import lotos.model.{TestLog, Scenario}

trait TestRun[F[_]] {
  def run(scenario: Scenario): F[List[List[TestLog]]]
}

case class TestRunImpl[F[_]: Concurrent: Parallel](invoke: Invoke[F])(cs: ContextShift[F]) extends TestRun[F] {
  def run(scenario: Scenario): F[List[List[TestLog]]] =
    for {
      inv  <- deepCopyF(invoke)
      logs <- scenario.actions.parTraverse(act => cs.shift *> act.traverse(method => inv.invoke(method)))
    } yield logs
}
