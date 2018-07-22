package uk.ac.wellcome.platform.ingestor.services

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.Indexable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.bulk.{BulkResponse, BulkResponseItem}
import com.twitter.inject.Logging
import org.elasticsearch.index.VersionType
import uk.ac.wellcome.elasticsearch.ElasticsearchExceptionManager
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WorkIndexer @Inject()(
  elasticClient: HttpClient
)(implicit ec: ExecutionContext)
    extends Logging
    with ElasticsearchExceptionManager {

  implicit object IdentifiedWorkIndexable
      extends Indexable[IdentifiedBaseWork] {
    override def json(t: IdentifiedBaseWork): String =
      toJson(t).get
  }

  def indexWorks(works: Seq[IdentifiedBaseWork],
                 esIndex: String,
                 esType: String)
    : Future[Either[Seq[IdentifiedBaseWork], Seq[IdentifiedBaseWork]]] = {

    debug(s"Indexing work ${works.map(_.canonicalId).mkString(", ")}")

    val inserts = works.map { work =>
      indexInto(esIndex / esType)
        .version(work.version)
        .versionType(VersionType.EXTERNAL_GTE)
        .id(work.canonicalId)
        .doc(work)
    }

    elasticClient
      .execute {
        bulk(inserts)
      }
      .map { bulkResponse: BulkResponse =>
        val actualFailures = filterVersionConflictErrors(bulkResponse)

        if (actualFailures.nonEmpty) {
          val failedIds = actualFailures.map(_.id)
          debug(s"Failed indexing works $failedIds")

          Left(works.filter(w => {
            failedIds.contains(w.canonicalId)
          }))
        } else Right(works)
      }
  }

  private def filterVersionConflictErrors(bulkResponse: BulkResponse): Seq[BulkResponseItem] = {
    bulkResponse.failures.filterNot { bulkResponseItem =>

      // This error is returned by Elasticsearch when we try to PUT a document
      // with a lower version than the existing version.
      val alreadyIndexedWorkHasHigherVersion = bulkResponseItem
        .error
        .exists(bulkError => bulkError.`type`.contains("version_conflict_engine_exception"))

      if (alreadyIndexedWorkHasHigherVersion) {
        info(s"Skipping ${bulkResponseItem.id} because already indexed work has a higher version (${bulkResponseItem.error}")
      }

      alreadyIndexedWorkHasHigherVersion
    }
  }
}
