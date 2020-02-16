package lotos

package object macros {
  type NList[T]      = List[(String, T)]
  type MethodDecl[T] = (List[NList[T]], T)
}
