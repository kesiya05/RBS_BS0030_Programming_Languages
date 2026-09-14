# Week 2 Grammar Exercises

Complete this file in your own fork. Short answers are enough, but use the vocabulary from the lecture: **terminal**, **nonterminal**, **production**, **start symbol**, **derivation**, **parse tree**, **AST**, **precedence**, and **associativity**.

The Tiny Language grammar used by the coding task is in [`examples/tiny-language.ebnf`](examples/tiny-language.ebnf).

## 1. Read one production

Consider:

```bnf
<assignment> ::= <identifier> "=" <expression> ";"
```

Identify:

- the nonterminal on the left-hand side;
- the nonterminals on the right-hand side;
- the terminal symbols;
- what `::=` means.

**Your answer:**

The nonterminal on the left side is <assignment>.

The nonterminals on the right side are <identifier> and <expression>.

The terminal symbols are = and ;.

::= basically means “is defined as”. It shows what a nonterminal can be made from.

## 2. Legal or illegal?

Using the Tiny Language grammar, decide whether each program is syntactically valid. For each invalid example, identify the first place where the grammar can no longer match the input.

### A

```text
let radius = 10;
radius * radius
```

**Your answer:** 
Legal. let radius = 10; matches statement, and radius * radius matches the final expression required by program ::= {statement} expression EOF — no trailing semicolon is needed since it's not a statement, it's the program's result.

### B

```text
let radius = ;
radius * radius
```

**Your answer:** 
Illegal. After =, the grammar expects an expression, but there is nothing before the semicolon.

### C

```text
let radius = 10;
let area = 3 * radius * radius;
area + 5
```

**Your answer:** 
Legal. Both let lines match {statement}, and area + 5 is the final expression — same reasoning as A, it's the result, not a statement, so no semicolon after it.

### D

```text
10 + * 5
```

**Your answer:** 
Illegal. After the +, the grammar expects a number or expression, but it gets * instead.

## 3. BNF recursion to EBNF repetition

The following BNF describes one or more comma-separated identifiers:

```bnf
<ident-list> ::= <identifier>
               | <identifier> "," <ident-list>
```

Rewrite the same idea using EBNF repetition.

**Your EBNF:**

```ebnf
<ident-list> = <identifier> { "," <identifier> } ;
```

In one or two sentences, explain why the EBNF is shorter.

**Your answer:**

EBNF is shorter because { } can be used to show repetition. We don't need to write the recursive rule again and again.

## 4. A short derivation

Use this simplified grammar:

```bnf
<expression> ::= <term> "+" <term>
<term>       ::= <number>
<number>     ::= "2" | "3"
```

Write a derivation from `<expression>` to:

```text
2 + 3
```

**Your derivation:**

```text
<expression> → <term> + <term> → <number> + <number> → 2 + <number> → 2 + 3
```

## 5. Parse tree versus AST

For:

```text
2 + 3 * 4
```

Explain why a parse tree contains grammar categories such as `expression`, `term`, and `factor`, while an AST can be reduced to something like:

```text
Add
├── Number(2)
└── Multiply
    ├── Number(3)
    └── Number(4)
```

**Your answer:**

A parse tree shows the full grammar structure, so it includes things like expression, term, and factor. An AST is simpler because it removes unnecessary grammar details and mainly shows the meaning of the expression.
Here, * has higher precedence than +, so 3 * 4 is grouped together first. That's why the AST looks like that.

## 6. Precedence versus associativity

Explain the difference between these two questions:

1. Why is `2 + 3 * 4` normally grouped as `2 + (3 * 4)`?
2. Why is `10 - 3 - 2` normally grouped as `(10 - 3) - 2`?

Use the words **precedence** and **associativity** correctly.

**Your answer:**

2 + 3 * 4 is written as 2 + (3 * 4) because multiplication has higher precedence than addition. So multiplication is done first.
10 - 3 - 2 is written as (10 - 3) - 2 because subtraction has left associativity. This means we group the operations from left to right.
So, precedence decides which operator has priority, while associativity decides the order when operators have the same precedence.