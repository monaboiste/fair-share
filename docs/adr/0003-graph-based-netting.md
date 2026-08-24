# Use graph-based netting

Netting will represent Participants as vertices and Obligations in the Settlement Currency as directed weighted edges,
then produce a Proposed Repayment Graph. JGraphT stays behind the `graphs` module interface; we accept using a graph
even for the initial greedy algorithm so that the selected graph archetype is an actual building block and later graph
transformations have a stable seam.
