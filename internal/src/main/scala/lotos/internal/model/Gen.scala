package lotos.internal.model

trait Gen[T] {
  def gen(seed: Long): T
  def show(t: T): String
}

object Gen extends GenPrimitiveInstances

trait GenPrimitiveInstances {
  implicit val intGen: Gen[Int] = new Gen[Int] {
    def gen(seed: Long): Int = (seed % 10).toInt

    def show(t: Int): String = t.toString
  }

  implicit val stringGen: Gen[String] = new Gen[String] {
    val alphabet = ('a' to 'z').zipWithIndex.map { case (a, b) => (b.toLong, a) }.toMap
    def gen(seed: Long): String =
      new String(Array.fill(5)("").zipWithIndex.map { case (_, ind) => alphabet((seed << ind) % 26) })

    def show(t: String): String = t
  }
}
