package uk.ac.wellcome.platform.ingestor.services

import com.sksamuel.elastic4s.analyzers.EnglishLanguageAnalyzer
import com.sksamuel.elastic4s.http.ElasticDsl.{booleanField, intField, keywordField, mapping, objectField, textField}
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import com.sksamuel.elastic4s.mappings.{FieldDefinition, MappingDefinition}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.ElasticSearchIndex
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.internal.{IdentifiedWork, IdentifierType, SourceIdentifier, Subject}
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.platform.ingestor.fixtures.WorkIndexerFixtures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class WorkIndexerTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with ElasticsearchFixtures
    with WorksUtil
    with WorkIndexerFixtures {

  val esType = "work"

  it("inserts an identified Work into Elasticsearch") {
    val work = createVersionedWork()

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWorks(List(work), indexName, esType)

        whenReady(future) { result =>
          result.right.get should contain (work)
          assertElasticsearchEventuallyHasWork(indexName = indexName, itemType = esType, work)
        }
      }
    }
  }

  it("only adds one record when the same ID is ingested multiple times") {
    val work = createVersionedWork()

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      withWorkIndexerFixtures(esType, elasticClient) {
        workIndexer =>
          val future = Future.sequence(
            (1 to 2).map(
              _ =>
                workIndexer.indexWorks(
                  works = List(work),
                  esIndex = indexName,
                  esType = esType
              ))
          )

          whenReady(future) { _ =>
            assertElasticsearchEventuallyHasWork(indexName = indexName, itemType = esType, work)
          }
      }
    }
  }

  it("doesn't add a Work with a lower version") {
    val work = createVersionedWork(version = 3)
    val olderWork = work.copy(version = 1)

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      insertIntoElasticsearch(indexName = indexName, itemType = esType, work)

      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWorks(
          works = List(olderWork),
          esIndex = indexName,
          esType = esType
        )

        whenReady(future) { result =>
          // Give Elasticsearch enough time to ingest the work
          Thread.sleep(700)
          result.right.get should contain (olderWork)

          assertElasticsearchEventuallyHasWork(indexName = indexName, itemType = esType, work)
        }
      }
    }
  }

  it("replaces a Work with the same version") {
    val work = createVersionedWork(version = 3)
    val updatedWork = work.copy(title = Some("boring title"))

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      insertIntoElasticsearch(indexName = indexName, itemType = esType, work)

      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWorks(
          works = List(updatedWork),
          esIndex = indexName,
          esType = esType
        )

        whenReady(future) { result =>
          result.right.get should contain (updatedWork)
          assertElasticsearchEventuallyHasWork(indexName = indexName, itemType = esType, updatedWork)
        }
      }
    }
  }

  it("inserts a list of works into elasticsearch and returns them"){
    val works = (1 to 5).map { i =>
      createVersionedWork().copy(canonicalId = s"$i-workid")
    }

    withLocalElasticsearchIndex(itemType = esType) { indexName =>
      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWorks(works, indexName, esType)

        whenReady(future) { successfullyInserted =>
          assertElasticsearchEventuallyHasWork(indexName = indexName, itemType = esType, works:_*)
          successfullyInserted.right.get should contain theSameElementsAs works
        }
      }
    }
  }

  it("inserts a list of works into elasticsearch and return the list of works that failed inserting"){
    val subsetOfFieldsIndex = new SubsetOfFieldsWorksIndex
    val validWorks = (1 to 5).map { i =>
      IdentifiedWork(
        canonicalId = s"s$i",
        sourceIdentifier = createIdentifier("sierra-system-number", "s1"),
        title = Some("s1 title"),
        version = 1)
    }
    val notMatchingMappingWork = IdentifiedWork(
      canonicalId = "not-matching",
      sourceIdentifier = createIdentifier("miro-image-number", "not-matching"),
      title = Some("title"),
      version = 1,
      subjects = List(Subject(label = "crystallography", concepts = Nil)))

    val works = validWorks :+ notMatchingMappingWork

    withLocalElasticsearchIndex(subsetOfFieldsIndex,indexName = (Random.alphanumeric take 10 mkString) toLowerCase) { indexName =>
      withWorkIndexerFixtures(esType, elasticClient) { workIndexer =>
        val future = workIndexer.indexWorks(works, indexName, esType)

        whenReady(future) { result =>
          assertElasticsearchEventuallyHasWork(indexName = indexName, itemType = esType, validWorks:_*)
          assertElasticsearchNeverHasWork(indexName = indexName, itemType = esType, notMatchingMappingWork)
          result.left.get should contain (notMatchingMappingWork)
        }
      }
    }
  }

  class SubsetOfFieldsWorksIndex extends ElasticSearchIndex {
    override val httpClient: HttpClient = elasticClient

    def sourceIdentifierFields = Seq(
      keywordField("ontologyType"),
      objectField("identifierType").fields(
        keywordField("id"),
        keywordField("label"),
        keywordField("ontologyType")
      ),
      keywordField("value")
    )

    val rootIndexFields: Seq[FieldDefinition with Product with Serializable] =
      Seq(
        keywordField("canonicalId"),
        intField("version"),
        objectField("sourceIdentifier")
          .fields(sourceIdentifierFields),
        textField("title").fields(
          textField("english").analyzer(EnglishLanguageAnalyzer)),
        booleanField("visible"),
        objectField("identifiers"),
        objectField("subjects"),
        keywordField("workType"),
        keywordField("description"),
        keywordField("physicalDescription"),
        keywordField("extent"),
        keywordField("lettering"),
        keywordField("createdDate"),
        keywordField("language"),
        keywordField("thumbnail"),
        keywordField("publicationDate"),
        keywordField("dimensions"),
        objectField("contributors"),
        objectField("genres"),
        objectField("items"),
        objectField("publishers"),
        objectField("placesOfPublication"),
        keywordField("ontologyType")
      )

    override val mappingDefinition: MappingDefinition = mapping(esType)
      .dynamic(DynamicMapping.Strict)
      .as(rootIndexFields)
  }

  private def createIdentifier(identifierType: String, value: String) = {
    SourceIdentifier(
      identifierType = IdentifierType(identifierType),
      ontologyType = "Work",
      value = value
    )
  }
  def createVersionedWork(version: Int = 1): IdentifiedWork =
    createWorks(count = 1).head
      .copy(version = version)
}
