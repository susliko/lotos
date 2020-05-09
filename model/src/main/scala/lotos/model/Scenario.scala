package lotos.model

import scala.util.Random

case class Scenario(actions: List[List[String]], parallelism: Int, length: Int)

object Scenario {
  def gen(methods: List[String], parallelism: Int, length: Int): Scenario =
    Scenario(List.fill(parallelism)(Random.shuffle(methods.flatMap(List.fill(length)(_))).take(length)),
             parallelism,
             length)
}
