package uk.ac.wellcome.utils

import com.gu.scanamo.error.DynamoReadError

object ScanamoUtils {
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
