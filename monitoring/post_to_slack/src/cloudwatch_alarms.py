# -*- encoding: utf-8

from datetime import datetime
import re
from urllib.parse import quote as urlquote

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
    r'the threshold \((?P<desired_value>\d+\.\d+)\).'
)

# We may also get a message of the form:
#
#   Threshold Crossed: no datapoints were received for 1 period
#   and 1 missing datapoint was treated as [Breaching].
#
MISSING_DATE_RE = re.compile(
    r'^Threshold Crossed: no datapoints were received for \d+ period '
    r'and \d missing datapoint was treated as \[Breaching\]\.$'
)


def _parse_numeral(xs):
    if xs is None:
        return None

    value = float(xs)
    if value.is_integer():
        return int(value)
    else:
        return value


@attr.s
class ThresholdMessage:
    """Holds information about a "threshold crossed" message."""
    is_breaching = attr.ib()

    actual_value = attr.ib(converter=_parse_numeral)
    desired_value = attr.ib(converter=_parse_numeral)

    # Datetime strings in alarms are of the form DD/MM/YY HH:MM:SS.
    # For example: "03/02/18 16:13:00" or "05/02/18 06:38:00".
    date = attr.ib(
        converter=lambda dt: datetime.strptime(dt, '%d/%m/%y %H:%M:%S') if dt is not None else None
    )

    # e.g. 'greater than or equal to', 'less than'
    operator = attr.ib()

    @classmethod
    def from_message(cls, message):
        match = MISSING_DATE_RE.match(message)
        if match is not None:
            return cls(
                is_breaching=True,
                actual_value=None,
                desired_value=None,
                date=None,
                operator=None
            )

        match = THRESHOLD_RE.match(message)
        if match is None:
            raise ValueError(f'Unable to parse message {message!r}')

        return cls(**match.groupdict(), is_breaching=False)


def build_cloudwatch_url(
    search_term, log_group_name, start_date, end_date, region='eu-west-1'
):
    """
    Builds a URL that opens the CloudWatch Console with the given filters.
    """
    return (
        f'https://{region}.console.aws.amazon.com/cloudwatch/home'
        f'?region={region}'
        f'#logEventViewer:group={log_group_name};'
        f'filter={urlquote(search_term)};'
        f'start={start_date.strftime("%Y-%m-%dT%H:%M:%SZ")};'
        f'end={end_date.strftime("%Y-%m-%dT%H:%M:%SZ")};'
    )


def datetime_to_cloudwatch_ts(dt):
    """
    Convert a Python ``datetime`` instance to a CloudWatch timestamp,
    the number of milliseconds after Jan 1, 1970 00:00:00 UTC.
    """
    epoch = datetime(1970, 1, 1, 0, 0, 0)
    return int((dt - epoch).total_seconds()) * 1000
