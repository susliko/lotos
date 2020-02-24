package lotos.testing

import cats.effect.Sync
import lotos.internal.model.SpecT
import lotos.internal.testing.Invoke
import lotos.macros.InvokeConstructor
import shapeless.HList

object LotosTest {
  def apply[F[_]: Sync] = new Applier[F]

  class Applier[F[_]: Sync] {
    def forSpec[Impl, Methods <: HList](spec: SpecT[Impl, Methods]): Invoke[F] =
      macro InvokeConstructor.construct[F, Impl, Methods]
  }
}
