# RFC 1: Matcher architecture

**Last updated: 15 May 2018.**

## Background

Works are often related to one another, for example items in a box or plates in a portfolio.
These relationships should be reflected in the way works are stored and searched.

## Problem Statement

Individual works can form part of larger works.
Works that are related should be merged to form merged works.
This is broken into two phases, a matching phase that identifies related works and a merging phase that merges those works
into new merged works.  
The matcher receives source records/transformed works, and it determines which source records correspond to the same work.
The merger takes identified related works and merges them into new combined works.

Each work has a unique persistent id, and this introduces additional complexity into the merging process because works 
may have an identity as an individual item, and then later be merged -- both these ids need to remain valid and redirect 
to the correct combined work.  Similarly, works can be combined and later split but should retain their identities so that
they can still be found.

This document has some notes on the proposed architecture.


## Proposed Solution

This document has notes on the proposed architecture.

## Model

We model source records and their relationships as a graph.
Vertices are source records, and there is a directed edge from X to Y if information in the source record X tells us that it is related to Y.

Each merged work is one of the connected components of this graph.

![](matcher_graph.png)

In this example, there are three works:

-   A red work made of three source records
-   A green work made of two source records
-   A blue work that is a single source record

The matcher will receive updates to this graph, one vertex at a time.
When it receives an update, it needs to tell us:

-   Any new works which have been created
-   Any old works which have been destroyed and need to be redirected (if two components merge, or one component splits)

## Storage

Related works that have been merged into a single work have an identity that should be preserved, 
where a merged work supersedes a previous work the resource identity of the previous work should redirect to the new
 combined work. Similarly when merged works are disconnected to create new individual works the resource identity of the 
 previous merged work should redirect.

This implies storing the state of related works to enable the merging process to also emit updates to the identities of 
all related merged works.

## Database schema

We can identify subgraphs/components by concatenating the sorted IDs of the vertices.
For example, the graph below has three components: `A-B-C-D`, `E-F-G` and `H`.

![](matcher_component_ids.png)

This is our current schema:

-   `source_component` -- contains the identifier of a component.
    This may be a single vertex.

-   `tail_vertices` -- if the source component is a single vertex, a list of
    vertices which are the "tail" of a directed edge from this vertex.

-   `component_vertices` -- a list of all the vertices in the same connected component.

-   `is_redirect` -- has this component been replaced and redirected to
    something else?

-   `redirected_target` -- if so, the ID of the target it redirects to.

-   `version` -- we can version updates to source records, and so we can
    version updates to the vertices on this graph.

## Example 1

This is best illustrated with an example.
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
