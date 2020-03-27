package lotos.examples

import cats.effect.{ExitCode, IO, IOApp}
import lotos.internal.model.{Consistency, Gen}
import lotos.internal.testing.TestConfig
import lotos.testing.LotosTest
import lotos.testing.syntax.{method, spec}

import scala.collection.concurrent.TrieMap

/*_*/
object TrieMapTest extends IOApp {
  val trieMapSpec =
    spec(new TrieMap[Int, String])
      .withMethod(method("put").param("key")(Gen.intGen(5)).param("value")(Gen.stringGen(1)))
      .withMethod(method("get").param("k")(Gen.intGen(5)))

  val cfg = TestConfig(parallelism = 2, scenarioLength = 10, scenarioRepetition = 3, scenarioCount = 5)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- LotosTest.forSpec(trieMapSpec, cfg, Consistency.sequential)
    } yield ExitCode.Success

}
