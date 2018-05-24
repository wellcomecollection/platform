package uk.ac.wellcome.storage.type_classes

import shapeless.ops.hlist.{Align, Intersection}
import shapeless.{HList, LabelledGeneric}

// This is based on code from "The Type Astronaut's Guide to Shapeless",
// as downloaded on 23 May 2018.
//
// The code in question comes from § 6.3 "Case study: case class migrations".
// (It omits § 6.3.4 "Adding new fields", as we don't currently need that.)
//
// The purpose of Migration is to allow you to move between case classes.
// For example, suppose I had the following case classes:
//
//      case class Shapes(squares: Int, rectangles: Int, circles: Int)
//      case class Roundshapes(circles: Int)
//
// This allows us to migrate an instance of "Shapes" to "Roundshapes".
//
//      val shapes = Shapes(squares = 1, rectangles = 2, circles = 3)
//      val roundShapes = shapes.migrateTo[Roundshapes]
//        ~> Roundshapes(circles = 3)
//
// You can migrate from `Source` to `Target` if the fields in `Target` are
// a subset of the fields of `Source`.
//
// To pick up the `.migrateTo` method, import this object as follows:
//
//      import uk.ac.wellcome.storage.type_classes.Migration._
//

trait Migration[Source, Target] {
  def apply(src: Source): Target
}

object Migration {
  implicit class MigrationOps[Source](src: Source) {
    def migrateTo[Target](
      implicit migration: Migration[Source, Target]): Target =
      migration.apply(src)
  }

  // Type parameters:
  //
  // -  We are migrating from `SRepr` a HList to `Target` a case class.
  // -  `TRepr` is a HList representations of `Target`.
  // -  `Unaligned` is an intermedate HList used during the migration.
  //
  implicit def hlistMigration[SRepr <: HList,
                              Target,
                              TRepr <: HList,
                              Unaligned <: HList](
    implicit targetGen: LabelledGeneric.Aux[Target, TRepr],
    intersection: Intersection.Aux[SRepr, TRepr, Unaligned],
    align: Align[Unaligned, TRepr]
  ): Migration[SRepr, Target] = new Migration[SRepr, Target] {
    def apply(sourceHList: SRepr): Target = {
      // This gets the key-value pairs whose keys are in both `Source` and
      // `Target`, but they may not be in the correct order.
      val commonFields: Unaligned = intersection.apply(sourceHList)

      // This reorders the HList fields to be in the same order as `Target`.
      val targetHList: TRepr = align.apply(commonFields)

      targetGen.from(targetHList)
    }
  }

  // Type parameters:
  //
  // -  We are migrating from `Source` to `Target`, both case classes.
  // -  `SRepr` is a HList representations of `Source`.
  //
  // Implicit parameters:
  //
  // -  Conversion from `Source` to `SRepr` provided by `sourceGen`
  // -  Further conversion from `SRepr` to `Target` is provided by `hlistMigration`
  //
  implicit def genericMigration[Source, SRepr <: HList, Target](
    implicit sourceGen: LabelledGeneric.Aux[Source, SRepr],
    hlistMigration: Migration[SRepr, Target]
  ): Migration[Source, Target] = new Migration[Source, Target] {
    def apply(src: Source): Target = {
      val sourceHList: SRepr = sourceGen.to(src)

      sourceHList.migrateTo[Target]
    }
  }
}
