package lotos.model

import shapeless.labelled.FieldType
import shapeless.{::, HList, Witness}

class MethodT[Name, Params <: HList](
    protected val name: String,
    protected val paramGens: Map[String, AnyRef]
) {
  def param[Key <: String, T](key: Witness.Aux[Key])(
      implicit gen: Gen[T]): MethodT[Name, FieldType[Key, T] :: Params] =
    new MethodT(name, paramGens + (key.value -> gen))
}

object MethodT {
  def name[Name, Params <: HList](methodT: MethodT[Name, Params]): String =
    methodT.name

  def paramGens[Name, Params <: HList](methodT: MethodT[Name, Params]): Map[String, AnyRef] =
    methodT.paramGens
}
