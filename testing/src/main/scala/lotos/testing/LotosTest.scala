package lotos.testing

import cats.MonadError
import lotos.internal.model.{SpecT, TestedImpl}
import lotos.macros.TestedImplConstructor
import shapeless.HList

object LotosTest {
  def apply[F[_]: MonadError[*[_], Throwable]] = new Applier[F]

  class Applier[F[_]: MonadError[*[_], Throwable]] {
    def forSpec[Impl, Methods <: HList](spec: SpecT[Impl, Methods]): TestedImpl[F] =
      macro TestedImplConstructor.construct[F, Impl, Methods]
  }
}
