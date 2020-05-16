package lotos.testing

import lotos.model.{MethodT, SpecT}
import shapeless.{HNil, Witness}

package object syntax {
  def method[Name <: String with Singleton](name: Name): MethodT[Name, HNil] =
    new MethodT(name = name, paramGens = Map.empty)

  def spec[I](construct: => I): SpecT[I, HNil] =
    new SpecT(construct = () => construct, methods = Map.empty)
}
