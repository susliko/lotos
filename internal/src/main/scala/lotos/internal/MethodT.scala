package lotos.internal

import shapeless.HList

import scala.reflect.ClassTag

case class MethodT(
    name: String,
    paramGens: HList,
    errors: Seq[ClassTag[_]]
) {
  def paramGen[T](name: String, gen: Gen[T]): MethodT =
    this.copy(paramGens = ParamGen(name, gen) :: paramGens)

  def throws[E <: Throwable](implicit ct: ClassTag[E]): MethodT = this.copy(errors = ct +: errors)
}
