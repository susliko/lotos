package lotos.internal.model

sealed trait Consistency

object Consistency {
  case object sequential   extends Consistency
  case object linearizable extends Consistency
}
