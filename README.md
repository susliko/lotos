A library for testing concurrent data structures that you lacked!

*Lotos* aims to provide a simple DSL and various concurrent guarantees checkers for your concurrent data structures.

# Quick example

Having some implementation:
```scala
import java.{util => ju}

class UnsafeHashMap extends Serializable {
    val underlying = new ju.HashMap[Int, String]
    def put(key: Int, value: String): Option[String] = Option(underlying.put(key, value))
    def get(key: Int): Option[String] = Option(underlying.get(key))
}
```

describe its specification via Lotos DSL and run the test:
```scala
object UnsafeHashMapTest extends IOApp {
  val hashMapSpec =
    spec(new UnsafeHashMap)
      .withMethod(method("put").param("key")(Gen.intGen(1)).param("value")(Gen.stringGen(1)))
      .withMethod(method("get").param("key")(Gen.intGen(1)))

  val cfg = TestConfig(parallelism = 2, scenarioLength = 2, scenarioRepetition = 3, scenarioCount = 5)

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- LotosTest.forSpec(hashMapSpec, cfg, Consistency.sequential)
    } yield ExitCode.Success

}
```

In case of failure it will provide a scenario which failed to comply with specified guaranties (e.g. sequential consistency):

```
Testing scenario 1
Test failed for scenario:
 put(key = 0, value = i): None | get(key = 0): None            
 get(key = 0): Some(i)         | put(key = 0, value = t): None 
```
