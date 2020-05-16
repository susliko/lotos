package lotos.examples

import cats.effect.{ExitCode, IO, IOApp}
import lotos.model.{Consistency, Gen, TestConfig}
import lotos.testing.LotosTest
import lotos.testing.syntax.{method, spec}

import scala.collection.concurrent.TrieMap

/*_*/
object TrieMapTest extends IOApp {
  val trieMapSpec =
    spec(new TrieMap[Int, String])
      .withMethod(method("put").param("key")(Gen.int(5)).param("value")(Gen.string(1)))
      .withMethod(method("get").param("k")(Gen.int(5)))

  val cfg = TestConfig(parallelism = 3, scenarioLength = 6, scenarioRepetition = 100, scenarioCount = 10)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- LotosTest.forSpec(trieMapSpec, cfg, Consistency.linearizable)
    } yield ExitCode.Success

}
