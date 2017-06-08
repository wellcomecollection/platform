package uk.ac.wellcome.utils

import java.util

import cats.free.Free
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryResult}
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.ops.{ScanamoOps, ScanamoOpsA}
import com.gu.scanamo.request.ScanamoQueryRequest
import com.gu.scanamo.{DynamoFormat, ScanamoFree}
import com.twitter.inject.Logging
import scala.collection.JavaConverters._

object ScanamoUtils extends Logging {
  def logAndFilterLeft[Y](rows: List[Either[DynamoReadError, Y]]) = {
    rows.foreach {
      case Left(e: DynamoReadError) => error(e.toString)
      case _ => Unit
    }

    rows
      .filter {
        case Right(_) => true
        case Left(_) => false
      }
      .flatMap(_.right.toOption)
  }
}

// This code is mostly cribbed from https://github.com/guardian/scanamo/blob/v0.9.4/src/main/scala/com/gu/scanamo/DynamoResultStream.scala#L13
// We need access to the results in batches to prevent OOM while processing large results sets, so the source is modified to allow this.
object ScanamoQueryStream {
  type EvaluationKey = java.util.Map[String, AttributeValue]

  private def items(
    res: QueryResult): util.List[util.Map[String, AttributeValue]] =
    res.getItems
  private def lastEvaluatedKey(
    res: QueryResult): util.Map[String, AttributeValue] =
    res.getLastEvaluatedKey
  private def withExclusiveStartKey(
    req: ScanamoQueryRequest,
    key: util.Map[String, AttributeValue]): ScanamoQueryRequest =
    req.copy(
      options = req.options.copy(exclusiveStartKey = Some(key.asScala.toMap)))

  private def exec(req: ScanamoQueryRequest): ScanamoOps[QueryResult] =
    ScanamoOps.query(req)

  def run[T: DynamoFormat, Y](
    req: ScanamoQueryRequest,
    f: (List[Either[DynamoReadError, T]]) => List[Y]): ScanamoOps[List[Y]] = {
    def runMore(lastKey: Option[EvaluationKey]): ScanamoOps[List[Y]] = {
      for {
        queryResult <- exec(lastKey.foldLeft(req)(withExclusiveStartKey(_, _)))
        results = items(queryResult).asScala.map(ScanamoFree.read[T]).toList
        // This is where we hook into the batch of results
        processedResults = f(results)
        resultList <- Option(lastEvaluatedKey(queryResult)).foldLeft(
          Free.pure[ScanamoOpsA, List[Y]](processedResults)
        )((rs, k) =>
          for {
            items <- rs
            more <- runMore(Some(k))
          } yield items ::: more)
      } yield resultList
    }
    runMore(None)
  }
}
