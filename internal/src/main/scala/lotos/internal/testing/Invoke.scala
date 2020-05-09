package lotos.internal.testing

import lotos.model.FuncInvocation

trait Invoke[F[_]] extends Serializable {
  def copy: Invoke[F]
  def invoke(method: String): F[FuncInvocation]
  def invokeWithSeeds(method: String, seeds: Map[String, Long]): F[FuncInvocation]
  def methods: List[String]
}
