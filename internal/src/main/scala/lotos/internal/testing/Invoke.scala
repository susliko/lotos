package lotos.internal.testing

import lotos.model.TestLog

import scala.concurrent.duration.FiniteDuration

trait Invoke[F[_]] extends Serializable {
  def copy: Invoke[F]
  def invoke(method: String, timeout: FiniteDuration): F[TestLog]
  def invokeWithSeeds(method: String, seeds: Map[String, Long]): F[TestLog]
  def methods: Vector[String]
}
