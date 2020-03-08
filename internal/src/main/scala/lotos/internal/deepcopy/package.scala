package lotos.internal

import java.io.{ObjectInputStream, ObjectOutputStream}

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
}
