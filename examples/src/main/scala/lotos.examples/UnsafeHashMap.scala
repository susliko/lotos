package lotos.examples

import java.{util => ju}

class UnsafeHashMap extends Serializable {
    val underlying = new ju.HashMap[Int, String]
    def put(key: Int, value: String): Option[String] = Option(underlying.put(key, value))
    def get(key: Int): Option[String] = Option(underlying.get(key))
}
