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

.scripts:
	mkdir $(ROOT)/.scripts

# Get docker_run.py script
.scripts/docker_run.py: .scripts
	wget https://raw.githubusercontent.com/wellcometrust/docker_run/master/docker_run.py -P .scripts
	chmod u+x .scripts/docker_run.py

# Get the build scripts required
build_setup: \
	.scripts/docker_run.py

sbt-common-test: build_setup
	$(call sbt_test,common)

sbt-common-publish:
	echo "Nothing to do!"
