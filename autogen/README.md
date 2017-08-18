# autogen

This directory contains the pieces for our code autogeneration ("autogen").

Some of the types in our API take a limited number of different values -- for example, licenses.
These are defined in external data files (currently `ontologies/Reference data`).
We considered a couple of approaches before settling on code autogen:

*   Hand-write Scala definitions of these types.
    This is potentially brittle and makes it harder for non-developers to edit or use our data.
    It could also get out-of-date very easily.

*   Add the data files to the `resources` folder, load them at runtime and look up values dynamically (e.g. by a string key).
    This means these types would bypass compile-time checking, and is potentially quite complicated code.

*   Something with reflection, which is the same as the previous but even more complicated.

Using code autogen gets us safety checks from the compiler, allows us to define all our resources in a single place, and is among the simpler options.

## Usage

Run the autogen process:

```console
$ make run-autogen
```

Check if there are autogen changes that aren't committed to Git:

```console
$ make check-autogen
```

## Adding new autogen tasks

The interesting parts live in the `autogen_helpers` module.
Create a new template file (written with Jinja2 syntax) in the `templates` directory, and copy `licenses.py` for an example of how an autogen task works.

The main runner script is `run_autogen.py`.
