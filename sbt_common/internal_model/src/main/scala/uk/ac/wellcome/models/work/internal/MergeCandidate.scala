package uk.ac.wellcome.models.work.internal

/** Indicates that it might be possible to merge this Work with another Work.
  *
  * @param identifier The SourceIdentifier of the other Work.
  * @param reason A statement of _why_ the we think it might be possible to
  *               to merge these two works.  For example, "MARC tag 776 points
  *               to electronic resource".
  *
  *               Long-term, this might be replaced with an enum or a fixed
  *               set of strings.
  */
case class MergeCandidate(
  identifier: SourceIdentifier,
  reason: Option[String] = None
)
