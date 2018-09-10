package uk.ac.wellcome.platform.merger.rules.digitalphysical

import org.scalatest.FunSpec
import uk.ac.wellcome.platform.merger.MergerTestUtils

class SierraPhysicalDigitalPartitionerTest extends FunSpec with MergerTestUtils {

  val partitioner = new SierraPhysicalDigitalPartitioner {}

  describe("filters physical and digital works") {
    it("extracts a physical and digital work") {
      val physicalWork = createPhysicalSierraWork
      val digitalWork = createDigitalSierraWork

      val result = partitioner.partitionWorks(Seq(physicalWork, digitalWork))

      result shouldBe Some(partitioner.Partition(physicalWork, digitalWork, Nil))
    }

    it("extracts a physical, digital and other works") {
      val physicalWork = createPhysicalSierraWork
      val digitalWork = createDigitalSierraWork
      val otherWorks = createUnidentifiedWorks(4)

      val result = partitioner.partitionWorks(Seq(physicalWork, digitalWork) ++ otherWorks)

      result shouldBe Some(partitioner.Partition(physicalWork, digitalWork, otherWorks))
    }

    it("ignores a single physical work") {
      val works = Seq(createPhysicalSierraWork)

      val result = partitioner.partitionWorks(works)

      result shouldBe None
    }

    it("ignores a single digital work") {
      val works = Seq(createDigitalSierraWork)

      val result = partitioner.partitionWorks(works)

      result shouldBe None
    }

    it("ignores multiple physical works") {
      val works = (1 to 3).map { _ =>
        createPhysicalSierraWork
      }

      val result = partitioner.partitionWorks(works)

      result shouldBe None
    }

    it("ignores multiple digital works") {
      val works = (1 to 3).map { _ =>
        createDigitalSierraWork
      }
      val result = partitioner.partitionWorks(works)

      result shouldBe None
    }

    it("ignores multiple physical works with a single digital work") {
      val works = (1 to 3).map { _ =>
        createPhysicalSierraWork
      } ++ Seq(createDigitalSierraWork)

      val result = partitioner.partitionWorks(works)

      result shouldBe None
    }

    it("ignores multiple digital works with a single physical work") {
      val works = (1 to 3).map { _ =>
        createDigitalSierraWork
      } ++ Seq(createPhysicalSierraWork)

      val result = partitioner.partitionWorks(works)

      result shouldBe None
    }
  }

}
