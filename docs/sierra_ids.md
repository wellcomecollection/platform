# Sierra IDs

The Sierra IDs returned by the Sierra API are seven digits long,
for example `1234567`.

However, there are several optional features of a Sierra ID:

*   A leading period (`.`)

*   A one-character tag to identify the type -- for example, `b` for a
    bibliographic record, or `i` for an item record.

    Note that bibs and items have overlapping namespaces -- so we need this
    prefix to unambiguously identify a record if we don't have more context.
    For example, `1234567` could be a bib or an item.

*   An eighth [check digit][checkdigit].

Unfortunately, the presentation (and searchability) of a Sierra ID is
inconsistent between different systems.

[checkdigit]: https://en.wikipedia.org/wiki/Check_digit

## Current uses

| Usage                       | Leading period? | Record type? | Check digit? |
| --------------------------- | :-------------: | :----------: | :----------: |
| Sierra API                  | ·               | ·            | ·            |
| Sierra client display       | ·               | ✔            | ✔            |
| Sierra client search        | ·               | ✔            | ✔            |
| Encore URL                  | ·               | ✔            | ·            |
| Encore display              | ✔               | ✔            | ✔            |
| Encore search               | ·               | ·            | ·            |
| OPAC URL                    | ·               | ✔            | ·            |
| OPAC display                | ✔               | ✔            | ✔            |
| OPAC search                 | ·               | ·            | ·            |
| Viewer page URL (canonical) | ·               | ✔            | ✔            |
| Viewer page URL (redirect)  | ·               | ✔            | ·            |
| Internet Archive URL        | ·               | ✔            | ✔            |
| Internet Archive page       | ·               | ✔            | ✔            |
| Goobi                       | ·               | ✔            | ✔            |
| METS filename               | ·               | ✔            | ✔            |
| METS identifier             | ·               | ✔            | ✔            |
| Asset filename              | ·               | ✔            | ✔            |
| Miro XML exports            | inconsistent    | ✔            | ·            |

So the IDs have two main representations:

*   The *Sierra ID* (no check digit, no record type), and
*   The *Sierra system number* (check digit and record type)

The `.` prefix is only displayed in two places, and never used for search, so
we can ignore it.

## In the Catalogue pipeline

Since we receive a Sierra ID from the Sierra API, we store all our records
using this ID.  Throughout the pipeline, it's unambiguous whether a given ID
is for a bib or an item, so we don't need to worry about the record type.

## In the Catalogue API

For completeness, we'll include both versions of the ID in the Catalogue API:

```json
"identifiers": [
  {
    "identifierScheme": "sierra-system-number",
    "value": "b1234567x",
    "type": "Identifier"
  },
  {
    "identifierScheme": "sierra-id",
    "value": "1234567",
    "type": "Identifier"
  }
]
```

and this means we support searching on both of those forms.

We explicitly *don't* support searching on the following variants:

-  Prefix but no check digit (e.g. `b1234567`)
-  Using `a` as a wildcard for the check digit (e.g. `b1234567a`)
-  With a period as a prefix (e.g. `.b1234567x`)

## Computing the check digit

Quoting from the Sierra manual:

> Check digits may be any one of 11 possible digits (0, 1, 2, 3, 4, 5, 6, 7,
> 8, 9, or x).
>
> The check digit is calculated as follows:
>
> Multiply the rightmost digit of the record number by 2, the next digit to the
> left by 3, the next by 4, etc., and total the products. For example:
>
> Divide the total by 11 and retain the remainder (for example, 78 / 11 is 7,
> with a remainder of 1). The remainder after the division is the check digit.
> If the remainder is 10, the letter x is used as the check digit.

Here's an implementation of the check digit calculation in Scala:

```scala
def checkDigit(s: String): String = {
  val remainder = s
    .reverse
    .zip(Stream from 2)
    .map { case (char: Char, count: Int) => char.toString.toInt * count }
    .foldLeft(0)(_ + _) % 11
  if (remainder == 10) "x" else remainder.toString
}
```
