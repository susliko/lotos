package lotos.internal.testing

import lotos.internal.model.LogEvent

trait Invoke[F[_]] extends Serializable {
  def copy: Invoke[F]
  def invoke(method: String): F[List[LogEvent]]
  def methods: List[String]
}
