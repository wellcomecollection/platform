#!/bin/sh

# Will try and run 4 bagger processes in parallel.

parallel --jobs 4 <<HERE
python3 -u main.py
python3 -u main.py
python3 -u main.py
python3 -u main.py
HERE