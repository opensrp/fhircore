# CQL `initialExpression` Population on Questionnaires

| Field | Value |
|-------|-------|
| **Status** | Implemented (pending on-device verification) |
| **Repos** | openSRP FHIRCore Android (`android/`) |
| **Related** | `feature/register-tricc.md` (PlanDefinition `$apply` / CQL usage precedent) |

Valid status values: `Draft` → `Approved` → `Implemented` → `Superseded`.

---

## Part I — Business spec

### 1. Problem

SDC Questionnaires support `item.extension` `initialExpression` to pre-populate an answer from a
computed expression instead of a static `initial` value. TRICC-generated content expresses these
computations in **CQL** (`language = text/cql-identifier` or `text/cql`), referencing `define`
statements in a `Library` linked to the Questionnaire via a `cqf-library` extension. Before this
change, the app only evaluated FHIRPath-based `initialExpression`s (via the Android FHIR SDK's
built-in `ResourceMapper.populate`) — CQL-based ones were silently ignored, so any TRICC form relying
on a CQL default (e.g. a computed BMI, a derived risk flag, a value copied from an earlier
encounter) opened with that field blank.

### 2. Goal

When a Questionnaire declares one or more `cqf-library` extensions, evaluate any CQL
`initialExpression` items against that library before the SDC library populates the
`QuestionnaireResponse`, so CQL-derived defaults appear exactly like any other prepopulated answer.

### 3. Decisions

| Topic | Decision |
|-------|----------|
| Supported languages | `text/cql-identifier` and `text/cql` only (FHIRPath `initialExpression` continues to go through the SDC library unchanged). |
| `cqf-library` extension value types | Support `valueCanonical`, `valueUri`, and `valueString` (TRICC/authoring tools are inconsistent about which type they emit). |
| When to run | Only when opening a **new** response — skipped when reopening an editable/read-only/summary/draft response, so previously saved answers are never overwritten by a fresh CQL evaluation. |
| Evaluation context | `FhirOperator.evaluateLibrary` per distinct library URL, with the launch-context subject resource as the CQL context resource, and `patient`/`encounterid` passed as CQL parameters when those resources are present in the launch context. |
| Conflict with SDC library | `initial` and `initialExpression` cannot coexist on the same item per the SDC populate contract, so once a CQL value is resolved, the `initialExpression` extension is removed from the item and replaced with a plain `initial` value carrying the CQL result. |
| Failure handling | Per-library evaluation failures are caught and logged (`Timber.e`); they do not block loading the rest of the questionnaire or other libraries' expressions. |

---

## Part II — Technical spec

### 4. Where it lives

| Area | Location |
|------|----------|
| `cqf-library` value extraction (multi-type) | `engine/src/main/java/org/smartregister/fhircore/engine/util/extension/QuestionnaireExtension.kt` — `cqfLibraryIds()`, `cqfLibraryUrls()`, private `Extension.cqfLibraryValue()` |
| CQL initial-expression evaluation | `quest/src/main/java/org/smartregister/fhircore/quest/ui/questionnaire/QuestionnaireViewModel.kt` — `evaluateCqlInitialExpressions`, `collectCqlInitialExpressionItems`, `applyCqlExpressionResultsToInitial` |
| Call site | `QuestionnaireViewModel` questionnaire-loading path, invoked before `fetchRepositoryQuestionnaireResponse`/populate, gated by a `willLoadSavedResponse` check |
| Tests | `quest/src/test/java/org/smartregister/fhircore/quest/ui/questionnaire/QuestionnaireViewModelTest.kt` |

### 5. Flow

```text
Open questionnaire
        │
        ▼
questionnaire.cqfLibraryUrls()  -- any cqf-library extensions?
        │ none                         │ one or more
        ▼                              ▼
  (unchanged SDC populate path)   collectCqlInitialExpressionItems()
                                        -- walk item tree, gather distinct
                                           CQL initialExpression strings
                                        │
                                        ▼
                                  build data Bundle from launchContextResources
                                  + Parameters (patient / encounterid, when present)
                                        │
                                        ▼
                                  for each distinct library URL:
                                    fhirOperator.evaluateLibrary(url, subjectRef,
                                        inputParameters, dataBundle, expressionSet)
                                        │
                                        ▼
                                  applyCqlExpressionResultsToInitial()
                                    -- for each item whose initialExpression
                                       matches a returned Parameters entry:
                                       remove initialExpression extension,
                                       set item.initial = [resultValue]
                                        │
                                        ▼
                                  ResourceMapper.populate() runs normally,
                                  now seeding these items from `initial`
```

### 6. Notable implementation details

- The CQL subject resource is chosen by matching `questionnaire.subjectType` against the
  `launchContextResources` list, falling back to the first launch-context resource if no type match
  is found.
- Only items whose `initialExpression.language` is in `{text/cql-identifier, text/cql}` are
  collected/evaluated; FHIRPath and other languages pass through untouched.
- `Parameters.getParameter(exprName)` results may come back as either `.value` or `.resource`
  depending on the CQL return type — both are checked.
- Item tree walking is recursive (`item.item`) so nested groups are covered.

### 7. Success criteria

1. A Questionnaire with a CQL `initialExpression` referencing a linked `Library` opens with that
   field pre-filled from the CQL evaluation result.
2. Reopening a previously saved (editable/read-only/summary/draft) response does not re-run CQL
   evaluation or overwrite the saved answer.
3. A Questionnaire with no `cqf-library` extension is unaffected (no behavior change, no performance
   cost).
4. A failure evaluating one library does not prevent the questionnaire from opening or other
   libraries' expressions from being evaluated.

### 8. Open items

- Not yet verified on-device (unit-tested only) — confirm real TRICC-generated Library/CQL content
  populates as expected on a physical device/emulator, including timing relative to CarePlan/Task
  launch-context loading.
