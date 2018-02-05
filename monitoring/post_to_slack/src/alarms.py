# -*- encoding: utf-8
"""
Utilities for working with CloudWatch alarms.
"""

from datetime import datetime
import re

import attr


# An alarm message is typically of the form:
#
#   Threshold Crossed: 1 datapoint [2.0 (05/02/18 06:35:00)] was greater than
#   or equal to the threshold (1.0).
#
THRESHOLD_RE = re.compile(
    r'^Threshold Crossed: '
    r'1 datapoint '
    r'\[(?P<actual_value>\d+)\.0 \((?P<date>\d{2}/\d{2}/\d{2} \d{2}:\d{2}:\d{2})\)\] '
    r'was '
    r'(?P<operator>(?:greater|less) than(?: or equal to)?) '
    r'the threshold \((?P<desired_value>\d+)\.0\).'
)


@attr.s
class Threshold:
    """Holds information about a "threshold crossed" error."""
    actual_value = attr.ib(converter=int)
    desired_value = attr.ib(converter=int)

    # Datetime strings in alarms are of the form DD/MM/YY HH:MM:SS.
    # For example: "03/02/18 16:13:00" or "05/02/18 06:38:00".
    date = attr.ib(
        converter=lambda dt: datetime.strptime(dt, '%d/%m/%y %H:%M:%S')
    )

    # e.g. 'greater than or equal to', 'less than'
    operator = attr.ib()

    @classmethod
    def from_message(cls, message):
        match = THRESHOLD_RE.match(message)
        if match is None:
            raise ValueError(f'Unable to parse message {message!r}')

        return cls(**match.groupdict())
