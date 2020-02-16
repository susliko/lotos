package lotos.internal.model

import shapeless.{::, HList}

class SpecT[I, Methods <: HList](
    val construct: () => I,
    val methods: Methods,
) {
  def withMethod[Name, ParamGens <: HList, Errors <: HList](
      m: MethodT[Name, ParamGens, Errors]): SpecT[I, MethodT[Name, ParamGens, Errors] :: Methods] =
    new SpecT(construct = this.construct, methods = m :: methods)
}
