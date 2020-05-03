package lotos.examples

import cats.effect.{ExitCode, IO, IOApp}
import lotos.internal.model.{Consistency, Gen}
import lotos.internal.testing.TestConfig
import lotos.testing.LotosTest
import lotos.testing.syntax.{method, spec}

import java.{util => ju}

class UnsafeHashMap extends Serializable {
  val underlying = new ju.HashMap[Int, String]
  def put(key: Int, value: String): Option[String] = Option(underlying.put(key, value))
  def get(key: Int): Option[String] = Option(underlying.get(key))
}

/*_*/
object HashMapTest extends IOApp {
  val hashMapSpec =
    spec(new UnsafeHashMap)
      .withMethod(method("put").param("key")(Gen.intGen(1)).param("value")(Gen.stringGen(1)))
      .withMethod(method("get").param("key")(Gen.intGen(1)))

  val cfg = TestConfig(parallelism = 2, scenarioLength = 2, scenarioRepetition = 20, scenarioCount = 5)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- LotosTest.forSpec(hashMapSpec, cfg, Consistency.linearizable)
    } yield ExitCode.Success

}
