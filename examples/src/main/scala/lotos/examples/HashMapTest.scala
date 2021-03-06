package lotos.examples

import cats.effect.{ExitCode, IO, IOApp}
import lotos.model.{Consistency, FailureFormat, Gen, TestConfig}
import lotos.testing.LotosTest
import lotos.testing.syntax.{method, spec}
import java.{util => ju}

import scala.concurrent.duration._

class UnsafeHashMap extends Serializable {
  val underlying                                   = new ju.HashMap[Int, String]
  def put(key: Int, value: String): Option[String] = Option(underlying.put(key, value))
  def get(key: Int): String = {
    Thread.sleep(1500)
    Option(underlying.get(key)).getOrElse(throw new NoSuchElementException)
  }
}

/*_*/
object HashMapTest extends IOApp {
  val hashMapSpec =
    spec(new UnsafeHashMap)
      .withMethod(method("put").param("key")(Gen.int(1)).param("value")(Gen.string(1)))
      .withMethod(method("get").param("key")(Gen.int(1)))

  val cfg = TestConfig(
    parallelism = 2,
    scenarioLength = 2,
    scenarioRepetition = 20,
    scenarioCount = 5,
    operationTimeout = 1.second,
    failureFormat = FailureFormat.Diagram("test.html")
  )

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- LotosTest.forSpec(hashMapSpec, cfg, Consistency.linearizable)
    } yield ExitCode.Success
}
