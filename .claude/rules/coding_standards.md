# Java Coding Standards of Safeguard Project

> This document only lists some standards that are often violated.

## 1. Naming and Declaration Standards

### 1.1. Classes (Interfaces, Enums, etc.)

| Type             | Standards                                                    | Example                                                    |
|------------------|--------------------------------------------------------------|------------------------------------------------------------|
| Abstract classes | Don't add the `Abstract` prefix unless there's a good reason | `top.yangguangmc.safeguard.protection.detection.Detection` |

### 1.2 Variables and Constants

| Type            | Standards                                                                                                      | Example                                                                            |
|-----------------|----------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| Log recorders   | Declare only when needed in every class independently. Always use `MOD_ID` as logger name.                     | `private static final Logger LOGGER = LoggerFactory.getLogger(ModContext.MOD_ID);` |
| `final` fields  | Declare as `final` whenever possible                                                                           | `private final int id;`                                                            |
| Local variables | Readability comes the first. Unless brevity is really important or causes confusion, try to use the full name. | `boolean enabled;` instead of `boolean bl;` or `boolean flag;`                     |

## 2. Code Structure Standards

### 2.1. Code Formatting Standards

Note: It is recommended to call IDEA's MCP server to reformat your code (tool `reformat_file`).

#### 2.1.1. `if`/`for`/`while`

- Use curly braces for `if`/`for`/`while`, even for single statements.
    - Don't put the whole `if`/`for`/`while` statement in one single line, like `if (condition) { statement; }`.

#### 2.1.2. Blank Lines

- One blank line in the end of every file.
- You should never use two or more blank lines at once.

## 3. Code Safety and Robustness Standards

### 3.1. Nullabilities

- Use nullability annotations inside JetBrains Annotations library.
- You can assume parameters and return values of `public` and `protected` methods are not null by default, which means
  there's no need to add `@NotNull` annotation intentionally unless there's possibility to cause pollution (such as
  adding the parameter to a collection).
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

## 4. Other Suggestions

- Take full advantage of IDEA's MCP server to get:
    - Problems in project files. (`get_file_problems`)
    - Fast documents of symbols. (`get_symbol_info`)
    - Fast code reformatting. (Highly recommended before commiting every file.) (`reformat_file`)
- Make good use of the tools and skills offered by Context7 and FireCrawl, like:
    - `find-docs` to get latest documents of APIs. (Strongly recommended.)
    - `firecrawl` to perform web search.
