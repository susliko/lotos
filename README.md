[![Maven Central](https://img.shields.io/maven-central/v/dev.susliko/lotos-testing_2.13.svg)](https://search.maven.org/search?q=dev.susliko.lotos-testing)

A library for testing concurrent data structures that you lacked!

*Lotos* aims to provide:
1. Simple DSL for specifications
2. Configurable generator of test scenarios
3. Various consistency model checkers
4. Verbose consistency violation reports

# Quick example

Having some implementation of a data structure:
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
      .withMethod(
        method("put")
          .param("key")(Gen.intGen(1))
          .param("value")(Gen.stringGen(1)))
      .withMethod(
        method("get")
          .param("key")(Gen.intGen(1)))

  val cfg = TestConfig(parallelism = 2, scenarioLength = 2, scenarioRepetition = 3, scenarioCount = 5)

  def run(args: List[String]): IO[ExitCode] =
      LotosTest.forSpec(hashMapSpec, cfg, Consistency.sequential) as ExitCode.Success
}
```

In case of failure it will provide a scenario which failed to comply with specified guaranties (e.g. sequential consistency):

```
Testing scenario 1
Test failed for scenario:
 put(key = 0, value = i): None | get(key = 0): None            
 get(key = 0): Some(i)         | put(key = 0, value = t): None 
```
