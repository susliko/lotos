package lotos.internal.testing

import cats.Functor
import cats.effect.Sync
import lotos.internal.model.LogEvent
import lotos.internal.deepcopy._
import cats.implicits._

import scala.collection.immutable.ArraySeq

sealed trait TestResult

case object TestSuccess extends TestResult
case object TestFailure extends TestResult

object LTS {
  def check[F[_]](spec: Invoke[F], logs: List[List[LogEvent]])(implicit F: Sync[F]): F[TestResult] = {

    val history      = ArraySeq.from(logs).map(ArraySeq.from)
    val eventsCounts = history.map(_.size)

    def go(spec: Invoke[F], candidates: List[(Int, Int)]): F[TestResult] = {
      if (candidates.isEmpty) (TestSuccess: TestResult).pure[F]
      else
        candidates.zipWithIndex
          .map {
            case ((threadId, order), candidateId) =>
              for {
                specCopy    <- deepCopyF(spec)
                moveForward <- validate(specCopy, history(threadId)(order))
                result <- if (moveForward) {
                           val newCandidates =
                             if (order < eventsCounts(threadId) - 1)
                               candidates.updated(candidateId, (threadId, order + 1))
                             else candidates.patch(candidateId, Nil, 1)
                           go(specCopy, newCandidates)
                         } else (TestFailure: TestResult).pure[F]
              } yield result
          }
          .findM[F](testAction => testAction.map(_ == TestSuccess))
          .flatMap(_.sequence)
          .map(_.getOrElse(TestFailure))

    }

    go(spec, List.iterate((0, 0), history.size) { case (threadId, ind) => (threadId + 1, ind) })
  }

  private def validate[F[_]: Functor](spec: Invoke[F], event: LogEvent): F[Boolean] =
    spec.invoke(event.methodName).map(_ == event)
}
