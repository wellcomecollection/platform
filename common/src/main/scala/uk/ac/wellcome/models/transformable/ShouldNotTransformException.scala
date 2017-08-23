package uk.ac.wellcome.models.transformable

case class FieldIssues(
  field: String,
  value: Any = "",
  message: Option[String] = None
)

case class ShouldNotTransformException(fieldIssues: List[FieldIssues])
    extends Exception(
      fieldIssues
        .map(issue => {
          val message = issue.message
            .map(msg => s" (${msg})")
            .getOrElse("")

          s"${issue.field}='${issue.value}'$message"
        })
        .mkString(",")
    )
