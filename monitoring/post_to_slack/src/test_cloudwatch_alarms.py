# -*- encoding: utf-8

import datetime as dt

import pytest

from cloudwatch_alarms import build_cloudwatch_url, ThresholdMessage


class TestThresholdMessage:

    @pytest.mark.parametrize('message, actual_value', [
        ('Threshold Crossed: 1 datapoint [1.0 (05/02/18 06:28:00)] was greater than the threshold (1.0).', 1),
        ('Threshold Crossed: 1 datapoint [12.0 (05/02/18 06:28:00)] was greater than the threshold (1.0).', 12),
    ])
    def test_actual_value(self, message, actual_value):
        t = ThresholdMessage.from_message(message)
        assert t.actual_value == actual_value

    @pytest.mark.parametrize('message, desired_value', [
        ('Threshold Crossed: 1 datapoint [1.0 (05/02/18 06:28:00)] was greater than the threshold (1.0).', 1),
        ('Threshold Crossed: 1 datapoint [1.0 (05/02/18 06:28:00)] was greater than the threshold (12.0).', 12),
        ('Threshold Crossed: 1 datapoint [1.0 (05/02/18 06:28:00)] was greater than the threshold (0.0).', 0),
        ('Threshold Crossed: 1 datapoint [1.0 (06/04/18 13:26:00)] was less than the threshold (1.5).', 1.5),
    ])
    def test_desired_value(self, message, desired_value):
        t = ThresholdMessage.from_message(message)
        assert t.desired_value == desired_value

    @pytest.mark.parametrize('message, expected_date', [
        ('Threshold Crossed: 1 datapoint [1.0 (05/02/18 06:28:00)] was greater than the threshold (1.0).',
         dt.datetime(2018, 2, 5, 6, 28, 0)),
        ('Threshold Crossed: 1 datapoint [1.0 (11/08/18 10:55:00)] was greater than the threshold (1.0).',
         dt.datetime(2018, 8, 11, 10, 55, 0)),
        ('Threshold Crossed: 1 datapoint [1.0 (09/01/18 10:36:31)] was greater than the threshold (1.0).',
         dt.datetime(2018, 1, 9, 10, 36, 31)),
    ])
    def test_date(self, message, expected_date):
        t = ThresholdMessage.from_message(message)
        assert t.date == expected_date

    @pytest.mark.parametrize('message, expected_operator', [
        ('Threshold Crossed: 1 datapoint [1.0 (05/02/18 06:28:00)] was greater than the threshold (1.0).', 'greater than'),
        ('Threshold Crossed: 1 datapoint [1.0 (05/02/18 06:28:00)] was greater than or equal to the threshold (1.0).', 'greater than or equal to'),
        ('Threshold Crossed: 1 datapoint [1.0 (05/02/18 06:28:00)] was less than the threshold (1.0).', 'less than'),
        ('Threshold Crossed: 1 datapoint [1.0 (05/02/18 06:28:00)] was less than or equal to the threshold (1.0).', 'less than or equal to'),
    ])
    def test_operator(self, message, expected_operator):
        t = ThresholdMessage.from_message(message)
        assert t.operator == expected_operator

    @pytest.mark.parametrize('bad_message', ['foo', 'not a real message'])
    def test_unexpected_message_is_valueerror(self, bad_message):
        with pytest.raises(ValueError):
            ThresholdMessage.from_message(bad_message)

    @pytest.mark.parametrize('message, is_breaching', [
        ('Threshold Crossed: 1 datapoint [1.0 (05/02/18 06:28:00)] was greater than the threshold (1.0).', False),
        ('Threshold Crossed: no datapoints were received for 1 period and 1 missing datapoint was treated as [Breaching].', True),
    ])
    def test_is_breaching(self, message, is_breaching):
        t = ThresholdMessage.from_message(message)
        assert t.is_breaching == is_breaching


def test_build_cloudwatch_url():
    url = build_cloudwatch_url(
        search_term='HTTP500',
        log_group_name='platform/loris',
        start_date=dt.datetime(2018, 3, 5, 6, 28, 0),
        end_date=dt.datetime(2018, 3, 5, 6, 34, 0),
        region='us-east-1'
    )
    assert url == (
        'https://us-east-1.console.aws.amazon.com/cloudwatch/home'
        '?region=us-east-1#logEventViewer:group=platform/loris;'
        'filter=HTTP500;start=2018-03-05T06:28:00Z;end=2018-03-05T06:34:00Z;'
    )
