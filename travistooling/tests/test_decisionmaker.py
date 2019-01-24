# -*- encoding: utf-8

import pytest

from travistooling.decisionmaker import (
    does_file_affect_build_task,
    should_run_build_task,
)
from travistooling.decisions import (
    ChangesToTestsDontGetPublished,
    ChangeToDependency,
    ChangeToUnusedLibrary,
    CheckedByTravisFormat,
    CheckedByTravisLambda,
    ExclusivelyAffectsAnotherTask,
    ExclusivelyAffectsThisTask,
    IgnoredFileFormat,
    IgnoredPath,
    PythonChangeAndIsScalaApp,
    ScalaChangeAndIsScalaApp,
    ScalaChangeAndNotScalaApp,
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
    # Changes that belong exclusively to a single task
    ("loris/loris/Dockerfile", "loris-build", ExclusivelyAffectsThisTask, True),
    (
        "loris/loris/Dockerfile",
        "ingestor-test",
        ExclusivelyAffectsAnotherTask,
        False,
    ),
    (
        "sierra_adapter/sierra_reader/foo.scala",
        "sierra_reader-publish",
        ExclusivelyAffectsThisTask,
        True,
    ),
    (
        "catalogue_pipeline/ingestor/bar.scala",
        "api-test",
        ExclusivelyAffectsAnotherTask,
        False,
    ),
    (
        "run_autoformat.py",
        "travistooling-test",
        ExclusivelyAffectsAnotherTask,
        False,
    ),
    ("run_autoformat.py", "travis-format", CheckedByTravisFormat, True),
    # Anything in the sierra_adapter directory/common lib
    (
        "sierra_adapter/common/main.scala",
        "loris-test",
        ScalaChangeAndNotScalaApp,
        False,
    ),
    (
        "sierra_adapter/common/main.scala",
        "s3_demultiplexer-test",
        ScalaChangeAndNotScalaApp,
        False,
    ),
    (
        "sierra_adapter/common/main.scala",
        "sierra_window_generator-test",
        ScalaChangeAndNotScalaApp,
        False,
    ),
    (
        "sierra_adapter/common/main.scala",
        "travistooling-test",
        ScalaChangeAndNotScalaApp,
        False,
    ),
    (
        "sbt_common/elasticsearch/model.scala",
        "loris-test",
        ScalaChangeAndNotScalaApp,
        False,
    ),
    (
        "sbt_common/display/model.scala",
        "loris-publish",
        ScalaChangeAndNotScalaApp,
        False,
    ),
    (
        "sbt_common/display/model.scala",
        "travis-lambda-test",
        ScalaChangeAndNotScalaApp,
        False,
    ),
    # Changes to the display models don't affect all of the stacks
    (
        "sbt_common/display/model.scala",
        "id_minter-test",
        ChangeToUnusedLibrary,
        False,
    ),
    (
        "sbt_common/display/model.scala",
        "reindex_worker-test",
        ChangeToUnusedLibrary,
        False,
    ),
    (
        "sbt_common/display/model.scala",
        "goobi_reader-test",
        ChangeToUnusedLibrary,
        False,
    ),
    (
        "sbt_common/display/model.scala",
        "sierra_reader-test",
        ChangeToUnusedLibrary,
        False,
    ),
    ("sbt_common/display/model.scala", "api-test", ScalaChangeAndIsScalaApp, True),
    # Changes to the elasticsearch lib don't affect all of the stacks
    (
        "sbt_common/elasticsearch/model.scala",
        "id_minter-test",
        ChangeToUnusedLibrary,
        False,
    ),
    (
        "sbt_common/elasticsearch/model.scala",
        "reindex_worker-test",
        ChangeToUnusedLibrary,
        False,
    ),
    (
        "sbt_common/elasticsearch/model.scala",
        "goobi_reader-test",
        ChangeToUnusedLibrary,
        False,
    ),
    (
        "sbt_common/elasticsearch/model.scala",
        "sierra_reader-test",
        ChangeToUnusedLibrary,
        False,
    ),
    (
        "sbt_common/elasticsearch/model.scala",
        "api-test",
        ScalaChangeAndIsScalaApp,
        True,
    ),

    # Changes to messaging-config don't affect the catalogue API but do affect
    # the pipeline apps.
    ("sbt_common/config/messaging", "api-test", ChangeToUnusedLibrary, False),
    ("sbt_common/config/messaging", "ingestor-test", ChangeToDependency, True),

    # Changes to storage common don't affect all the stacks
    ("storage/common/foo.scala", "api-test", ChangeToUnusedLibrary, False),
    ("storage/common/foo.scala", "notifier-test", ScalaChangeAndIsScalaApp, True),

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
    (
        "reindexer/trigger_reindex.py",
        "ingestor-test",
        ExclusivelyAffectsAnotherTask,
        False,
    ),
    # Chnages to Python files shouldn't trigger a Scala app
    ("shared_conftest.py", "ingestor-test", PythonChangeAndIsScalaApp, False),
    ("shared_conftest.py", "loris-test", UnrecognisedFile, True),

    # And now a grab bag of cases I've noticed actually happening in CI
    # where we ran tests we didn't need to, that I'll just keep adding as
    # regression tests.
    (
        "sbt_common/display/src/main/scala/uk/ac/wellcome/display/models/v1/DisplayConceptV1.scala",
        "bags_api-test",
        ChangeToUnusedLibrary,
        False,
    ),
    (
        "sbt_common/elasticsearch/src/main/scala/uk/ac/wellcome/elasticsearch/WorksIndex.scala",
        "ingests-test",
        ChangeToUnusedLibrary,
        False,
    ),
    (
        "sbt_common/internal_model/src/main/scala/uk/ac/wellcome/models/work/internal/Work.scala",
        "notifier-test",
        ChangeToUnusedLibrary,
        False,
    ),
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
        changed_paths=["sierra_adapter/common/main.scala"], task="loris-test"
    )
    assert result == (
        False,
        {
            False: {
                ScalaChangeAndNotScalaApp.message: set(
                    ["sierra_adapter/common/main.scala"]
                )
            },
            True: {},
        },
    )


def test_should_run_build_task_with_relevant_changes():
    result = should_run_build_task(
        changed_paths=["sierra_adapter/common/main.scala", "loris/loris/Dockerfile"],
        task="loris-test",
    )
    assert result == (
        True,
        {
            False: {
                ScalaChangeAndNotScalaApp.message: set(
                    ["sierra_adapter/common/main.scala"]
                )
            },
            True: {ExclusivelyAffectsThisTask.message: set(["loris/loris/Dockerfile"])},
        },
    )
