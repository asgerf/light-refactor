package dk.brics.scalautil

/** A 3-tuple where all components have the same type. */
final case class Triplet[+A](val fst:A, val snd:A, val trd:A)
