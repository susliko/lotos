package lotos.internal.testing

import cats.Functor
import cats.effect.Sync
import cats.implicits._
import lotos.internal.deepcopy._
import lotos.model.TestLog

object lts {
  sealed trait CheckResult

  case class CheckSuccess(linearized: Vector[TestLog]) extends CheckResult
  case object CheckFailure                             extends CheckResult
  case object CheckNotStarted                          extends CheckResult

  // Check for the sequential consistency compliance
  def sequentially[F[_]](spec: Invoke[F], logs: Vector[Vector[TestLog]])(implicit F: Sync[F]): F[CheckResult] = {

    val eventsCounts = logs.map(_.size)
    val eventsTotal  = eventsCounts.sum

    go(spec,
       List.iterate((0, 0), logs.size) { case (threadId, ind) => (threadId + 1, ind) },
       Vector.empty,
       logs,
       eventsCounts,
       eventsTotal,
       considerTimes = false)
  }

  // Check for the linearizability compilance
  def linearizable[F[_]](spec: Invoke[F], logs: Vector[Vector[TestLog]])(implicit F: Sync[F]): F[CheckResult] = {
    val eventsCounts = logs.map(_.size)
    val eventsTotal  = eventsCounts.sum

    go(spec,
       List.iterate((0, 0), logs.size) { case (threadId, ind) => (threadId + 1, ind) },
       Vector.empty,
       logs,
       eventsCounts,
       eventsTotal,
       considerTimes = true)
  }

  private def go[F[_]: Sync](spec: Invoke[F],
                             candidates: List[(Int, Int)],
                             explanation: Vector[TestLog],
                             logs: Vector[Vector[TestLog]],
                             eventsCounts: Vector[Int],
                             eventsTotal: Long,
                             considerTimes: Boolean): F[CheckResult] = {
    if (explanation.size == eventsTotal) {
      (CheckSuccess(explanation): CheckResult).pure[F]
    } else {
      val filteredCandidates = if (considerTimes) {
        val maxEnding =
          candidates.map { case (threadId, order) => logs(threadId)(order) }.minBy(_.resp.timestamp).resp.timestamp
        candidates.zipWithIndex.filter {
          case ((threadId, order), _) => logs(threadId)(order).call.timestamp < maxEnding
        }
      } else candidates.zipWithIndex
      filteredCandidates
        .map {
          case ((threadId, order), candidateId) =>
            for {
              specCopy    <- deepCopyF(spec)
              log         = logs(threadId)(order)
              moveForward <- validate(specCopy, logs(threadId)(order))
              result <- if (moveForward) {
                         val newCandidates =
                           if (order < eventsCounts(threadId) - 1)
                             candidates.updated(candidateId, (threadId, order + 1))
                           else candidates.patch(candidateId, Nil, 1)
                         go(specCopy, newCandidates, explanation :+ log, logs, eventsCounts, eventsTotal, considerTimes)
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

  private def validate[F[_]: Functor](spec: Invoke[F], event: TestLog): F[Boolean] =
    spec
      .invokeWithSeeds(event.call.methodName, event.call.paramSeeds)
      .map(specEvent => TestLog.catsEq.eqv(specEvent, event))
}
