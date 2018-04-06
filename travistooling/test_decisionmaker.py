# -*- encoding: utf-8

import pytest

from travistooling.decisionmaker import does_file_affect_build_job
from travistooling.decisions import (
    IgnoredFileFormat,
    IgnoredPath,
    KnownAffectsTask,
    UnrecognisedFile
)


@pytest.mark.parametrize('path, job_name, exc_class, is_significant', [
    # Travis format task
    ('misc/myscript.py', 'travis-format', KnownAffectsTask, True),
    ('ontologies/item.ttl', 'travis-format', KnownAffectsTask, True),
    ('main.tf', 'travis-format', KnownAffectsTask, True),
    ('example.json', 'travis-format', KnownAffectsTask, True),
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
])
def test_does_file_affect_build_job(path, job_name, exc_class, is_significant):
    with pytest.raises(exc_class) as err:
        does_file_affect_build_job(path=path, job_name=job_name)
    assert err.value.is_significant == is_significant
