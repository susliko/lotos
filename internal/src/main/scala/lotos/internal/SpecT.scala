package lotos.internal

case class SpecT[I](
    construct: () => I,
    methods: Seq[MethodT],
) {
  def methods(ms: MethodT*): SpecT[I] = this.copy(methods = ms ++ methods)
}
