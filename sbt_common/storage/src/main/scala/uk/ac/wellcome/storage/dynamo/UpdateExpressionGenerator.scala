package uk.ac.wellcome.storage.dynamo

import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.update.UpdateExpression
import com.gu.scanamo.syntax._
import shapeless.{HList, LabelledGeneric, Poly1, Poly2, Witness}
import shapeless.labelled.FieldType
import shapeless.ops.hlist.{LeftFolder, Mapper}
import shapeless.ops.record.Remover
import shapeless.record._

trait UpdateExpressionGenerator[T] {
  def generateUpdateExpression(t: T): Option[UpdateExpression]
}

object UpdateExpressionGenerator {
  val w = Witness(Symbol("id"))
  type IdKey = w.T

  def apply[T](implicit generator: UpdateExpressionGenerator[T]) = generator

  def create[T](f: (T) => Option[UpdateExpression]) =
    new UpdateExpressionGenerator[T] {
      override def generateUpdateExpression(record: T) = f(record)
    }

  // Generate an UpdateExpression for an HList.
  //
  // Specifically, it creates a Scanamo UpdateExpression that updates
  // every field in the HList except "id".
  //
  // Remover is a Shapeless type class that removes fields from an HList.
  // It takes three type parameters:
  //
  //  - the input (L)
  //  - the field to remove (IdKey)
  //  - a tuple containing the removed value, and the remaining HList (A, B)
  //
  // LeftFolder is a Shapeless type class that left folds over an HList.
  // It takes four type parameters:
  //
  //  - the input (B)
  //  - the accumulated value (Option[UpdateExpression])
  //  - the operation (buildUpdateExpression.type)
  //  - the output (Option[UpdateExpression])
  //
  implicit def hlistExpressionGenerator[L <: HList, B <: HList, C <: HList](
    implicit remover: Remover.Aux[L, IdKey, (String, B)],
    folder: LeftFolder.Aux[B,
                           Option[UpdateExpression],
                           buildUpdateExpression.type,
                           Option[UpdateExpression]]
  ) =
    create { t: L =>
      {
        val listWithoutId = t - 'id
        listWithoutId.foldLeft[Option[UpdateExpression]](None)(
          buildUpdateExpression)
      }
    }

  // Generates an UpdateExpressionGenerator for a case class ("product" in Shapeless).
  //
  // LabelledGeneric is a Shapeless type class that allows us to convert
  // between case classes and their HList representation.
  //
  // labelledGeneric.to(c) converts the case class C to an HList L, and then we
  // can use the constructor above.
  //
  implicit def productUpdateExpressionGenerator[C, L <: HList](
    implicit labelledGeneric: LabelledGeneric.Aux[C, L],
    updateExpressionGenerator: UpdateExpressionGenerator[L]) = create[C] {
    c: C =>
      updateExpressionGenerator.generateUpdateExpression(labelledGeneric.to(c))
  }

  // Creates an UpdateExpression out of a FieldType and adds it to the
  // accumulating UpdateExpression.
  //
  // Poly2 is a Shapeless type class that represents a polymorphic function
  // with two arguments.  For more info, see § 7.2 "Polymorphic functions" of
  // "The Type Astronaut’s Guide to Shapeless".
  //
  // Case is a Shapeless type class where the polymorphic function is defined.
  // Because 'fold' is a function with two arguments, there are three type
  // parameters:
  //
  //  - The first input (Option[UpdateExpression])
  //  - The second input (FieldType[K, V])
  //  - The return value (Option[UpdateExpression])
  //
  object buildUpdateExpression extends Poly2 {
    implicit def fold[K <: Symbol, V](
      implicit witness: Witness.Aux[K],
      dynamoFormat: DynamoFormat[V]): Case.Aux[Option[UpdateExpression],
                                               FieldType[K, V],
                                               Option[UpdateExpression]] =
      at[Option[UpdateExpression], FieldType[K, V]] {
        (maybeUpdateExpression, fieldType) =>
          // Casting to type V gives us the value of the field.
          val fieldValue: V = fieldType

          // This is an Update for a single field in the Scanamo DSL.
          val partUpdate = set(witness.value -> fieldValue)

          maybeUpdateExpression match {
            case Some(updateExpression) =>
              Some(updateExpression and partUpdate)
            case None => Some(partUpdate)
          }
      }
  }
}
