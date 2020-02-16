package lotos.internal.model

trait TestedImpl[F[_]] {
  def copy: TestedImpl[F]
  def invoke(method: String): F[String]
}
