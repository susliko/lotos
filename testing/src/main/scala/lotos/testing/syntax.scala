package lotos.testing

import lotos.internal.model.{MethodT, SpecT}
import shapeless.HNil

package object syntax {
  def method[name <: String with Singleton](name: name): MethodT[name, HNil, HNil] =
    new MethodT(name = name, paramGens = Map.empty)

  def spec[I](construct: => I): SpecT[I, HNil] =
    new SpecT(construct = () => construct, methods = Map.empty)
}
