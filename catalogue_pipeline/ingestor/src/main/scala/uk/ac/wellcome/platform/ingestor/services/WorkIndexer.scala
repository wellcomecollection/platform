package uk.ac.wellcome.platform.ingestor.services

import com.sksamuel.elastic4s.{Index, Indexable}
import com.sksamuel.elastic4s.VersionType.ExternalGte
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.bulk.{BulkResponse, BulkResponseItem}
import com.sksamuel.elastic4s.http.{ElasticClient, Response}
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.{
  IdentifiedBaseWork,
  IdentifiedInvisibleWork,
  IdentifiedRedirectedWork,
  IdentifiedWork
}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

class WorkIndexer(
  elasticClient: ElasticClient
)(implicit ec: ExecutionContext)
    extends Logging {

  implicit object IdentifiedWorkIndexable
      extends Indexable[IdentifiedBaseWork] {
    override def json(t: IdentifiedBaseWork): String =
      toJson(t).get
  }

  def indexWorks(works: Seq[IdentifiedBaseWork], index: Index)
    : Future[Either[Seq[IdentifiedBaseWork], Seq[IdentifiedBaseWork]]] = {

    debug(s"Indexing work ${works.map(_.canonicalId).mkString(", ")}")

    val inserts = works.map { work =>
      // Elasticsearch are removing types entirely in ES 7, and creating an index
      // with more than one type in ES 6 is a 400 Error.
      //
      // Our prod cluster is already creating a single "type" with the same name
      // as the index, so do the same here.
      indexInto(index.name / index.name)
        .version(calculateEsVersion(work))
        .versionType(ExternalGte)
        .id(work.canonicalId)
        .doc(work)
    }

    elasticClient
      .execute {
        bulk(inserts)
      }
      .map { response: Response[BulkResponse] =>
        if (response.isError) {
          warn(s"Error from Elasticsearch: $response")
          Left(works)
        } else {
          debug(s"Bulk response = $response")
          val bulkResponse = response.result
          val actualFailures = bulkResponse.failures.filterNot {
            isVersionConflictException
          }

          if (actualFailures.nonEmpty) {
            val failedIds = actualFailures.map(_.id)
            debug(s"Failed indexing works $failedIds")

            Left(works.filter(w => {
              failedIds.contains(w.canonicalId)
            }))
          } else Right(works)
        }
      }
  }

  /**
    * When the merger makes the decision to merge some works, it modifies the content of
    * the affected works. Despite the content of these works being modified, their version
    * remains the same. Instead, the merger sets the merged flag to true for the target work
    * and changes the type of the other works to redirected.
    *
    * When we ingest those works, we need to make sure that the merger modified works
    * are never overridden by unmerged works for the same version still running through
    * the pipeline.
    * We also need to make sure that, if a work is modified by a source in such a way that
    * it shouldn't be merged (or redirected) anymore, the new unmerged version is ingested
    * and never replaced by the previous merged version still running through the pipeline.
    *
    * We can do that by ingesting works into Elasticsearch with a version derived by a
    * combination of work version, merged flag and work type. More specifically, by
    * multiplying the work version by 10, we make sure that a new version of a work
    * always wins over previous versions (merged or unmerged).
    * We make sure that a merger modified work always wins over other works with the same
    * version, by adding one to work.version * 10.
    */
  private def calculateEsVersion(work: IdentifiedBaseWork): Int = work match {
    case w: IdentifiedWork           => (w.version * 10) + w.merged
    case w: IdentifiedRedirectedWork => (w.version * 10) + 1
    case w: IdentifiedInvisibleWork  => w.version * 10
  }

  /** Did we try to PUT a document with a lower version than the existing version?
    *
    */
  private def isVersionConflictException(
    bulkResponseItem: BulkResponseItem): Boolean = {
    // This error is returned by Elasticsearch when we try to PUT a document
    // with a lower version than the existing version.
    val alreadyIndexedWorkHasHigherVersion = bulkResponseItem.error
      .exists(bulkError =>
        bulkError.`type`.contains("version_conflict_engine_exception"))

    if (alreadyIndexedWorkHasHigherVersion) {
      info(
        s"Skipping ${bulkResponseItem.id} because already indexed work has a higher version (${bulkResponseItem.error}")
    }

    alreadyIndexedWorkHasHigherVersion
  }

  implicit private def toInteger(bool: Boolean): Int = if (bool) 1 else 0
}
