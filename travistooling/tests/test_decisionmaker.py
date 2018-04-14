# -*- encoding: utf-8

import pytest

from travistooling.decisionmaker import (
    does_file_affect_build_task,
    should_run_build_task
)
from travistooling.decisions import (
    ChangesToTestsDontGetPublished,
    CheckedByTravisFormat,
    ExclusivelyAffectsAnotherTask,
    ExclusivelyAffectsThisTask,
    IgnoredFileFormat,
    IgnoredPath,
    ScalaChangeAndIsScalaApp,
    ScalaChangeAndNotScalaApp,
    UnrecognisedFile
)


@pytest.mark.parametrize('path, task, exc_class, is_significant', [
    # Travis format task
    ('misc/myscript.py', 'travis-format', CheckedByTravisFormat, True),
    ('ontologies/item.ttl', 'travis-format', CheckedByTravisFormat, True),
    ('main.tf', 'travis-format', CheckedByTravisFormat, True),
    ('example.json', 'travis-format', CheckedByTravisFormat, True),
    ('README.md', 'travis-format', IgnoredFileFormat, False),

    # Unrecognised filenames fall through to being significant, regardless
    # of the build job.
    ('foo.txt', 'loris-build', UnrecognisedFile, True),
    ('foo.txt', 'api-test', UnrecognisedFile, True),
    ('foo.txt', 'snapshot_convertor-publish', UnrecognisedFile, True),

    # Certain file formats are always excluded.
    ('foo.md', 'ingestor-build', IgnoredFileFormat, False),
    ('image.png', 'reindex_worker-test', IgnoredFileFormat, False),
    ('ontology.graffle', 'nginx-test', IgnoredFileFormat, False),

    # Terraform files are significant, but only in the travis-format task
    ('s3.tf', 'elasticdump-test', IgnoredFileFormat, False),

    # Certain paths are always insignificant.
    ('LICENSE', 'travistooling-test', IgnoredPath, False),

    # Ontology/misc are significant, but only in the travis-format task
    ('misc/myscript.py', 'sierra_reader-build', IgnoredPath, False),
    ('ontologies/work.ttl', 'monitoring-publish', IgnoredPath, False),

    # Changes that belong exclusively to a single task
    ('loris/loris/Dockerfile', 'loris-build', ExclusivelyAffectsThisTask, True),
    ('loris/loris/Dockerfile', 'ingestor-test', ExclusivelyAffectsAnotherTask, False),
    ('sierra_adapter/sierra_reader/foo.scala', 'sierra_reader-publish', ExclusivelyAffectsThisTask, True),
    ('catalogue_pipeline/ingestor/bar.scala', 'api-test', ExclusivelyAffectsAnotherTask, False),

    # Anything in the sierra_adapter directory/common lib
    ('sierra_adapter/common/main.scala', 'loris-test', ScalaChangeAndNotScalaApp, False),
    ('sierra_adapter/common/main.scala', 's3_demultiplexer-test', ScalaChangeAndNotScalaApp, False),
    ('sierra_adapter/common/main.scala', 'sierra_window_generator-test', ScalaChangeAndNotScalaApp, False),
    ('sbt_common/display/model.scala', 'id_minter-test', ScalaChangeAndIsScalaApp, True),
    ('sbt_common/display/model.scala', 'loris-publish', ScalaChangeAndNotScalaApp, False),
    ('sbt_common/display/model.scala', 'sierra_adapter-publish', UnrecognisedFile, True),

    # Changes to Scala test files trigger a -test Scala task, but not
    # a -publish task.
    ('sbt_common/src/test/scala/uk/ac/wellcome/MyTest.scala', 'sierra_adapter-publish', ChangesToTestsDontGetPublished, False),
    ('sbt_common/src/test/scala/uk/ac/wellcome/MyTest.scala', 'sierra_adapter-test', UnrecognisedFile, True),
])
def test_does_file_affect_build_task(path, task, exc_class, is_significant):
    with pytest.raises(exc_class) as err:
        does_file_affect_build_task(path=path, task=task)
    assert err.value.is_significant == is_significant


def test_should_run_build_task_with_no_important_changes():
    result = should_run_build_task(changed_paths=[], task='loris-test')
    assert result == (False, {False: {}, True: {}})


def test_should_not_run_job_with_no_relevant_changes():
    result = should_run_build_task(
        changed_paths=['sierra_adapter/common/main.scala'],
        task='loris-test'
    )
    assert result == (False, {
        False: {ScalaChangeAndNotScalaApp.message: set(['sierra_adapter/common/main.scala'])},
        True: {}
    })


def test_should_run_build_task_with_relevant_changes():
    result = should_run_build_task(
        changed_paths=[
            'sierra_adapter/common/main.scala',
            'loris/loris/Dockerfile',
        ],
        task='loris-test'
    )
    assert result == (True, {
        False: {ScalaChangeAndNotScalaApp.message: set(['sierra_adapter/common/main.scala'])},
        True: {ExclusivelyAffectsThisTask.message: set(['loris/loris/Dockerfile'])}
    })
