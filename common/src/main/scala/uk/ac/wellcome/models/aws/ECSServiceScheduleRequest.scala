package uk.ac.wellcome.models.aws

case class ECSServiceScheduleRequest(cluster: String,
                                     service: String,
                                     desired_count: Long)
