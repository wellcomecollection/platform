{% extends "_base.scala" %}

{% block content %}
package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

trait LicenseTrait {
  val licenseType: String
  val label: String
  val url: String
  @JsonProperty("type") val ontologyType: String = "License"
}

case class License (
  val licenseType: String,
  val label: String,
  val url: String
) extends LicenseTrait
{% for lic in data %}
case object License_{{ lic.licenseType|replace('-', '') }} extends LicenseTrait {
  val licenseType = "{{ lic.licenseType }}"
  val label = "{{ lic.label }}"
  val url = "{{ lic.url }}"
}
{% endfor %}{% endblock %}
