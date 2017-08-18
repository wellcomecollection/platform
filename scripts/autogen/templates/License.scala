package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

trait License {
  val licenseType: String
  val label: String
  val url: String
  @JsonProperty("type") val ontologyType: String = "License"
}

// ======================= AUTOGENERATED CODE STARTS ======================= //
//
// The code in this file is autogenerated.  You can rebuild it by running
// 'make autogen' from the root of the repo.  Edits to this file will be lost.
{% for lic in licenses %}
case object {{ lic.name }} extends License {
  val licenseType = "{{ lic.licenseType }}"
  val label = "{{ lic.label }}"
  val url = "{{ lic.url }}"
}
{% endfor %}
// ======================== AUTOGENERATED CODE ENDS ======================== //
