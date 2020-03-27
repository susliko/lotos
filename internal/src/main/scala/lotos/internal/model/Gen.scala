package lotos.internal.model

trait Gen[T] {
  def gen(seed: Long): T
  def show(t: T): String
}

object Gen extends GenPrimitiveInstances

trait GenPrimitiveInstances {
  implicit def intGen(limit: Int = 10): Gen[Int] = new Gen[Int] {
    def gen(seed: Long): Int = (seed % limit).toInt

    def show(t: Int): String = t.toString
  }

  implicit def stringGen(length: Int = 5): Gen[String] = new Gen[String] {
    val alphabet = ('a' to 'z').zipWithIndex.map { case (a, b) => (b.toLong, a) }.toMap
    def gen(seed: Long): String =
      new String(Array.fill(length)("").zipWithIndex.map {
        case (_, ind) =>
          alphabet(Math.abs((seed << ind) % 26))
      })

    def show(t: String): String = t
  }
}
