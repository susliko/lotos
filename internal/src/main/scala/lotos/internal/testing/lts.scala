package lotos.internal.testing

import cats.Functor
import cats.effect.Sync
import cats.implicits._
import lotos.internal.deepcopy._
import lotos.model.{FuncInvocation, LogEvent}

import scala.collection.immutable.ArraySeq

object lts {
  sealed trait CheckResult

  case class CheckSuccess(linearized: ArraySeq[LogEvent]) extends CheckResult
  case object CheckFailure                                extends CheckResult

  // Check for the sequential consistency compliance
  def sequentially[F[_]](spec: Invoke[F], logs: List[List[FuncInvocation]])(implicit F: Sync[F]): F[CheckResult] = {

    val history      = ArraySeq.from(logs).map(ArraySeq.from)
    val eventsCounts = history.map(_.size)
    val eventsTotal  = eventsCounts.sum

    go(spec,
       List.iterate((0, 0), history.size) { case (threadId, ind) => (threadId + 1, ind) },
       ArraySeq.empty,
       history,
       eventsCounts,
       eventsTotal,
       false)
  }

  // Check for the linearizability compilance
  def linearizable[F[_]](spec: Invoke[F], logs: List[List[FuncInvocation]])(implicit F: Sync[F]): F[CheckResult] = {
    val history      = ArraySeq.from(logs.map(ArraySeq.from))
    val eventsCounts = history.map(_.size)
    val eventsTotal  = eventsCounts.sum

    go(spec,
       List.iterate((0, 0), history.size) { case (threadId, ind) => (threadId + 1, ind) },
       ArraySeq.empty,
       history,
       eventsCounts,
       eventsTotal,
       true)
  }

  private def go[F[_]: Sync](spec: Invoke[F],
                             candidates: List[(Int, Int)],
                             explanation: ArraySeq[FuncInvocation],
                             history: ArraySeq[ArraySeq[FuncInvocation]],
                             eventsCounts: ArraySeq[Int],
                             eventsTotal: Long,
                             linearizability: Boolean): F[CheckResult] = {
    if (explanation.size == eventsTotal) {
      (CheckSuccess(explanation): CheckResult).pure[F]
    } else {
      val filteredCandidates = if (linearizability) {
        val maxEnding = candidates.map { case (threadId, order) => history(threadId)(order) }.minBy(_.end).end
        candidates.zipWithIndex.filter { case ((threadId, order), _) => history(threadId)(order).start < maxEnding }
      } else candidates.zipWithIndex
      filteredCandidates
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
                         go(specCopy,
                            newCandidates,
                            explanation :+ log,
                            history,
                            eventsCounts,
                            eventsTotal,
                            linearizability)
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
  }

  private def validate[F[_]: Functor](spec: Invoke[F], event: LogEvent): F[Boolean] =
    spec.invokeWithSeeds(event.methodName, event.paramSeeds).map(specEvent => LogEvent.catsEq.eqv(specEvent, event))
}
