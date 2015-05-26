package org.kframework.func-repr


case class TypeName( tname : String )

// Aliased for clarity.
case class ConstructorName( cname : String )
case class ConstructorArg( tn : TypeName )
case class ConstructorArgs( args : List[ConstructorArg])

case class Constructor( name : ConstructorName, args : ConstructorArgs)
case class ADTDefinition( constructors : List[Constructor])
