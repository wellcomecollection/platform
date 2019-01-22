#!/usr/bin/env bash

# Add b numbers to the queue from the local machine.

# This is the primary means of triggering the process, as it is a manual decision.

# USAGE:

# . queue_for_bagging.sh <filter|bnumber> <bag|no-bag>

# the <filter> limits the b numbers returned to a filtered set, based on keys.
# The spread of b numbers is fairly even.

# EXAMPLES:

# . queue_for_bagging.sh x/1/2 no-bag
# Enqueue approximately 0.1% of the b numbers, but do not bag - just process METS

# . queue_for_bagging.sh b18035978 bag
# Enqueue one b number only, and bag it.

# . queue_for_bagging.sh b18035978 no-bag
# Same but METS only.

# aws
export AWS_ACCESS_KEY_ID=''
export AWS_SECRET_ACCESS_KEY=''
export AWS_DEFAULT_REGION=''
export BAGGING_QUEUE=''

python ../src/local_enqueue.py $1 $2
