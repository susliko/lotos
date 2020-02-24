package lotos.internal.testing

import cats.Parallel
import cats.effect.{Concurrent, ContextShift, Sync}
import cats.implicits._
import lotos.internal.model.{LogEvent, Scenario}

trait TestRun[F[_]] {
  def run(scenario: Scenario): F[List[List[LogEvent]]]
}

case class TestRunImpl[F[_]: Concurrent: Parallel](invoke: Invoke[F])(cs: ContextShift[F]) extends TestRun[F] {
  private var freshInvoke: Invoke[F] = invoke

  def run(scenario: Scenario): F[List[List[LogEvent]]] =
    for {
      inv <- Sync[F].delay({
              val inv = freshInvoke
              freshInvoke = inv.copy
              inv
            })
      logs <- scenario.actions.parTraverse(act => cs.shift *> act.flatTraverse(method => inv.invoke(method)))
    } yield logs
}
