package dk.brics.scalautil

/** A pair where both components have the same type. */
final case class Twin[+T](val fst:T, val snd:T)
