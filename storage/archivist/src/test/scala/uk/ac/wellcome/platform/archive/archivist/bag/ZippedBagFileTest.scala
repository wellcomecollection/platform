package uk.ac.wellcome.platform.archive.archivist.bag

import java.util.zip.ZipFile

import org.scalatest.TryValues._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.archivist.fixtures.ZipBagItFixture
import uk.ac.wellcome.platform.archive.archivist.generators.FileGenerators

import scala.util.Success
import org.scalatest.prop.TableDrivenPropertyChecks._

class ZippedBagFileTest
    extends FunSpec
    with Matchers
    with ZipBagItFixture
    with FileGenerators {

  describe("locateBagInfo") {
    it("locates a bag-info file in a zipped bag") {
      withBagItZip() { zippedBag =>
        ZippedBagFile.locateBagInfo(new ZipFile(zippedBag)) shouldBe Success(
          "bag-info.txt")
      }
    }

    it("locates a valid bag-info file in a zip file") {
      val validBagInfoFiles = Table(
        "filename",
        "bag-info.txt",
        "bag/bag-info.txt",
        "/parent/bag/bag-info.txt",
        "//parent/bag/bag-info.txt")
      forAll(validBagInfoFiles) { bagInfofilename =>
        withZipFile(List(createFileEntry(bagInfofilename))) { zippedBag =>
          ZippedBagFile.locateBagInfo(new ZipFile(zippedBag)) shouldBe Success(
            bagInfofilename)
        }
      }
    }

    it("fails if there is no valid bag-info file in zip file") {
      val invalidBagInfoFiles = Table(
        "filename",
        "",
        "no-bag.txt",
        "Xbag-info.txt",
        "bag-info.txtX",
        "/bag-info.txtX",
        "/bag-info.txt/X",
        "X/Xbag-info.txt")
      forAll(invalidBagInfoFiles) { invalidBagInfoFilename =>
        withZipFile(List(createFileEntry(invalidBagInfoFilename))) {
          zippedBag =>
            ZippedBagFile
              .locateBagInfo(new ZipFile(zippedBag))
              .failure
              .exception
              .getMessage shouldBe
              "'bag-info.txt' not found."
        }
      }
    }

    it("fails if there is more than one bag") {
      withZipFile(
        List(
          createFileEntry("bag-info.txt"),
          createFileEntry("bag/bag-info.txt"))) { zippedBag =>
        ZippedBagFile
          .locateBagInfo(new ZipFile(zippedBag))
          .failure
          .exception
          .getMessage shouldBe
          "Expected only one 'bag-info.txt' found List(bag-info.txt, bag/bag-info.txt)."
      }
    }
  }

  describe("bagPathFromBagInfoPath") {
    it("returns None if there is no enclosing bag directory") {
      ZippedBagFile.bagPathFromBagInfoPath("bag-info.txt") shouldBe None
    }

    it("finds the bag path when in a parent directory") {
      ZippedBagFile.bagPathFromBagInfoPath("bag/bag-info.txt") shouldBe Some(
        "bag/")
    }

    it("ignores bag-info.txt not at the end") {
      ZippedBagFile.bagPathFromBagInfoPath("bag-info.txt/bag/bag-info.txt") shouldBe Some(
        "bag-info.txt/bag/")
    }

    it("preserves preceding slashes") {
      ZippedBagFile.bagPathFromBagInfoPath("/bag/bag-info.txt") shouldBe Some(
        "/bag/")
    }

    it("preserves preceding slashes even if bag-info.txt is at the root") {
      ZippedBagFile.bagPathFromBagInfoPath("/bag-info.txt") shouldBe Some("/")
    }
  }
}
