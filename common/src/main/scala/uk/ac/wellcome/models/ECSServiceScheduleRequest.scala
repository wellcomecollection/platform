package uk.ac.wellcome.models

case class ECSServiceScheduleRequest(cluster: String,
                                     service: String,
                                     desired_count: Long)
