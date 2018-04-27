package uk.ac.wellcome.work_model

case class Subject(label: String,
                   concepts: List[AbstractConcept],
                   ontologyType: String = "Subject")
