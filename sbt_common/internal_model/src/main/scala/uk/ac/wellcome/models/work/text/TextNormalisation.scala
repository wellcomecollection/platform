package uk.ac.wellcome.models.work.text

object TextNormalisation {
  def trimTrailing(s: String, c: Char) : String = {
    s.replaceAll(s"[\\s]*[$c][\\s]*$$", "")
  }
}
