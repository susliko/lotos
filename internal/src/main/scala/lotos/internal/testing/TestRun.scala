package lotos.internal.testing

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.Parallel
import cats.effect.concurrent.Ref
import cats.implicits._
import lotos.internal.deepcopy._
import lotos.model.MethodResp.Timeout
import lotos.model.{Scenario, TestLog}

import scala.concurrent.duration.FiniteDuration

trait TestRun[F[_]] {
  def run(scenario: Scenario, operationTimeout: FiniteDuration): F[Vector[Vector[TestLog]]]
}

case class TestRunImpl[F[_]: Concurrent: Parallel: Timer](invoke: Invoke[F])(cs: ContextShift[F]) extends TestRun[F] {
  def run(scenario: Scenario, operationTimeout: FiniteDuration): F[Vector[Vector[TestLog]]] =
    for {
      inv <- deepCopyF(invoke)
      logs <- scenario.actions.parTraverse(act =>
               for {
                 logsRef <- Ref.of(Vector.empty[TestLog])
                 _       <- cs.shift
                 _ <- act.collectFirstSomeM(
                       method =>
                         for {
                           log <- inv.invoke(method, operationTimeout)
                           _   <- logsRef.update(_ :+ log)
                           result = log.resp match {
                             case Timeout(_) => Some(())
                             case _          => None
                           }
                         } yield result
                     )
                 logs <- logsRef.get
               } yield logs)
    } yield logs
}
