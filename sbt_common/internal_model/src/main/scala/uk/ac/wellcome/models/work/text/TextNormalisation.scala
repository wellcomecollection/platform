package uk.ac.wellcome.models.work.text

import scala.util.matching.Regex

object TextNormalisation {
  def trimTrailing(s: String, c: Char): String = {
    // remove the given character and surrounding whitespace from the end of the String
    // regexp = <any whitespace>c<any whitespace><end> = "\s*[c]\s*$"
    val regexp = """\s*[""" + Regex.quote(c.toString) + """]\s*$"""
    s.replaceAll(regexp, "")
  }

  def sentenceCase(s: String): String =
    s.capitalize
}
