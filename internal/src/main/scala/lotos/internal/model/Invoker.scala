package lotos.internal.model

trait Invoker[F[_]] {
  def copy: Invoker[F]
  def invoke(method: String): F[(FuncCall, FuncResp)]
  def methods: List[String]
}
