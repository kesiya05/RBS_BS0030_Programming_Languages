# BS0030 Week 3 Lab

**Functional Programming I — Clojure REPL, functions, immutable data, and collection transformations**

Week 3 is the first major paradigm shift in BS0030. The goal is not to memorize Clojure syntax. The goal is to experience computation organized around **values, functions, and transformations** rather than explicit mutable state.

## Before you start

Use **your fork** of `ValRCS/RBS_BS0030_Programming_Languages`. Your fork is your permanent course repository; a Codespace is a disposable development environment.

1. Open your fork on GitHub.
2. Create a Codespace.
3. Choose the **BS0030 Functional — Clojure** Dev Container configuration.
4. Wait for the Codespace to finish building.

The container checks Java and Clojure automatically after creation. You can rerun the check from the repository root:

```bash
bash scripts/check-functional-environment.sh
```

The Week 3 project pins the Clojure language to **1.12.6** in `labs/week03/deps.edn`.

## Create your Week 3 work area

The `starter/` directory is instructor-managed. **Do not edit it directly.** Copy the starter file into `work/` once:

```bash
cp labs/week03/starter/week03_lab.clj labs/week03/work/
cd labs/week03
```

If `work/week03_lab.clj` already exists, do not overwrite it.

## Start a REPL

From `labs/week03`:

```bash
clojure
```

You should see a prompt similar to:

```text
Clojure 1.12.6
user=>
```

Load your working file:

```clojure
(load-file "work/week03_lab.clj")
```

After editing the file, run the same `load-file` form again to reload your definitions.

Exit the REPL with `Ctrl+D`.

## Recommended working rhythm

For each exercise:

1. **Predict** what a form should evaluate to.
2. Enter or load the form.
3. **Evaluate** it in the REPL.
4. Compare the result with your prediction.
5. Explain the result using Week 3 vocabulary: value, binding, function, higher-order function, immutable data, transformation, predicate, reduction, recursion, or effect.

This is more important than typing quickly.

## Continue with the lab

Read [`assignment.md`](assignment.md) for the full sequence.

Your main working file is:

```text
labs/week03/work/week03_lab.clj
```

When finished, commit your Week 3 work to **your fork**.

> **Course workflow:** Codespace = disposable laboratory. GitHub fork = permanent course portfolio.

## Reading connection

Sebesta Chapter 15 discusses functional programming primarily through **Lisp and Scheme**, while this course uses **Clojure**. Use Sebesta for the functional-programming concepts; use the Clojure examples and official Clojure documentation for exact syntax and library functions.
