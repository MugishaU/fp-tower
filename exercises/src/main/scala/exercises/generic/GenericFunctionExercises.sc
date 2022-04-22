import exercises.generic.GenericFunctionExercises._

val code: Pair[List[Byte]] = Pair(
  first = List(103, 110, 105, 109, 109, 97, 114, 103, 111, 114, 80),
  second = List(108, 97, 110, 111, 105, 116, 99, 110, 117, 70)
)

code.map(_.toArray.reverse).swap.map(new String(_))

val g = "Mike"

g.toCharArray.head
