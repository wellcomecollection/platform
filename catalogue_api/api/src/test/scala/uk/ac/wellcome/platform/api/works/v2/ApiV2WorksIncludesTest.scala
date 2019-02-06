package uk.ac.wellcome.platform.api.works.v2

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.models.work.generators.{
  ProductionEventGenerators,
  SubjectGenerators
}
import uk.ac.wellcome.models.work.internal._

class ApiV2WorksIncludesTest
    extends ApiV2WorksTestBase
    with ProductionEventGenerators
    with SubjectGenerators {
  it(
    "includes a list of identifiers on a list endpoint if we pass ?include=identifiers") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val works = createIdentifiedWorks(count = 2).sortBy { _.canonicalId }

        val identifier0 = createSourceIdentifier
        val identifier1 = createSourceIdentifier

        val work0 = works(0).copy(otherIdentifiers = List(identifier0))
        val work1 = works(1).copy(otherIdentifiers = List(identifier1))

        insertIntoElasticsearch(indexV2, work0, work1)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?include=identifiers",
            andExpect = Status.Ok,
            withJsonBody =
              s"""
                              |{
                              |  ${resultList(apiPrefix, totalResults = 2)},
                              |  "results": [
                              |   {
                              |     "type": "Work",
                              |     "id": "${work0.canonicalId}",
                              |     "title": "${work0.title}",
                              |     "identifiers": [ ${identifier(
                   work0.sourceIdentifier)}, ${identifier(identifier0)} ]
                              |   },
                              |   {
                              |     "type": "Work",
                              |     "id": "${work1.canonicalId}",
                              |     "title": "${work1.title}",
                              |     "identifiers": [ ${identifier(
                   work1.sourceIdentifier)}, ${identifier(identifier1)} ]
                              |   }
                              |  ]
                              |}
          """.stripMargin
          )
        }
    }
  }

  it(
    "includes a list of identifiers on a single work endpoint if we pass ?include=identifiers") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val otherIdentifier = createSourceIdentifier
        val work = createIdentifiedWorkWith(
          otherIdentifiers = List(otherIdentifier)
        )
        insertIntoElasticsearch(indexV2, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?include=identifiers",
            andExpect = Status.Ok,
            withJsonBody =
              s"""
                              |{
                              | "@context": "https://localhost:8888/$apiPrefix/context.json",
                              | "type": "Work",
                              | "id": "${work.canonicalId}",
                              | "title": "${work.title}",
                              | "identifiers": [ ${identifier(
                   work.sourceIdentifier)}, ${identifier(otherIdentifier)} ]
                              |}
          """.stripMargin
          )
        }
    }
  }

  it("renders the items if the items include is present") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val work = createIdentifiedWorkWith(
          items = createIdentifiedItems(count = 1) :+ createUnidentifiableItemWith()
        )

        insertIntoElasticsearch(indexV2, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?include=items",
            andExpect = Status.Ok,
            withJsonBody = s"""
                              |{
                              | "@context": "https://localhost:8888/$apiPrefix/context.json",
                              | "type": "Work",
                              | "id": "${work.canonicalId}",
                              | "title": "${work.title}",
                              | "items": [ ${items(work.items)} ]
                              |}
          """.stripMargin
          )
        }
    }
  }

  it(
    "includes a list of subjects on a list endpoint if we pass ?include=subjects") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val works = createIdentifiedWorks(count = 2).sortBy { _.canonicalId }

        val subjects1 = List(createSubject)
        val subjects2 = List(createSubject)
        val work0 = works(0).copy(subjects = subjects1)
        val work1 = works(1).copy(subjects = subjects2)

        insertIntoElasticsearch(indexV2, work0, work1)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?include=subjects",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  ${resultList(apiPrefix, totalResults = 2)},
                 |  "results": [
                 |   {
                 |     "type": "Work",
                 |     "id": "${work0.canonicalId}",
                 |     "title": "${work0.title}",
                 |     "subjects": [ ${subjects(subjects1)}]
                 |   },
                 |   {
                 |     "type": "Work",
                 |     "id": "${work1.canonicalId}",
                 |     "title": "${work1.title}",
                 |     "subjects": [ ${subjects(subjects2)}]
                 |   }
                 |  ]
                 |}
          """.stripMargin
          )
        }
    }
  }

  it(
    "includes a list of subjects on a single work endpoint if we pass ?include=subjects") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val subject = List(createSubject)
        val work = createIdentifiedWork.copy(subjects = subject)

        insertIntoElasticsearch(indexV2, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?include=subjects",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                 |  "type": "Work",
                 |  "id": "${work.canonicalId}",
                 |  "title": "${work.title}",
                 |  "subjects": [ ${subjects(subject)}]
                 |}
          """.stripMargin
          )
        }
    }
  }

  it("includes a list of genres on a list endpoint if we pass ?include=genres") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val works = createIdentifiedWorks(count = 2).sortBy { _.canonicalId }

        val genres1 = List(
          Genre("ornithology", List(Unidentifiable(Concept("ornithology")))))
        val genres2 = List(
          Genre("flying cars", List(Unidentifiable(Concept("flying cars")))))
        val work0 = works(0).copy(genres = genres1)
        val work1 = works(1).copy(genres = genres2)

        insertIntoElasticsearch(indexV2, work0, work1)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?include=genres",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  ${resultList(apiPrefix, totalResults = 2)},
                 |  "results": [
                 |   {
                 |     "type": "Work",
                 |     "id": "${work0.canonicalId}",
                 |     "title": "${work0.title}",
                 |     "genres": [ ${genres(genres1)}]
                 |   },
                 |   {
                 |     "type": "Work",
                 |     "id": "${work1.canonicalId}",
                 |     "title": "${work1.title}",
                 |     "genres": [ ${genres(genres2)}]
                 |   }
                 |  ]
                 |}
          """.stripMargin
          )
        }
    }
  }

  it(
    "includes a list of genres on a single work endpoint if we pass ?include=genres") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val genre = List(
          Genre("ornithology", List(Unidentifiable(Concept("ornithology")))))
        val work = createIdentifiedWork.copy(genres = genre)

        insertIntoElasticsearch(indexV2, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?include=genres",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                 |  "type": "Work",
                 |  "id": "${work.canonicalId}",
                 |  "title": "${work.title}",
                 |  "genres": [ ${genres(genre)}]
                 |}
          """.stripMargin
          )
        }
    }
  }

  it(
    "includes a list of contributors on a list endpoint if we pass ?include=contributors") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val works = createIdentifiedWorks(count = 2).sortBy { _.canonicalId }

        val contributors1 =
          List(Contributor(Unidentifiable(Person("Ginger Rogers"))))
        val contributors2 =
          List(Contributor(Unidentifiable(Person("Fred Astair"))))
        val work0 = works(0).copy(contributors = contributors1)
        val work1 = works(1).copy(contributors = contributors2)

        insertIntoElasticsearch(indexV2, work0, work1)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?include=contributors",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  ${resultList(apiPrefix, totalResults = 2)},
                 |  "results": [
                 |   {
                 |     "type": "Work",
                 |     "id": "${work0.canonicalId}",
                 |     "title": "${work0.title}",
                 |     "contributors": [ ${contributors(contributors1)}]
                 |   },
                 |   {
                 |     "type": "Work",
                 |     "id": "${work1.canonicalId}",
                 |     "title": "${work1.title}",
                 |     "contributors": [ ${contributors(contributors2)}]
                 |   }
                 |  ]
                 |}
          """.stripMargin
          )
        }
    }
  }

  it(
    "includes a list of contributors on a single work endpoint if we pass ?include=contributors") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val contributor =
          List(Contributor(Unidentifiable(Person("Ginger Rogers"))))
        val work = createIdentifiedWork.copy(contributors = contributor)

        insertIntoElasticsearch(indexV2, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?include=contributors",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                 |  "type": "Work",
                 |  "id": "${work.canonicalId}",
                 |  "title": "${work.title}",
                 |  "contributors": [ ${contributors(contributor)}]
                 |}
          """.stripMargin
          )
        }
    }
  }

  it(
    "includes a list of production events on a list endpoint if we pass ?include=production") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val works = createIdentifiedWorks(count = 2).sortBy { _.canonicalId }

        val productionEvents1 = createProductionEventList(count = 1)
        val productionEvents2 = createProductionEventList(count = 2)
        val work0 = works(0).copy(production = productionEvents1)
        val work1 = works(1).copy(production = productionEvents2)

        insertIntoElasticsearch(indexV2, work0, work1)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works?include=production",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  ${resultList(apiPrefix, totalResults = 2)},
                 |  "results": [
                 |   {
                 |     "type": "Work",
                 |     "id": "${work0.canonicalId}",
                 |     "title": "${work0.title}",
                 |     "production": [ ${production(productionEvents1)}]
                 |   },
                 |   {
                 |     "type": "Work",
                 |     "id": "${work1.canonicalId}",
                 |     "title": "${work1.title}",
                 |     "production": [ ${production(productionEvents2)}]
                 |   }
                 |  ]
                 |}
          """.stripMargin
          )
        }
    }
  }

  it(
    "includes a list of production on a single work endpoint if we pass ?include=production") {
    withV2Api {
      case (indexV2, server: EmbeddedHttpServer) =>
        val productionEventList = createProductionEventList()
        val work = createIdentifiedWorkWith(
          production = productionEventList
        )

        insertIntoElasticsearch(indexV2, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?include=production",
            andExpect = Status.Ok,
            withJsonBody = s"""
                 |{
                 |  "@context": "https://localhost:8888/$apiPrefix/context.json",
                 |  "type": "Work",
                 |  "id": "${work.canonicalId}",
                 |  "title": "${work.title}",
                 |  "production": [ ${production(productionEventList)}]
                 |}
          """.stripMargin
          )
        }
    }
  }
}
