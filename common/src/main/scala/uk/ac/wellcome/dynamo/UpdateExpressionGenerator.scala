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
  implicit def hlistExpressionGenerator[L <: HList,
                                                A,
                                                B <: HList,
                                                C <: HList](
    implicit remover: Remover.Aux[L, IdKey, (A, B)],
    mapper: Mapper.Aux[toUpdateExpressions.type, B, C],
    folder: LeftFolder.Aux[C,
                           Option[UpdateExpression],
                           combineUpdateExpressions.type,
                           Option[UpdateExpression]]) =
    create { t: L =>
      {
        val recordAsHlist = t - 'id
        val nonIdTaggedFields = recordAsHlist.map(toUpdateExpressions)

        nonIdTaggedFields.foldLeft[Option[UpdateExpression]](None)(combineUpdateExpressions)
      }
    }

  // Generates an UpdateExpressionGenerator for a case class
  implicit def productUpdateExpressionGenerator[C, T](
    implicit labelledGeneric: LabelledGeneric.Aux[C, T],
    updateExpressionGenerator: UpdateExpressionGenerator[T]) = create[C] {
    t: C =>
      updateExpressionGenerator.generateUpdateExpression(labelledGeneric.to(t))
  }

  object toUpdateExpressions extends Poly1 {
    implicit def some[K <: Symbol, V](implicit witness: Witness.Aux[K],
                                      dynamoFormat: DynamoFormat[V])
    : Case.Aux[FieldType[K, V], UpdateExpression] = {

      at[FieldType[K, V]] { fieldtype =>
      {
        val fieldValue: V = fieldtype
        set(witness.value -> fieldValue)
      }
      }
    }
  }

  object combineUpdateExpressions extends Poly2 {
    implicit def fold: Case.Aux[Option[UpdateExpression],
      UpdateExpression,
      Option[UpdateExpression]] =
      at[Option[UpdateExpression], UpdateExpression] {
        (maybeUpdateExpression, partUpdate) =>
          maybeUpdateExpression match {
            case Some(updateExpression) =>
              Some(updateExpression and partUpdate)
            case None => Some(partUpdate)
          }
      }
  }
}