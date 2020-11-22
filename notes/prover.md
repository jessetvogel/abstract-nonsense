## Prover

- Set of propositions that have already been searched for (to prevent infinite loops)
- List / queue of goals
  - **Goal** is a list of categories (i.e. propositions to prove) and a pointer to a category (i.e. propositon) that it proves. Maybe also a reference to a Theorem and a Mapping, so that it can be applied once verified.
- When trying to prove a goal, go through each condition of the goal, and try to apply theorems to prove that condition.
- 



----

- Need to keep track of which goals have already been attempted (and which might succeed, and which have no hope of being proven)



- `Map<Morphism, Goal> goals`

- `Goal`
  - `Morphism P`
  - `status = PROVEN | FAILED | UNDECIDED`
  - `List<Implication> needed_for `

- `Implication`
  - `List<Goal> conditions`
  - `Goal goal`



1. Start with some proposition $P$ that we want to prove.
2. If we already have a proof, done.
3. Create some `Goal finalGoal` corresponding to $P$
4. For each applicable theorem:
   1. Check the conditions, if all hold, then the goal is proven.
   2. Create an `Implication` with the remaining conditions, which points to the current goal.
   3. Add all conditions (as goals) to the queue
5. Move on to the next goal in the queue





- `while(finalGoal is not proven && queue not empty)`

  - Pop the first goal from the queue, and `considerGoal(goal)`

- `return finalGoal.isProven()`

  

- `considerGoal(goal)`
  - If it is not needed for anything, simply continue
  - If it already has a proof, then set the goal to `PROVEN`
  - For each applicable theorem:
    - Check the conditions, if all hold, then the goal is proven.
    - Create an `Implication` with the remaining conditions, which points to the current goal.
    - Add all conditions (as goals) to the queue



- When a goal is set to `PROVEN`: remove the goal from all implications that it is needed for (no longer a condition since it is true)
- When a goal is set to `FAILED`: 'delete' all implications which this goal is needed for
- When an implication is 'deleted' : remove itself from all goals that it needs







