#!/usr/bin/env python
# -*- encoding: utf-8
"""
Start/stop all the instances in an autoscaling group.

Usage: toggle_asg.py --name=<NAME> (--start | --stop)

Options:
  --name=<NAME>     Name of the autoscaling group.

Actions:
  --start           Start the autoscaling group (set the desired count to 1).
  --stop            Stop the autoscaling group (set the desired count to 0).

"""

import docopt


if __name__ == '__main__':
    args = docopt.docopt(__doc__)
