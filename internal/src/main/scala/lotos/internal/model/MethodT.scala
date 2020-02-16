package lotos.internal.model

import shapeless.labelled.FieldType
import shapeless.{::, HList, Witness}

class MethodT[Name, Params <: HList, Errors <: HList](
    val name: String,
    val paramGens: Map[String, AnyRef]
) {
  def paramGen[key <: String, T](w: Witness.Aux[key], gen: Gen[T]): MethodT[Name, FieldType[key, T] :: Params, Errors] =
    new MethodT(name = this.name, paramGens = this.paramGens + (w.value.toString -> gen))

  def throws[E <: Throwable]: MethodT[Name, Params, E :: Errors] =
    new MethodT(name = this.name, paramGens = this.paramGens)
}
