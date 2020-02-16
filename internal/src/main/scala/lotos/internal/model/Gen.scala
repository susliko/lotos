package lotos.internal.model

trait Gen[T] {
  def gen: T
  def show(t: T): String
}

object Gen extends GenPrimitiveInstances

trait GenPrimitiveInstances {
  implicit val intGen: Gen[Int] = new Gen[Int] {
    def gen: Int = 42

    def show(t: Int): String = t.toString
  }
}

case class ParamGen[T](name: String, gen: Gen[T])
