package uk.ac.wellcome.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.query.{KeyEquals, UniqueKey}
import com.gu.scanamo.syntax.{attributeExists, not, _}
import com.gu.scanamo.update.UpdateExpression
import com.gu.scanamo.{DynamoFormat, Scanamo, Table}
import com.twitter.inject.Logging
import shapeless.{HList, LabelledGeneric, Poly1, Witness}
import shapeless.labelled.FieldType
import uk.ac.wellcome.models._
import shapeless._
import shapeless.ops.hlist.{LeftFolder, Mapper}
import shapeless.ops.record.Remover
import shapeless.ops.tuple.ToTraversable
import shapeless.record._
import uk.ac.wellcome.dynamo.UpdateExpressionGenerator.toUpdateExpressions
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.type_classes.IdGetter.{createIdGetter, IdKey}
import uk.ac.wellcome.type_classes.{IdGetter, VersionGetter, VersionUpdater}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.collection.generic.CanBuildFrom
import scala.concurrent.Future

trait UpdateExpressionGenerator[T] {
  def generateUpdateExpression(t: T): Option[UpdateExpression]
}

object UpdateExpressionGenerator {
  val w = Witness(Symbol("id"))
  type IdKey = w.T
  type IdField = FieldType[IdKey, String]

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

  object Folder extends Poly2 {
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

  def apply[T](implicit generator: UpdateExpressionGenerator[T]) = generator

  def create[T](f: (T) => Option[UpdateExpression]) =
    new UpdateExpressionGenerator[T] {
      override def generateUpdateExpression(record: T) = f(record)
    }

  implicit def elementUpdateExpressionGenerator[L <: HList,
                                                A,
                                                B <: HList,
                                                C <: HList](
    implicit remover: Remover.Aux[L, IdKey, (A, B)],
    mapper: Mapper.Aux[toUpdateExpressions.type, B, C],
    folder: LeftFolder.Aux[C,
                           Option[UpdateExpression],
                           Folder.type,
                           Option[UpdateExpression]]) =
    create { t: L =>
      {
        val recordAsHlist = t - 'id
        val nonIdTaggedFields = recordAsHlist.map(toUpdateExpressions)

        nonIdTaggedFields.foldLeft[Option[UpdateExpression]](None)(Folder)
      }
    }

  implicit def productUpdateExpressionGenerator[C, T](
    implicit labelledGeneric: LabelledGeneric.Aux[C, T],
    updateExpressionGenerator: UpdateExpressionGenerator[T]) = create[C] {
    t: C =>
      updateExpressionGenerator.generateUpdateExpression(labelledGeneric.to(t))
  }
}

class VersionedDao @Inject()(
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
) extends Logging {

  private def updateBuilder[T](record: T)(
    implicit evidence: DynamoFormat[T],
    versionUpdater: VersionUpdater[T],
    versionGetter: VersionGetter[T],
    idGetter: IdGetter[T],
    updateExpressionGenerator: UpdateExpressionGenerator[T]
  ) = {
    val version = versionGetter.version(record)
    val newVersion = version + 1

    val updatedRecord = versionUpdater.updateVersion(record, newVersion)

    updateExpressionGenerator.generateUpdateExpression(updatedRecord).map {
      updateExpression =>
        Table[T](dynamoConfig.table)
          .given(
            not(attributeExists('id)) or
              (attributeExists('id) and 'version < newVersion)
          )
          .update(
            UniqueKey(KeyEquals('id, idGetter.id(record))),
            updateExpression
          )
    }
  }

  def updateRecord[T](record: T)(
    implicit evidence: DynamoFormat[T],
    versionUpdater: VersionUpdater[T],
    idGetter: IdGetter[T],
    versionGetter: VersionGetter[T],
    updateExpressionGenerator: UpdateExpressionGenerator[T]
  ): Future[Unit] = Future {
    val id = idGetter.id(record)
    info(s"Attempting to update Dynamo record: $id")

    updateBuilder(record).map { ops =>
      Scanamo.exec(dynamoDbClient)(ops) match {
        case Left(scanamoError) => {
          val exception = new RuntimeException(scanamoError.toString)

          warn(s"Failed to updating Dynamo record: $id", exception)

          throw exception
        }
        case Right(_) => {
          info(s"Successfully updated Dynamo record: $id")
        }
      }
    }
  }

  def getRecord[T](id: String)(
    implicit evidence: DynamoFormat[T]): Future[Option[T]] = Future {
    val table = Table[T](dynamoConfig.table)

    info(s"Attempting to retrieve Dynamo record: $id")
    Scanamo.exec(dynamoDbClient)(table.get('id -> id)) match {
      case Some(Right(record)) => {
        info(s"Successfully retrieved Dynamo record: $id")

        Some(record)
      }
      case Some(Left(scanamoError)) =>
        val exception = new RuntimeException(scanamoError.toString)

        error(
          s"An error occurred while retrieving $id from DynamoDB",
          exception
        )

        throw exception
      case None => {
        info(s"No Dynamo record found for id: $id")

        None
      }
    }
  }
}
