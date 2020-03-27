package lotos.internal.testing

import cats.Functor
import cats.effect.Sync
import lotos.internal.model.LogEvent
import lotos.internal.deepcopy._
import cats.implicits._

import scala.collection.immutable.ArraySeq
import cats.Monad

sealed trait CheckResult

case class CheckSuccess(linearized: ArraySeq[LogEvent]) extends CheckResult
case object CheckFailure                                extends CheckResult

object lts {

  // Check for the sequential consistency compliance
  def sequentially[F[_]](spec: Invoke[F], logs: List[List[LogEvent]])(implicit F: Sync[F]): F[CheckResult] = {

    val history      = ArraySeq.from(logs).map(ArraySeq.from)
    val eventsCounts = history.map(_.size)

    def go(spec: Invoke[F], candidates: List[(Int, Int)], linearized: ArraySeq[LogEvent]): F[CheckResult] = {
      if (candidates.isEmpty) {
        (CheckSuccess(linearized): CheckResult).pure[F]
      } else
        candidates.zipWithIndex
          .map {
            case ((threadId, order), candidateId) =>
              for {
                specCopy    <- deepCopyF(spec)
                log         = history(threadId)(order)
                moveForward <- validate(specCopy, history(threadId)(order))
                result <- if (moveForward) {
                           val newCandidates =
                             if (order < eventsCounts(threadId) - 1)
                               candidates.updated(candidateId, (threadId, order + 1))
                             else candidates.patch(candidateId, Nil, 1)
                           go(specCopy, newCandidates, linearized :+ log)
                         } else (CheckFailure: CheckResult).pure[F]
              } yield result
          }
          .collectFirstSomeM[F, CheckResult](testAction =>
            testAction.map {
              case x @ CheckSuccess(_) => Some(x)
              case _                   => None
          })
          .map(_.getOrElse(CheckFailure))

    }

    go(spec, List.iterate((0, 0), history.size) { case (threadId, ind) => (threadId + 1, ind) }, ArraySeq.empty)
  }

  private def validate[F[_]: Functor](spec: Invoke[F], event: LogEvent): F[Boolean] =
    spec.seedsInvoke(event.methodName, event.paramSeeds).map(_ == event)
}
