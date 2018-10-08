# -*- encoding: utf-8

import types

from ._base import TypedModel
from .catalogue import Error


def register_models(namespace, models):
     """
     Register all the models in a module against the given namespace.

     We have to do this because Flask-RESTPlus doesn't register models
     recursively -- so if we have Foo(id: str, bar: Bar), then registering
     the Foo model doesn't get us the Bar model, and then the Swagger
     fails to render.  Boo.
     """
     # How it works: for a module, the __dict__ contains a mapping from
     # name to importable object.  By iterating over the values, we get
     # everything that you can import from the module.
     #
     assert isinstance(models, types.ModuleType)
     for m in models.__dict__.values():
         if not isinstance(m, TypedModel):
             continue

         # This function gets called recursively (somewhere) -- this stops
         # us triggering an infinite recursion.
         if m.name not in namespace.models:
             namespace.add_model(m.name, m)


__all__ = [
    "Error",
    "register_models",
]
