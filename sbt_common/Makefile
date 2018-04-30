ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

sbt-internal_model-test:
	$(call sbt_test_no_docker,internal_model)

sbt-internal_model-publish:
	echo "Nothing to do!"

sbt-display-test:
	$(call sbt_test_no_docker,common_display)

sbt-display-publish:
	echo "Nothing to do!"


sbt-elasticsearch-test:
	$(call sbt_test,common_elasticsearch)

sbt-elasticsearch-publish:
	echo "Nothing to do!"

sbt-elasticsearch-docker_compose_up:
	$(call docker_compose_up,sbt_common/elasticsearch/docker-compose.yml)

sbt-elasticsearch-docker_compose_down:
	$(call docker_compose_down,sbt_common/elasticsearch/docker-compose.yml)
