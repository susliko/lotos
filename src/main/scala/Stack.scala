trait Stack[T] {
  def push(t: T): Unit

  def pop(): Option[T]
}
