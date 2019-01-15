#!/bin/sh

# Will try and run 12 bagger processes in parallel.

parallel --ungroup --jobs 12 <<HERE
python3 -u main.py
python3 -u main.py
python3 -u main.py
python3 -u main.py
python3 -u main.py
python3 -u main.py
python3 -u main.py
python3 -u main.py
python3 -u main.py
python3 -u main.py
python3 -u main.py
python3 -u main.py
HERE