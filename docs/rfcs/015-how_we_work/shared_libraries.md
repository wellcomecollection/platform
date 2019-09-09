# Shared Libraries

## Why share?

The purpose of creating shared libraries is to:

- Increase development speed across projects where language and design patterns reoccur.
- Provide thorough testing for commonly reused code paths.
- Share optimisations where language and design patterns reoccur.

## Working with shared libraries

When adding functionality to existing libraries we:

- Add documentation as appropriate. See https://docs.scala-lang.org/style/scaladoc.html
- Release documentation should carry examples of API changes where they occur.
- Code reviews for shared libraries require approval from 2 developers one of whom is from a different project team.