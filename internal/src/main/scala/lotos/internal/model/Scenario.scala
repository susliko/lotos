package lotos.internal.model

import scala.util.Random

object Scenario {
  def gen(methods: List[String], parallelism: Int, length: Int): List[List[String]] =
    List.fill(parallelism)(Random.shuffle(methods.flatMap(List.fill(length)(_))))
}
