package uk.ac.wellcome.platform.api.models

import org.scalatest.FunSpec

class ElasticsearchQueryOptionsTest extends FunSpec {
  it("rejects creating an instance without a queryString or sortByField") {
    intercept[IllegalArgumentException] {
      ElasticsearchQueryOptions(
        queryString = None,
        sortByField = None,
        indexName = "myIndex"
      )
    }
  }

  it("rejects creating an instance with both queryString or sortByField") {
    intercept[IllegalArgumentException] {
      ElasticsearchQueryOptions(
        queryString = Some("my query string"),
        sortByField = Some("sort by this field"),
        indexName = "myIndex"
      )
    }
  }
}
