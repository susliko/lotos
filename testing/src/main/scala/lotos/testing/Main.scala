package lotos.testing

import lotos.testing.syntax.{method, spec}
import cats.effect.{ExitCode, IO, IOApp}
import lotos.internal.model.{Gen, TestedImpl}

/*_*/
object Main extends IOApp {

  val stackSpec =
    spec(new Stack[Int])
      .withMethod(method("push").paramGen("elem", Gen.intGen))
      .withMethod(method("pop").throws[RuntimeException])

  //  println(method("pop").throws[RuntimeException].name)

  val inv: TestedImpl[IO] = LotosTest[IO].forSpec(stackSpec)
  override def run(args: List[String]): IO[ExitCode] =
    for {
      res <- inv.invoke("peka")
      _   = println(res)
    } yield ExitCode.Success
}
