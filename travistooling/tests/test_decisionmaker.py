# -*- encoding: utf-8

import pytest

from travistooling.decisionmaker import (
    does_file_affect_build_task,
    should_run_build_task,
)
from travistooling.decisions import (
    ChangesToTestsDontGetPublished,
    CheckedByTravisFormat,
    CheckedByTravisLambda,
    ExclusivelyAffectsAnotherTask,
    ExclusivelyAffectsThisTask,
    IgnoredFileFormat,
    IgnoredPath,
    InsignificantFile,
    PythonChangeAndIsScalaApp,
    SignificantFile,
    UnrecognisedFile,
)


test_cases = [
    # Travis format task
    ("misc/myscript.py", "travis-format", CheckedByTravisFormat, True),
    ("ontologies/item.ttl", "travis-format", CheckedByTravisFormat, True),
    ("main.tf", "travis-format", CheckedByTravisFormat, True),
    ("example.json", "travis-format", CheckedByTravisFormat, True),
    ("README.md", "travis-format", IgnoredFileFormat, False),
    # Unrecognised filenames fall through to being significant, regardless
    # of the build job.
    ("foo.txt", "loris-build", UnrecognisedFile, True),
    ("foo.txt", "api-test", UnrecognisedFile, True),
    ("foo.txt", "snapshot_generator-publish", UnrecognisedFile, True),
    # Certain file formats are always excluded.
    ("foo.md", "ingestor-build", IgnoredFileFormat, False),
    ("image.png", "reindex_worker-test", IgnoredFileFormat, False),
    ("ontology.graffle", "nginx-test", IgnoredFileFormat, False),
    ("Makefile", "travistooling-test", IgnoredFileFormat, False),
    ("monitoring/Makefile", "travistooling-test", IgnoredFileFormat, False),
    ("formatting.Makefile", "travistooling-test", IgnoredFileFormat, False),
    ("my_lambda/requirements.in", "my_lambda-test", IgnoredFileFormat, False),
    ("data_science/data/.gitkeep", "ingestor-test", IgnoredFileFormat, False),
    ("data_science/.gitignore", "ingestor-test", IgnoredFileFormat, False),
    ("data_science/experiments.ipynb", "ingestor-test", IgnoredFileFormat, False),
    # Terraform files are significant, but only in the travis-format task
    ("s3.tf", "elasticdump-test", IgnoredFileFormat, False),
    # Certain paths are always insignificant.
    ("LICENSE", "travistooling-test", IgnoredPath, False),
    # Ontology/misc are significant, but only in the travis-format task
    ("misc/myscript.py", "sierra_reader-build", IgnoredPath, False),
    ("ontologies/work.ttl", "monitoring-publish", IgnoredPath, False),
    # At some point, we should really be building/publishing Loris in Travis
    # again, at which point these tests wake up.
    ("loris/loris/Dockerfile", "loris-build", UnrecognisedFile, True),
    ("loris/loris/Dockerfile", "ingestor-test", UnrecognisedFile, True),
    (
        "sierra_adapter/sierra_reader/foo.scala",
        "sierra_reader-publish",
        SignificantFile,
        True,
    ),
    ("catalogue_pipeline/ingestor/bar.scala", "api-test", InsignificantFile, False),
    ("run_autoformat.py", "travistooling-test", ExclusivelyAffectsAnotherTask, False),
    ("run_autoformat.py", "travis-format", CheckedByTravisFormat, True),
    # Anything in the sierra_adapter directory/common lib
    ("sierra_adapter/common/main.scala", "loris-test", UnrecognisedFile, True),
    (
        "sierra_adapter/common/main.scala",
        "s3_demultiplexer-test",
        UnrecognisedFile,
        True,
    ),
    (
        "sierra_adapter/common/main.scala",
        "sierra_window_generator-test",
        UnrecognisedFile,
        True,
    ),
    ("sierra_adapter/common/main.scala", "travistooling-test", UnrecognisedFile, True),
    ("sbt_common/elasticsearch/model.scala", "loris-test", UnrecognisedFile, True),
    ("sbt_common/display/model.scala", "loris-publish", UnrecognisedFile, True),
    ("sbt_common/display/model.scala", "travis-lambda-test", UnrecognisedFile, True),
    # Changes to the display models don't affect all of the stacks
    ("sbt_common/display/model.scala", "id_minter-test", InsignificantFile, False),
    ("sbt_common/display/model.scala", "reindex_worker-test", InsignificantFile, False),
    ("sbt_common/display/model.scala", "goobi_reader-test", InsignificantFile, False),
    ("sbt_common/display/model.scala", "sierra_reader-test", InsignificantFile, False),
    ("sbt_common/display/model.scala", "api-test", SignificantFile, True),
    # Changes to the elasticsearch lib don't affect all of the stacks
    (
        "sbt_common/elasticsearch/model.scala",
        "id_minter-test",
        InsignificantFile,
        False,
    ),
    (
        "sbt_common/elasticsearch/model.scala",
        "reindex_worker-test",
        InsignificantFile,
        False,
    ),
    (
        "sbt_common/elasticsearch/model.scala",
        "goobi_reader-test",
        InsignificantFile,
        False,
    ),
    (
        "sbt_common/elasticsearch/model.scala",
        "sierra_reader-test",
        InsignificantFile,
        False,
    ),
    ("sbt_common/elasticsearch/model.scala", "api-test", SignificantFile, True),
    # Changes to messaging-config don't affect the catalogue API but do affect
    # the pipeline apps.
    ("sbt_common/config/messaging/foo.scala", "api-test", InsignificantFile, False),
    ("sbt_common/config/messaging/foo.scala", "ingestor-test", SignificantFile, True),
    # Changes to storage common don't affect all the stacks
    ("storage/common/foo.scala", "api-test", InsignificantFile, False),
    ("storage/common/foo.scala", "notifier-test", SignificantFile, True),
    # Changes to Scala test files trigger a -test Scala task, but not
    # a -publish task.
    (
        "sbt_common/src/test/scala/uk/ac/wellcome/MyTest.scala",
        "sierra_adapter-publish",
        ChangesToTestsDontGetPublished,
        False,
    ),
    (
        "sbt_common/src/test/scala/uk/ac/wellcome/MyTest.scala",
        "sierra_adapter-test",
        UnrecognisedFile,
        True,
    ),
    (
        "catalogue_pipeline/ingestor/src/test/scala/uk/ac/wellcome/platform/ingestor/fixtures/WorkIndexerFixtures.scala",
        "ingestor-publish",
        ChangesToTestsDontGetPublished,
        False,
    ),
    # Changes to Python test files never trigger a -publish task.
    ("lambda_conftest.py", "loris-publish", ChangesToTestsDontGetPublished, False),
    (
        "lambda_conftest.py",
        "post_to_slack-publish",
        ChangesToTestsDontGetPublished,
        False,
    ),
    (
        "shared_conftest.py",
        "reindex_shard_generator-publish",
        ChangesToTestsDontGetPublished,
        False,
    ),
    (
        "reindex_worker/.coveragerc",
        "reindex_shard_generator-publish",
        ChangesToTestsDontGetPublished,
        False,
    ),
    # Changes to Lambdas trigger the travis-lambda-test task.
    (
        "reindexer/reindex_worker/src/reindex_worker.py",
        "travis-lambda-test",
        CheckedByTravisLambda,
        True,
    ),
    (
        "reindexer/reindex_worker/src/reindex_worker.py",
        "travis-lambda-publish",
        CheckedByTravisLambda,
        True,
    ),
    # Changes to travistooling only trigger the travistooling tests
    (
        "travistooling/decisionmaker.py",
        "travistooling-test",
        ExclusivelyAffectsThisTask,
        True,
    ),
    (
        "travistooling/decisionmaker.py",
        "loris-test",
        ExclusivelyAffectsAnotherTask,
        False,
    ),
    # Changes to the data_science/scripts folder only trigger the format tests
    ("data_science/scripts/foo.py", "loris-test", IgnoredPath, False),
    ("data_science/scripts/bar.py", "travis-format", CheckedByTravisFormat, True),
    # Scripts that are one deep in a stack only trigger the format tests
    ("reindexer/trigger_reindex.py", "travis-format", CheckedByTravisFormat, True),
    (
        "reindexer/trigger_reindex.py",
        "post_to_slack-publish",
        ExclusivelyAffectsAnotherTask,
        False,
    ),
    ("reindexer/trigger_reindex.py", "ingestor-test", PythonChangeAndIsScalaApp, False),
    # Chnages to Python files shouldn't trigger a Scala app
    ("shared_conftest.py", "ingestor-test", PythonChangeAndIsScalaApp, False),
    ("shared_conftest.py", "loris-test", UnrecognisedFile, True),
    # And now a grab bag of cases I've noticed actually happening in CI
    # where we ran tests we didn't need to, that I'll just keep adding as
    # regression tests.
    (
        "sbt_common/display/src/main/scala/uk/ac/wellcome/display/models/v1/DisplayConceptV1.scala",
        "bags_api-test",
        InsignificantFile,
        False,
    ),
    (
        "sbt_common/elasticsearch/src/main/scala/uk/ac/wellcome/elasticsearch/WorksIndex.scala",
        "ingests-test",
        InsignificantFile,
        False,
    ),
    (
        "sbt_common/internal_model/src/main/scala/uk/ac/wellcome/models/work/internal/Work.scala",
        "notifier-test",
        InsignificantFile,
        False,
    ),
    ("build.sbt", "ingestor-test", SignificantFile, True),
    ("project/Dependencies.scala", "ingestor-test", SignificantFile, True),
    ("builds/sbt_metadata/api.json", "goobi_reader-test", IgnoredPath, False),
    ("storage/bagger/src/tech_md.py", "bagger-publish", UnrecognisedFile, True),
]


@pytest.mark.parametrize("path, task, exc_class, is_significant", test_cases)
def test_does_file_affect_build_task(path, task, exc_class, is_significant):
    with pytest.raises(exc_class) as err:
        does_file_affect_build_task(path=path, task=task)
    assert err.value.is_significant == is_significant


def test_should_run_build_task_with_no_important_changes():
    result = should_run_build_task(changed_paths=[], task="loris-test")
    assert result == (False, {False: {}, True: {}})


def test_should_not_run_job_with_no_relevant_changes():
    result = should_run_build_task(
        changed_paths=["sierra_adapter/common/main.scala"], task="sierra_reader-test"
    )
    assert result == (
        True,
        {
            True: {SignificantFile.message: set(["sierra_adapter/common/main.scala"])},
            False: {},
        },
    )


def test_should_run_build_task_with_relevant_changes():
    result = should_run_build_task(
        changed_paths=["sierra_adapter/common/main.scala"], task="ingestor-test"
    )
    assert result == (
        False,
        {
            False: {
                InsignificantFile.message: set(["sierra_adapter/common/main.scala"])
            },
            True: {},
        },
    )
