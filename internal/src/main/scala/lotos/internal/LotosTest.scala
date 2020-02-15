package lotos.internal

object LotosTest {
  def apply[Impl](spec: SpecT[Impl]) = new Applier[Impl](spec)

  class Applier[Impl](val spec: SpecT[Impl]) extends AnyVal{
    def test[F[_]]: F[Unit] = ???
  }
}
