# Java Coding Standards of Safeguard Project

> This document only lists some standards that are often violated or forgotten.

## 1. Naming and Declaration Standards

### 1.1. Classes (Interfaces, Enums, etc.)

| Type             | Standards                                                        | Example                                                    |
|------------------|------------------------------------------------------------------|------------------------------------------------------------|
| Abstract classes | **Don't** add the `Abstract` prefix unless there's a good reason | `top.yangguangmc.safeguard.protection.detection.Detection` |

### 1.2 Variables and Constants

| Type            | Standards                                                                                                      | Example                                                                            |
|-----------------|----------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| Log recorders   | Declare only when needed in every class independently. Always use `MOD_ID` as logger name.                     | `private static final Logger LOGGER = LoggerFactory.getLogger(ModContext.MOD_ID);` |
| `final` fields  | Declare as `final` whenever possible                                                                           | `private final int id;`                                                            |
| Local variables | Readability comes the first. Unless brevity is really important or causes confusion, try to use the full name. | `boolean enabled;` instead of `boolean bl;` or `boolean flag;`                     |

## 2. Code Structure Standards

### 2.1. Code Formatting Standards

#### 2.1.1. `if`/`for`/`while`

- Use curly braces for `if`/`for`/`while`, even for single statements.
    - **Don't** put the whole `if`/`for`/`while` statement in one single line, like `if (condition) { statement; }`.

#### 2.1.2. Blank Lines

- One blank line in the end of every file.
- You should never use two or more blank lines at once.

## 3. Code Safety and Robustness Standards

### 3.1. Nullabilities

- Use nullability annotations inside **JetBrains Annotations** library.
- You can assume parameters and return values of `public` and `protected` methods are not null by default, which means
  there's no need to add `@NotNull` annotation intentionally **unless there's possibility to cause heap pollution**
  (such as adding the parameter to a collection).
    - In that case, add `@NotNull` or call `Objects.requireNonNull()`.
- However, it is necessary to add `@Nullable` to nullable parameters and return values of `public` and `protected`
  methods, unless the return values are sure not to be null in some cases so that adding `@Nullable` will result in
  plenty of warnings by IDEA.
    - For example,`top.yangguangmc.safeguard.protection.SwitchTreeNode.getNode` and
      `top.yangguangmc.safeguard.protection.SwitchTreeNode.getParent` can return null theoretically, but there's
      obviously no need and annoying to mark them as `@Nullable`.
    - Best practice: If you're not sure, just don't add `@Nullable` and let the user decide.

### 3.2. Utility-like Classes

- Use `private` constructor + `throw new AsserationError();` defense.
- It's not recommend to make the class `final` since it has already had `private` constructors.

## 4. Tool Usage Requirements

The tools listed below are ***first-choice tools**, not optional helpers. Whenever one of them can do the job, you must
prefer it over invoking command-line equivalents. Only use the command-line equivalents if the former ones encounter
problems.

### 4.1. LSP and IDEA MCP Server

The lsp tool has the highest priority.

Then, prefer IDEA's MCP server tools over shell commands (such as `javac`) for IDE-level work:

- Problems in project files. (`get_file_problems`)
- Fast documents of symbols. (`get_symbol_info`)
- Fast code reformatting. (**Mandatory** before committing every changed file.) (`reformat_file`)
- Semantic searches (files, symbols, text, directory trees), fast and stable refactoring (Renaming classes, fields,
  etc.) and debugging are also available via the MCP server. Use them whenever possible.

Note: Depending on the client, tool names may carry a prefix (e.g. `idea_get_file_problems` in OpenCode,
`mcp__idea__get_file_problems` in Claude Code).

### 4.2. Context7 and FireCrawl Skills

- `find-docs` — the **default way** to get latest documents of APIs. Do not rely on training-data memory or hand-rolled
  requests for library documentation. If this method can't work, fallback to:
    1. source code in the `contexts/sources/` folder;
    2. commandlines that exact JARs in this computer to get source code, or web searches;
    3. ask the user to provide information you need;
    4. existing training-data.
- `firecrawl` — the **default way** to perform web search and page scraping. Prefer it over generic web tools or
  command-line HTTP requests.
