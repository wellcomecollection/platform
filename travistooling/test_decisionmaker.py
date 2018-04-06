# -*- encoding: utf-8

import pytest

from travistooling.decisionmaker import does_file_affect_build_job
from travistooling.decisions import UnrecognisedFile


@pytest.mark.parametrize('path, job_name, exc_class, is_significant', [
    # Unrecognised filenames fall through to being significant, regardless
    # of the build job.
    ('foo.txt', 'loris-build', UnrecognisedFile, True),
    ('foo.txt', 'api-test', UnrecognisedFile, True),
    ('foo.txt', 'snapshot_convertor-publish', UnrecognisedFile, True),
])
def test_does_file_affect_build_job(path, job_name, exc_class, is_significant):
    with pytest.raises(exc_class) as err:
        does_file_affect_build_job(path=path, job_name=job_name)
    assert err.value.is_significant == is_significant
