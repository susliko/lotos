package lotos.internal.model

import shapeless.{::, HList}

class SpecT[I, Methods <: HList](
    protected val construct: () => I,
    protected val methods: Map[String, AnyRef],
) {
  def withMethod[Name, ParamGens <: HList, Errors <: HList](
      m: MethodT[Name, ParamGens, Errors]): SpecT[I, MethodT[Name, ParamGens, Errors] :: Methods] =
    new SpecT(construct = this.construct, methods = methods + (MethodT.name(m) -> m))
}

object SpecT {
  def construct[I, Methods <: HList](specT: SpecT[I, Methods]): I = specT.construct()

  def methods[I, Methods <: HList](specT: SpecT[I, Methods]): Map[String, AnyRef] = specT.methods
}
