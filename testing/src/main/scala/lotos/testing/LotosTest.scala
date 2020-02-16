package lotos.testing

import lotos.internal.{Invoker, SpecT}
import lotos.macros.InvokerConstructor

object LotosTest {
  def invoker[F[_]] = new Applier[F]

  class Applier[F[_]] {
    def apply[Impl](spec: SpecT[Impl]): Invoker[F] = macro InvokerConstructor.construct[F, Impl]
  }
}
