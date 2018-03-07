package uk.ac.wellcome.dynamo

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

  // Generates a UpdateExpression for an HList
  implicit def hlistExpressionGenerator[L <: HList, A, B <: HList, C <: HList](
    implicit remover: Remover.Aux[L, IdKey, (A, B)],
    folder: LeftFolder.Aux[B,
                           Option[UpdateExpression],
                           buildUpdateExpression.type,
                           Option[UpdateExpression]]
  ) =
    create { t: L =>
      {
        val recordAsHlist = t - 'id
        recordAsHlist.foldLeft[Option[UpdateExpression]](None)(
          buildUpdateExpression)
      }
    }

  // Generates an UpdateExpressionGenerator for a case class
  implicit def productUpdateExpressionGenerator[C, L](
    implicit labelledGeneric: LabelledGeneric.Aux[C, L],
    updateExpressionGenerator: UpdateExpressionGenerator[L]) = create[C] {
    c: L =>
      updateExpressionGenerator.generateUpdateExpression(labelledGeneric.to(c))
  }

  // Creates an UpdateExpression out of a FieldType and adds it to the
  // accumulating UpdateExpression
  object buildUpdateExpression extends Poly2 {
    implicit def fold[K <: Symbol, V](
      implicit witness: Witness.Aux[K],
      dynamoFormat: DynamoFormat[V]): Case.Aux[Option[UpdateExpression],
                                               FieldType[K, V],
                                               Option[UpdateExpression]] =
      at[Option[UpdateExpression], FieldType[K, V]] {
        (maybeUpdateExpression, fieldType) =>
          val fieldValue: V = fieldType
          val partUpdate = set(witness.value -> fieldValue)

          maybeUpdateExpression match {
            case Some(updateExpression) =>
              Some(updateExpression and partUpdate)
            case None => Some(partUpdate)
          }
      }
  }
}
