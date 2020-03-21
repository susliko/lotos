package lotos.internal

import java.io.{ObjectInputStream, ObjectOutputStream}

import cats.effect.Sync
import cats.implicits._
import scala.util.Try

package object deepcopy {
  def deepCopy[T <: AnyRef](orig: T): Either[Throwable, T] = {
    Try {
      val fbos = new FastBytesOutputStream();
      val out  = new ObjectOutputStream(fbos)
      out.writeObject(orig)
      out.flush()
      out.close()
      val in = new ObjectInputStream(fbos.getInputStream)
      in.readObject.asInstanceOf[T]
    }.toEither
  }

  def deepCopyF[F[_]: Sync, T <: AnyRef](orig: T): F[T] = Sync[F].delay(deepCopy(orig)).rethrow
}
