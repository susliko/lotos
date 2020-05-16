package lotos.model

import scala.util.Random

case class Scenario(actions: Vector[Vector[String]])

object Scenario {
  def gen(methods: Vector[String], parallelism: Int, length: Int): Scenario =
    Scenario(Vector.fill(parallelism)(Random.shuffle(methods.flatMap(Vector.fill(length)(_))).take(length)))
}
