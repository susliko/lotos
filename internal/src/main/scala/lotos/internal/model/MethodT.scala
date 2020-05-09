package lotos.internal.model

import shapeless.labelled.FieldType
import shapeless.{::, HList}

class MethodT[Name, Params <: HList, Errors <: HList](
    protected val name: String,
    protected val paramGens: Map[String, AnyRef]
) {
  def param[key <: String with Singleton, T](key: key)(
      implicit gen: Gen[T]): MethodT[Name, FieldType[key, T] :: Params, Errors] =
    new MethodT(name = this.name, paramGens = this.paramGens + (key -> gen))

  def throws[E <: Throwable]: MethodT[Name, Params, E :: Errors] =
    new MethodT(name = this.name, paramGens = this.paramGens)
}

object MethodT {
  def name[Name, Params <: HList, Errors <: HList](methodT: MethodT[Name, Params, Errors]): String =
    methodT.name

  def paramGens[Name, Params <: HList, Errors <: HList](methodT: MethodT[Name, Params, Errors]): Map[String, AnyRef] =
    methodT.paramGens
}
