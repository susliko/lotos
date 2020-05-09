package lotos.internal.testing

import lotos.model.TestLog

trait Invoke[F[_]] extends Serializable {
  def copy: Invoke[F]
  def invoke(method: String): F[TestLog]
  def invokeWithSeeds(method: String, seeds: Map[String, Long]): F[TestLog]
  def methods: List[String]
}
