package lotos.testing

class Stack[T] extends Serializable {
  var l: List[T] = List.empty

  def push(elem: T): Unit = l = List(elem)

  def pop(): Option[T] = l.headOption
}