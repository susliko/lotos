package lotos.internal.model

import scala.util.Random

trait Gen[T] {
  def gen: T
  def show(t: T): String
}

object Gen extends GenPrimitiveInstances

trait GenPrimitiveInstances {
  implicit val intGen: Gen[Int] = new Gen[Int] {
    val random = new Random(System.currentTimeMillis())

    def gen: Int = random.nextInt(10)

    def show(t: Int): String = t.toString
  }
}
