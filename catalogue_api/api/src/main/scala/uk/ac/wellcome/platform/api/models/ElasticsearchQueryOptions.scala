package uk.ac.wellcome.platform.api.models

case class ElasticsearchQueryOptions(
  queryString: Option[String] = None,
  sortByField: Option[String] = None,
  workType: Option[String] = None,
  indexName: String,
  limit: Int = 10,
  from: Int = 0
) {
  require(
    queryString.isDefined || sortByField.isDefined,
    "One of queryString and sortByField should be defined"
  )

  require(
    queryString.isEmpty || sortByField.isEmpty,
    "Exactly one of queryString and sortByField should be defined"
  )
}
