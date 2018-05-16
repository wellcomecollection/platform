# RFC 1: Matcher architecture

**Last updated: 15 May 2018.**

## Background

Items can be closely related, for example a book may be both a printed book and an eBook, or a painting can include a physical painting and photographs or xray
imagery of the painting.
Where works are so closely related that they represent or refer to the same 'thing' we wish to 
merge them into combined works to aid understanding and searching.

## Problem Statement

Individual works can be merged to form larger combined works.
The merging of works is broken into two phases

 * a matching/redirect phase that identifies works to be merged and tracks previously identified combined works.
 * a merging phase that merges works to form new combined works.
   
The matcher/redirecter receives source identifiers for processed works along with source identifiers for works that should be merged, 
and determines what changes to published works should be made.  Changes can include combining existing works and redirecting previously 
published works that have been superseded by merging operations.

Each work has a unique persistent id, and this introduces additional complexity because works 
may have an identity as an individual item, and then later be merged -- both ids need to remain valid and redirect 
to the correct combined work.  Similarly, works can be combined and later split or further combined but should retain 
their published identities so that the relevant combined work can still be found.

The merger takes identified works and merges them into new combined works.


## Proposed Solution

This document has notes on the proposed architecture.

## Model

A matching/redirecting phase is introduced to determine where works need to be combined, and to track where individual or 
combined works have previously published ids that need to be preserved.  

The input to this phase is the source identity for a work and a list of the identities that make up the combined work. 
The output is identifiers for each affected work, with redirection identifiers for existing works that need to change.

Works are identified by their source identifiers

## Storage

Related works that have been merged into a single work have an identity that should be preserved, 
where a merged work supersedes a previous work the resource identity of the previous work should redirect to the new
 combined work. Similarly when merged works are disconnected to create new individual works the resource identity of the 
 previous merged work should redirect.

This implies storing the state of related works to enable the merging process to also emit updates to the identities of 
affected merged works.

## Database schema

The id used for combined works is created by concatenating the sorted, namespaced source idenfifiers of their components.
For example if a work A is edited to include work B this will be identified in the 'identifiers' field of A.

For example creating a combined work 'A-B' when processing work 'A' could give as input:
```javascript
{
  A.identifiers : [ 
    // A
    { 
      identifierScheme: 'sierra-system-number',
      ontologyType: 'Work',
      value: 's12345'
    },
    // B
    {
      identifierScheme: 'miro-image-number',
      ontologyType: 'Work',
      value: 's67890'
    }
  ]
}
```
Note that `A.identifiers` includes the source identifier for  `A` itself.
In the case that neither A or B has previously been identified as a part of a combined work the matcher/redirector 
will output: 
```javascript
redirects = [ 
  { 
    target: 'sierra-system-number/s12345+miro-image-number/s67890' // A-B
    sources: [ 'sierra-system-number/s12345', 'miro-image-number/s67890'] // A, B
  }
]
```
The identifiers used are generated to be unique from source identifers (`$identifierScheme/$identifierValue`), and 
combined in a deterministic way for combined works [`$identifier1Scheme/$identifier1Value`, 
`$identifier2Scheme/$identifier2Value`].`sortAlphabetical`.`concatenate("+")`

Where works have previously published identifiers the output of the matcher/redirector will be updates to the 
existing works referenced by source ids.

For example, suppose works `A`, `B`, and `A-B` have been previously created and `A` is 
edited to additionally include `C`, then references to `A`, `B`, and the combined work `A-B` should be redirected to 
the new combined work `A-B-C`.  In this case the output from the matcher/redirector would be:
```javascript
{
  redirects: [ 
    { 
      target: 'sierra/A+miro/B+miro/C' // A-B-C
      sources: [ 'sierra/A', 'miro/B', 'sierra/A+miro/B' ] // A, B, A-B
    }
  ]
}

```


## Concurrent updates and locking

While the required updates for a merged work are being found there cannot be changes to the records for participating 
works.
A locking mechanism is used to achieve this, with all participating works being locked for editing.

## Storage of combined works and locks

Use dynamoDB with a works and a lock table.


## Examples

The matcher/redirector is best understood with examples.

## Example 1

Suppose we have the following graph:

![](matcher_example1.png)

We have two existing works: ABC and DEF.
We receive an update to B telling us it now has edges B→A and B→D.

0.  The existing DB is as follows:

        src   | tail_v | component_v | is_redirect | redirected_target
        A     | B      | ABC         | true        | ABC
        B     | A      | ABC         | true        | ABC
        C     | B      | ABC         | true        | ABC
        ABC   | _      | _           | false       | _
        D     | F      | DEF         | true        | DEF
        E     | D      | DEF         | true        | DEF
        F     | -      | DEF         | true        | DEF
        DEF   | -      | _           | false       | _

    (TODO: Should we have explicit redirects for single nodes?)

1.  Because we have an update that affects A, B and D, we read those rows from
    the database first:

        A     | B      | ABC         | true        | ABC
        B     | A      | ABC         | true        | ABC
        D     | F      | DEF         | true        | DEF    

2.  By looking at their connected components, we can do a second read to gather
    all the vertices that might be affected: ABCDEF.
    This gives us the database above, and enough to construct the entire graph
    we're interested in.

3.  Apply the changes, and work out what the new components are.  Write that
    back to the database.

        src   | tail_v | component_v | is_redirect | redirected_target
        A     | B      | ABCDEF      | true        | ABCDEF
        B     | AD     | ABCDEF      | true        | ABCDEF
        C     | B      | ABCDEF      | true        | ABCDEF
        ABC   | D      | ABCDEF      | true        | ABCDEF
        D     | F      | ABCDEF      | true        | ABCDEF
        E     | D      | ABCDEF      | true        | ABCDEF
        F     | -      | ABCDEF      | true        | ABCDEF
        DEF   | -      | ABCDEF      | true        | ABCDEF
        ABCDEF| -      | _           | false       | _

4.  The output JSON is:
    ```[json]
    [
        {
            "winner": "ABCDEF",
            "loosers":
                [
                    "A",
                    "B",
                    "C",
                    "ABC",
                    "D",
                    "E",
                    "F",
                    "DEF"
                ]
        }
    ]
    ```

## Example 2

What if we have two conflicting updates?

The graph of works is fairly sparse, and most of the time edits will either be
no-ops or affect entirely disconnected portions of the graph.

But let's suppose we have the following graph, and receive two updates that
overlap (deliberately not numbered as there's no ordering on updates to
different vertices).

![](matcher_example2.png)

0.  The existing DB is as follows:

            src   | tail_v | component_v | is_redirect | redirected_target
            A     | B      | ABC         | true        | ABC
            B     | A      | ABC         | true        | ABC
            C     | B      | ABC         | true        | ABC
            ABC   | _      | _           | false       | _
            D     | F      | DEF         | true        | DEF
            E     | D      | DEF         | true        | DEF
            F     | -      | DEF         | true        | DEF
            DEF   | -      | _           | false       | _
            G     | -      | G           | false       | -

1.  Update (*) is processed, and it affects nodes B and E.
    So the worker handling (*) acquires a row-level lock on those two nodes.

    Meanwhile update (**) is also being processed, and it affects F and G.
    So the worker handling (**) F and G acquires a lock on those two rows.

            src   | tail_v | component_v | is_redirect | redirected_target
            A     | B      | ABC         | true        | ABC
        *   B     | A      | ABC         | true        | ABC
            C     | B      | ABC         | true        | ABC
            ABC   | _      | _           | false       | _
        *   D     | F      | DEF         | true        | DEF
        **  E     | D      | DEF         | true        | DEF
        **  F     | -      | DEF         | true        | DEF
            DEF   | -      | _           | false       | _
            G     | -      | G           | false       | -
2.  Process (*) discovers that it affects vertices ABCDEF.

    Process (**) discovers that it affects vertices DEFG.

    Both of them attempt to expand to lock all the vertices they affect -- and
    whoever tries first will hit the other's lock.  They then release their
    lock, and allow the other update to proceed.

    The update will go back into the SQS queue, and will be retried until it
    can be applied conflict-free.

This results in an eventually consistent graph, which is as good as we can
guarantee.

## Example 3

What happens if we remove a link?

![](matcher_example3.png)

In this example A, B and C are connected into a component called ABC. We receive an update to C that causes ABC to be split.

0.  The existing DB is as follows:

            src   | tail_v | component_v | is_redirect | redirected_target
            A     | B      | ABC         | true        | ABC
            B     | A      | ABC         | true        | ABC
            C     | B      | ABC         | true        | ABC
            ABC   | _      | _           | false       | _

1.  Because we have an update that affects C, we read and acquire a lock on C from the database first:

            src   | tail_v | component_v | is_redirect | redirected_target
            C     | B      | ABC         | true        | ABC

2.  By looking at their connected components, we acquire a lock on all other vertices affected: A,B and ABC.
3.  We update C to not redirect to ABC:

            src   | tail_v | component_v | is_redirect | redirected_target
            C     | _      | _           | false       | _

4.  We create a new component AB and redirect A and B to it:

            src   | tail_v | component_v | is_redirect | redirected_target
            A     | B      | AB          | true        | AB
            B     | A      | AB          | true        | AB
            AB    | _      | _           | false       | _

5.  We modify ABC to redirect to AB:

            src   | tail_v | component_v | is_redirect | redirected_target
            ABC   | _      | _           | true        | AB

    How do we choose where to redirect a subgraph that gets split? In this case AB seems the most reasonable choice because it's the biggest bit of the late ABC, but what about graphs that get split into same size bits?

6.  The database ends up looking like this:

            src   | tail_v | component_v | is_redirect | redirected_target
            A     | B      | AB          | true        | AB
            B     | A      | AB          | true        | AB
            AB    | _      | _           | false       | _
            C     | _      | _           | false       | _
            ABC   | _      | _           | true        | AB

7.  The output JSON is:
    ```[json]
    [
        {
            "winner": "AB",
            "loosers":
                [
                    "A",
                    "B",
                    "ABC"
                ]
        },
        {
            "winner": "C",
            "loosers": []
        }
    ]
    ```
