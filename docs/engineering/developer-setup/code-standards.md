# Code Standards

## Naming Conventions

* When using abbreviations in CamelCase, keep the first letter capitalized and lowercase the remaining letters. For example
  * A "BMI Thresholds" variable name must be written as `bmiThresholds`
  * An "ANC Patient Repository" class name must be written as `AncPatientRespository`

## Branch Naming

Create a new branch using the following naming convention:

```
issue-number-feature-name
```

For example:

```
238-fix-login-page-styling
```

## Commit Messages

Here are some guidelines when writing a commit message:

1. Separate subject/title from body with a blank line
2. Limit the subject line to 50 characters
3. Capitalize the subject line/Title
4. Do not end the subject line with a period
5. Use hyphens at the beginning of the commit messages in the body
6. Use the imperative mood in the commit messages
7. Wrap the body at 72 characters
8. Use the body to explain what and why vs. how

**Sample commit message:**

```
Implement Login functionality

- Add login page view
- Implement authentication and credentials management
- Add tests for login implementation
- Fix sync bug causing crash on install
```
