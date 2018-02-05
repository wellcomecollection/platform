# -*- encoding: utf-8

import pytest

from alarms import Alarm


@pytest.mark.parametrize('alarm_name, expected_source', [
    ('ingestor-alb-unhealthy-hosts', 'ingestor'),
    ('loris-alb-not-enough-healthy-hosts', 'loris'),
    ('transformer_queue_dlq_not_empty', 'transformer_queue'),
    ('sierra_items_windows_dlq_not_empty', 'sierra_items_windows'),
])
def test_source(alarm_name, expected_source):
    alarm = Alarm(message={'AlarmName': alarm_name})
    assert alarm.source == expected_source
