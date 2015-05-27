package org.kframework.func-repr

sealed abstract class InternalType
case class Hom(x : InternalType, y : InternalType) extends InternalType
case class Product(x : InternalType, y : InternalType) extends InternalType
case class Coproduct(x : InternalType, y : InternalType) extends InternalType

sealed abstract class PolynomialFunctor
case class Const( t : InternalType ) extends PolynomialFunctor
case class Horner(t : InternalType, p : Polynomialfunctor) extends PolynomialFunctor
