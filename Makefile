include functions.Makefile

include formatting.Makefile

include assets/Makefile
include loris/Makefile
include shared_infra/Makefile
include catalogue_api/Makefile
include catalogue_pipeline/Makefile
include monitoring/Makefile
include ontologies/Makefile
include reindexer/Makefile
include sierra_adapter/Makefile
include nginx/Makefile


sbt-common-test:
	$(call sbt_test,common)

sbt-common-publish:
	echo "Nothing to do!"
