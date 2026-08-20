# Basic Editing Rules of Safeguard Project

## Before All Edits

- If the current session requires a medium-big edit (affecting >= 5 files), you should check which branch the workspace
  is on before starting if the user didn't ask you not to.
    - If it's on the main branches (`master` (latest supported Minecraft version),`26.1.2` , `1.21.11`, `1.20.6`, etc.),
      create a new branch according to the circumstances of the task and checkout to it.
    - If it's already on a new branch, and the name of the branch is suitable for the task, you can perform edits on
      this branch.
    - If you're not sure, ask the user directly.
- Unless asked specifically, please don't use worktrees.

## After Applying Edits

- After making a certain amount of changes, you should commit the code in multiple times.
    - Commit messages follow the Conventional Commits specification.
- After completing all commits, there's no need to push unless the user specifically asks.
