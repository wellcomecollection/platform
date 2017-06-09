lambdas
=======

This directory contains the source code for our AWS Lambdas.

common library
**************

Because of the slightly esoteric way we build our Lambdas, to add a file from the common lib to a package, you need to create a symlink from the right location.

.. code-block:: console

   $ cd /path/to/platform-infra
   $ cd lambdas/name-of-lambda
   $ ln -s ../common/sns_utils.py .
