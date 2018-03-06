# Merger architecture

The merger is a new application that we are planning to write in Q3 2018.

It receives source records/transformed Works, and it works out which source records correspond to the same Work in our pipeline.

This document has some notes on the proposed architecture.

## Model

We model source records and their relationships as a graph.
The vertices are the source records, and there is a directed edge from X to Y if information in the source record X tells us that it is related to Y.

Each merged work is one of the connected components of this graph.

![](relater_graph.png)

In this example, there are three works:

-   A red work made of three source records
-   A green work made of two source records
-   A blue work that is a single source record

The relater will receive updates to this graph, one vertex at a time.
When it receives an update, it needs to tell us:

-   Any new works which have been created
-   Any old works which have been destroyed and need to be redirected (if two components emerge, or one component splits)

## Storage

We considered three storage options:

-   Neo4j (a filesystem-backed graph database)
-   Titan/JanusGraph (a DynamoDB-backed graph database)
-   Storing graph information in SQL

We decided to use SQL, specifically RDS, as we already use it in the ID minter, and we know how it scales.

The idea is to store the structure of the graph in SQL, but do all of the graph theory logic outside the database.
Trying to do graph operations in SQL queries feels a step too far!

## Database schema

We can identify subgraphs/components by concatenating the sorted IDs of the vertices.
For example, the graph below has three components: `A-B-C-D`, `E-F-G` and `H`.

![](relater_component_ids.png)

This is our current schema:

-   `source_component` -- contains the identifier of a component.
    This may be a single vertex.

-   `tail_vertices` -- if the source component is a single vertex, a list of
    vertices which are the "tail" of a directed edge from this vertex.

-   `component_vertices` -- if this is a single vertex, a list of all the
    vertices in the same connected component.

-   `is_redirect` -- has this component been replaced and redirected to
    something else?

-   `redirected_target` -- if so, the ID of the target it redirects to.

-   `version` -- we can version updates to source records, and so we can
    version updates to the vertices on this graph.

## Example 1

This is best illustrated with an example.
Suppose we have the following graph:

![](relater_example1.png)

We have two existing works: ABC and DEF.
We receive an update to B telling us it now has edges B→A and B→D.

0.  The existing DB is as follows:

        src   | tail_v | component_v | is_redirect | redirected_target
        A     | B      | ABC         | -           | -
        B     | A      | ABC         | -           | -
        C     | B      | ABC         | -           | -
        D     | F      | DEF         | -           | -
        E     | D      | DEF         | -           | -
        F     | -      | DEF         | -           | -

    (TODO: Should we have explicit redirects for single nodes?)

1.  Because we have an update that affects A, B and D, we read those rows from
    the database first:

        A     | B      | ABC         | -           | -
        B     | A      | ABC         | -           | -
        D     | F      | DEF         | -           | -

2.  By looking at their connected components, we can do a second read to gather
    all the vertices that might be affected: ABCDEF.
    This gives us the database above, and enough to construct the entire graph
    we're interested in.

3.  Apply the changes, and work out what the new components are.  Write that
    back to the database.

        src   | tail_v | component_v | is_redirect | redirected_target
        A     | B      | ABCDEF      | -           | -
        B     | AD     | ABCDEF      | -           | -
        C     | B      | ABCDEF      | -           | -
        D     | F      | ABCDEF      | -           | -
        E     | D      | ABCDEF      | -           | -
        F     | -      | ABCDEF      | -           | -

    Include two extra rows for the original components ABC and DEF, which now
    redirect to ABCDEF:

        src   | tail_v | component_v | is_redirect | redirected_target
        ABC   | -      | -           | true        | ABCDEF
        DEF   | -      | -           | true        | ABCDEF

## Example 2

What if we have two conflicting updates?

The graph of works is fairly sparse, and most of the time edits will either be
no-ops or affect entirely disconnected portions of the graph.

But let's suppose we have the following graph, and receive two updates that
overlap (deliberately not numbered as there's no ordering on updates to
different vertices).

![](relater_example2.png)

0.  The existing DB is as follows:

            src   | tail_v | component_v | is_redirect | redirected_target
            A     | B      | ABC         | -           | -
            B     | A      | ABC         | -           | -
            C     | B      | ABC         | -           | -
            D     | F      | DEF         | -           | -
            E     | D      | DEF         | -           | -
            F     | -      | DEF         | -           | -
            G     | -      | G           | -           | -

1.  Update (*) is processed, and it affects nodes B and E.
    So the worker handling (*) acquires a row-level lock on those two nodes.

    Meanwhile update (**) is also being processed, and it affects F and G.
    So the worker handling (**) F and G acquires a lock on those two rows.

            src   | tail_v | component_v | is_redirect | redirected_target
            A     | B      | ABC         | -           | -
        *   B     | A      | ABC         | -           | -
            C     | B      | ABC         | -           | -
            D     | F      | DEF         | -           | -
        *   E     | D      | DEF         | -           | -
        **  F     | -      | DEF         | -           | -
        **  G     | -      | G           | -           | -

2.  Process (*) discovers that it affects vertices ABCDEF.

    Process (**) discovers that it affects vertices DEFG.

    Both of them attempt to expand to lock all the vertices they affect -- and
    whoever tries first will hit the other's lock.  They then release their
    lock, and allow the other update to proceed.

    The update will go back into the SQS queue, and will be retried until it
    can be applied conflict-free.

This results in an eventually consistent graph, which is as good as we can
guarantee.
