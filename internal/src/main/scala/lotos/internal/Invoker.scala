package lotos.internal

trait Invoker[F[_]] {
  def invoke(method: String): F[String]
}
