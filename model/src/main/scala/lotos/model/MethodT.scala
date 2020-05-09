package lotos.model

import shapeless.labelled.FieldType
import shapeless.{::, HList}

class MethodT[Name, Params <: HList, Errors <: HList](
    protected val name: String,
    protected val paramGens: Map[String, AnyRef]
) {
  def param[Key <: String with Singleton, T](key: Key)(
      implicit gen: Gen[T]): MethodT[Name, FieldType[Key, T] :: Params, Errors] =
    copy(newParamGens = Map(key -> gen))

  def throws[E <: Throwable]: MethodT[Name, Params, E :: Errors] =
    copy()

  private def copy[N, P <: HList, E <: HList](newParamGens: Map[String, AnyRef] = Map.empty) =
    new MethodT[N, P, E](name, paramGens ++ newParamGens)
}

object MethodT {
  def name[Name, Params <: HList, Errors <: HList](methodT: MethodT[Name, Params, Errors]): String =
    methodT.name

  def paramGens[Name, Params <: HList, Errors <: HList](methodT: MethodT[Name, Params, Errors]): Map[String, AnyRef] =
    methodT.paramGens
}
