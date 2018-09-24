# -*- encoding: utf-8

import datetime as dt


def pprint_timedelta(seconds):
    """
    Returns a pretty-printed summary of a duration as seconds.

    e.g. "1h", "2d 3h", "1m 4s".

    """
    days, seconds = divmod(seconds, 86400)
    hours, seconds = divmod(seconds, 3600)
    minutes, seconds = divmod(seconds, 60)

    if days > 0:
        if hours == 0:
            return '%dd' % days
        else:
            return '%dd %dh' % (days, hours)

    elif hours > 0:
        if minutes == 0:
            return '%dh' % hours
        else:
            return '%dh %dm' % (hours, minutes)

    elif minutes > 0:
        if seconds == 0:
            return '%dm' % minutes
        else:
            return '%dm %ds' % (minutes, seconds)
    else:
        return '%ds' % seconds
