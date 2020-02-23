package lotos.testing

import cats.effect.Sync
import lotos.internal.model.{SpecT, Invoker}
import lotos.macros.InvokerConstructor
import shapeless.HList

object LotosTest {
  def apply[F[_]: Sync] = new Applier[F]

  class Applier[F[_]: Sync] {
    def forSpec[Impl, Methods <: HList](spec: SpecT[Impl, Methods]): Invoker[F] =
      macro InvokerConstructor.construct[F, Impl, Methods]
  }
}
