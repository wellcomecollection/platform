# -*- encoding: utf-8

import attr
import pytest

from platform_alarms import guess_cloudwatch_log_group


@attr.s
class Alarm:
    name = attr.ib()


@pytest.mark.parametrize('alarm_name, expected_log_group_name', [
    ('loris-alb-target-500-errors', 'platform/loris'),
    ('loris-alb-not-enough-healthy-hosts', 'platform/loris'),
    ('api_romulus_v1-alb-target-400-errors', 'platform/api_romulus_v1'),
    ('api_remus_v1-alb-target-500-errors', 'platform/api_remus_v1'),
    ('lambda-ecs_ec2_instance_tagger-errors', '/aws/lambda/ecs_ec2_instance_tagger'),
    ('lambda-post_to_slack-errors', '/aws/lambda/post_to_slack'),
])
def test_guess_cloudwatch_log_group(alarm_name, expected_log_group_name):
    alarm = Alarm(name=alarm_name)
    assert guess_cloudwatch_log_group(alarm) == expected_log_group_name


@pytest.mark.parametrize('bad_alarm_name', [
    'api_remus_v2-alb-target-500-errors',
    'winnipeg-not-enoguh-healthy-hosts',
])
def test_unrecognised_log_group_name_is_valueerror(bad_alarm_name):
    alarm = Alarm(name=bad_alarm_name)
    with pytest.raises(ValueError):
        guess_cloudwatch_log_group(alarm)
