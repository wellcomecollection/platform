{% extends "_base.scala" %}

{% block content %}
package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

trait License {
  val licenseType: String
  val label: String
  val url: String
  @JsonProperty("type") val ontologyType: String = "License"
}

{% for lic in licenses %}
case object License_{{ lic.licenseType|replace('-', '') }} extends License {
  val licenseType = "{{ lic.licenseType }}"
  val label = "{{ lic.label }}"
  val url = "{{ lic.url }}"
}
{% endfor %}{% endblock %}
