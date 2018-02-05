# -*- encoding: utf-8
"""
Utilities for working with CloudWatch alarms.
"""

import json


class Alarm:
    def __init__(self, message):
        self.message = message

    @property
    def name(self):
        """Returns the name of the alarm."""
        return self.message['AlarmName']

    @property
    def source(self):
        """Returns the name of the alarming service or queue, if possible."""
        # e.g. ingestor-alb-unhealthy-hosts, loris-alb-not-enough-healthy-hosts
        if '-alb-' in self.name:
            return self.name.split('-alb-')[0]

        # e.g. transformer_queue_dlq_not_empty, s3_demultiplexer_dlq_not_empty
        if self.name.endswith('_dlq_not_empty'):
            return self.name[:-len('_dlq_not_empty')]

        raise ValueError(f'Unable to identify source in {self.name!r}')
