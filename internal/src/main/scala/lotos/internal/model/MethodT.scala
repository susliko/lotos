package lotos.internal.model

import shapeless.labelled.FieldType
import shapeless.{::, HList, Witness}

class MethodT[Name, Params <: HList, Errors <: HList](
    protected val name: String,
    protected val paramGens: Map[String, AnyRef]
) {
  def param[key <: String, T](w: Witness.Aux[key])(
      implicit gen: Gen[T]): MethodT[Name, FieldType[key, T] :: Params, Errors] =
    new MethodT(name = this.name, paramGens = this.paramGens + (w.value.toString -> gen))

  def throws[E <: Throwable]: MethodT[Name, Params, E :: Errors] =
    new MethodT(name = this.name, paramGens = this.paramGens)
}

object MethodT {
  def name[Name, Params <: HList, Errors <: HList](methodT: MethodT[Name, Params, Errors]): String =
    methodT.name

  def paramGens[Name, Params <: HList, Errors <: HList](methodT: MethodT[Name, Params, Errors]): Map[String, AnyRef] =
    methodT.paramGens
}
