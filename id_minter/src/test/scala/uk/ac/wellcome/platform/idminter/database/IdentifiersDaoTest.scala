package uk.ac.wellcome.platform.idminter.database

import java.sql.SQLIntegrityConstraintViolationException

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import scalikejdbc._
import uk.ac.wellcome.platform.idminter.model.Identifier
import uk.ac.wellcome.platform.idminter.utils.MysqlLocal

class IdentifiersDaoTest
    extends FunSpec
    with MysqlLocal
    with ScalaFutures
    with Matchers {

  val identifiersDao = new IdentifiersDao(DB.connect(), identifiersTable)

  describe("findMiroIdInDb") {

    it("should return a future of some if the requested miroId is in the database") {
      val miroId = "1234"
      val canonicalId = "5678"
      withSQL {
        insert
          .into(identifiersTable)
          .namedValues(identifiersTable.column.CanonicalID -> canonicalId,
                       identifiersTable.column.MiroID -> miroId)
      }.update().apply()

      whenReady(identifiersDao.findMiroIdInDb(miroId)) { maybeIdentifier =>
        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe Identifier(canonicalId, miroId)
      }
    }

    it("should return a future of none if the requested miroId is not in the database") {
      whenReady(identifiersDao.findMiroIdInDb("abcdef")) { maybeIdentifier =>
        maybeIdentifier shouldNot be(defined)
      }
    }
  }

  describe("saveCanonicalId") {

    it("should insert the provided identifier into the database") {
      val identifier = Identifier(CanonicalID = "5678", MiroID = "1234")
      val future = identifiersDao.saveCanonicalId(identifier)

      whenReady(future) { _ =>
        val maybeIdentifier = withSQL {
          select
            .from(identifiersTable as identifiersTable.i)
            .where
            .eq(identifiersTable.i.MiroID, identifier.MiroID)
            .and
            .eq(identifiersTable.i.CanonicalID, identifier.CanonicalID)
        }.map(Identifier(identifiersTable.i)).single.apply()

        maybeIdentifier shouldBe defined
        maybeIdentifier.get shouldBe identifier
      }
    }

    it("should fail inserting a record if there is already a record for the same miroId") {
      val identifier = new Identifier(CanonicalID = "5678", MiroID = "1234")
      withSQL {
        insert
          .into(identifiersTable)
          .namedValues(identifiersTable.column.CanonicalID -> identifier.CanonicalID,
            identifiersTable.column.MiroID -> identifier.MiroID)
      }.update().apply()

      val saveCanonicalId = identifiersDao.saveCanonicalId(identifier.copy(CanonicalID = "0987"))
      whenReady(saveCanonicalId.failed){ exception =>
        exception shouldBe a[SQLIntegrityConstraintViolationException]
      }
    }
  }
}
