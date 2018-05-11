package uk.ac.wellcome.elasticsearch

case class ElasticClientConfig(hostname: String,
                               hostPort: Int,
                               hostProtocol: String,
                               elasticUsername: String,
                               elasticPassword: String
                              )
