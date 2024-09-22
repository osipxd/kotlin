# Inference

This document defines and explains a number of terms
that are often used to reason about inner workings of type inference in Kotlin.

A basic description of how inference works might also be added to this document in the future.

## Glossary

### TV — type variable

To avoid confusion with *type parameters*, we often use the following convention:
a type variable `Tv` is based on a type parameter `T`.

### Type constraint
A type relation between a type variable and another type in one of the following forms:
- `Tv <: AnotherType`
- `AnotherType <: Tv`
- `Tv == AnotherType`

### Proper type
A type that doesn't contain any type variables.

### Proper constraint
A constraint that is imposed on a type variable by a proper type.

### Constraint system (CS)
A collection of type variables and constraints imposed on them.

**Source code representation:** `org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl`

### Related type variables

**Source code reference:** `TypeVariableDependencyInformationProvider`

#### Shallow relation
Two type variables `Xv` and `Yv` are **shallowly related** if:
- there exists a type relation chain `Xv ~ T_1 ~ .. ~ T_n ~ Yv` (where `~` is either `<:` or `>:`)

#### Deep relation
Two type variables `Xv` and `Yv` are **deeply related** if:
- there exists a type relation chain `T_1 ~ .. ~ T_n` (where `~` is either `<:` or `>:`)
- `T_1` contains `Xv`
- `T_n` contains `Yv`

### Call tree / Call-tree
A tree of calls where constraint systems for each call are joined and solved (completed) together.

### Postponed atoms
- Lambda argument of a call
- Callable reference argument of a call

### Input types of a postponed atom
- Type of the postponed atom's receiver
- Types of the postponed atom's value parameters

### Completion
A process that tries to infer type variables and analyze lambda arguments
for a given call tree, its constraint system, and its postponed atoms.

### Completion mode
A definition of how actively/forcefully a given call tree should be completed.

**Source code representation:** `ConstraintSystemCompletionMode`

### `FULL` completion mode
Used for call trees on top-statement and receiver positions.

- tries to fix every type variable of the call tree's constraint system
- reports errors if there’s not enough information to fix a type variable
- processes lambdas via [PCLA](pcla.md) if necessary and possible

### `PARTIAL` completion mode
Used for call trees on value-argument positions.

- doesn't try to fix type variables of the call tree's constraint system that are related to the calls' return types
  - (fixation of such type variables depends on the results of overload resolution for the containing call)
- doesn't report errors if there’s not enough information to fix a type variable
  - (such a type variable could be fixed later during `FULL` completion of the top-level containing call tree)
- doesn't process lambdas via [PCLA](pcla.md) (but does process lambdas via regular lambda analysis if possible)
  - (see [the "PCLA entry point" section of pcla.md](pcla.md#pcla-entry-point))

One could argue that the `PARTIAL` mode could or/and should have been removed
in favor of performing a single `FULL` completion of the entire top-level containing call tree;
however, `PARTIAL` completion of value arguments affects overload resolution for containing calls:

```
fun foo(arg: Int) {}
fun foo(arg: Double) {}

fun <R> run(block: () -> R): R = block()

fun example(int: Int) {
    // a candidate for this call cannot be chosen
    // without fixing Rv to Int first
    foo(
        // Rv cannot be fixed to Int
        // without analyzing this call's lambda argument first
        run(
            // PARTIAL completion mode makes it possible to analyze this lambda
            // before a candidate for the `foo` call has to be chosen
            { int }
        )
    )
}
```

### `PCLA_POSTPONED_CALL` completion mode
A modification of `FULL` completion mode.
Used for [PCLA](pcla.md).
See [the "PCLA_POSTPONED_CALL completion mode" section of pcla.md](pcla.md#pcla_postponed_call-completion-mode).

### `UNTIL_FIRST_LAMBDA` completion mode
A modification of `FULL` completion mode that stops after the first lambda analyzed.
Used for overload resolution by a lambda's return type.

### Inference session
A set of callbacks related to inference that are called during function body transformations.

**Source code representation:** `FirInferenceSession`
