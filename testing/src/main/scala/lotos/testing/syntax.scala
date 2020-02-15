package lotos.testing

import lotos.internal.{MethodT, SpecT}
import shapeless.HNil

package object syntax {
  def method(name: String): MethodT = MethodT(name = name, paramGens = HNil, errors = Nil)
  def specFor[T](construct: => T) = SpecT(() => construct, Nil)
}
