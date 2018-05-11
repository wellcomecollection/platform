package uk.ac.wellcome.elasticsearch

case class ElasticConfig(
  hostname: String,
  hostPort: Int,
  hostProtocol: String,
  username: String,
  password: String
)
