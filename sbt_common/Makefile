ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

sbt-display-test:
	$(call sbt_test_no_docker,common_display)

sbt-display-publish:
	echo "Nothing to do!"

sbt-elasticsearch-test:
	$(call sbt_test_no_docker,common_elasticsearch)

sbt-elasticsearch-publish:
	echo "Nothing to do!"
