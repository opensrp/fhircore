# Client Register — Applicable Care Discovery via PlanDefinition

| Field | Value |
|-------|-------|
| **Status** | Implemented (initial) — pending on-device verification |
| **Repos** | openSRP FHIRCore Android (`android/`) |
| **Related** | `feature/register-tricc.md` Part IV (original design for this mechanism); `tricc_oo` `OpenSRPStrategy` (generates the PlanDefinitions this feature consumes) |
| **Named event (default)** | `available-care` |

Valid status values: `Draft` → `Approved` → `Implemented` → `Superseded`.

---

## Part I — Business spec

### 1. Problem

The client register and client profile need a "Start care" action that shows a patient only the
interventions that are actually **applicable to them** (age, sex, existing conditions, prior
encounters, …), without the app ever hardcoding which interventions exist or what makes them
applicable. Content authors (TRICC) publish that catalog and its eligibility logic as
`PlanDefinition` resources; the app's job is purely mechanical: find the PlanDefinitions tagged for
this button, evaluate them against this one patient, and let the user pick from whatever comes back
applicable.

### 2. Goal

Given a `namedEvent` string (from config, default `available-care`) and a patient, return the list of
currently-applicable interventions for that patient — driven entirely by synced FHIR content, with no
PlanDefinition/Questionnaire IDs known to the app binary.

### 3. Decisions

| Topic | Decision |
|-------|----------|
| Trigger | Any config action (register row `serviceButton`, profile button, …) with `workflow: APPLY_NAMED_EVENT` and a `namedEvent` param. Wired today on both `client_register_config.json` and `client_profile_config.json` ("Start care" button). |
| PlanDefinition discovery | All locally-stored `PlanDefinition`s are searched and filtered to those whose `action.trigger` (recursively, including nested actions) has `type = named-event` and a matching `name`. No separate index/config lists PD ids. |
| Applicability evaluation — fast path | When a matching PlanDefinition's applicable actions only use **FHIRPath** conditions, evaluate them in-process against the `Patient` resource directly (`FHIRPathEngine.evaluateToBoolean`) — no `$apply`, no side effects, no CarePlan generated. |
| Applicability evaluation — CQL path | When any condition uses a non-FHIRPath language (i.e. CQL), fall back to a full `PlanDefinition/$apply` via `WorkflowCarePlanGenerator` to get a correctly-evaluated `RequestGroup`. This path is **read-only** (`persist = false`, see §8): it never writes Task/RequestGroup/CarePlan resources to the local database just from browsing. |
| Strategy vs leaf PlanDefinitions | A "strategy" PD (nested `action.action` under the named-event action, per `register-tricc.md` §8.8) is supported: its child actions are evaluated as the candidate interventions rather than the parent wrapper action. |
| Where the picker lives | A native `AlertDialog` list of intervention titles, shown from the config-action handler — not a config-declared bottom sheet. |
| Selecting an option | If it resolves to a `Questionnaire` id, launch it directly. Otherwise (nested `PlanDefinition` with no direct Questionnaire) show a toast placeholder — full apply-on-select for nested PDs is not yet wired (see Part IV). |

---

## Part II — Technical spec

### 4. Where it lives

| Area | Location |
|------|----------|
| Workflow enum value | `engine/.../configuration/workflow/ApplicationWorkflow.kt` — `APPLY_NAMED_EVENT` |
| Discovery + applicability + option-building | `engine/.../task/NamedEventInterventionService.kt` — `listInterventions()` |
| `$apply` execution (CQL path) | `engine/.../task/WorkflowCarePlanGenerator.kt` — `applyPlanDefinitionOnPatient()` |
| Config action → service call → picker UI | `quest/.../util/extensions/ConfigExtensions.kt` — `handleApplyNamedEvent()`, `launchInterventionOption()` |
| Hilt access from a non-injected extension function | `quest/.../di/NamedEventInterventionEntryPoint.kt` |
| Config wiring (button) | `quest/src/main/assets/configs/{app,cdss}/registers/client_register_config.json`, `.../profiles/client_profile_config.json` — `serviceButton`/button `actions` with `workflow: APPLY_NAMED_EVENT`, `params: [namedEvent, subjectId]` |

### 5. End-to-end flow

```text
Register/profile button (ON_CLICK, workflow=APPLY_NAMED_EVENT,
params: namedEvent="available-care", subjectId=@{patientLogicalId})
        │
        ▼
ConfigExtensions.handleApplyNamedEvent
  - resolves namedEvent (default "available-care") and subjectId from interpolated params
  - requires the nav context to be a LifecycleOwner (bails with a toast otherwise)
  - resolves NamedEventInterventionService via NamedEventInterventionEntryPoint (Hilt EntryPointAccessors)
        │
        ▼
NamedEventInterventionService.listInterventions(namedEvent, subjectId)
  1. fhirEngine.get<Patient>(subjectId)                       -- bail (empty list) if missing
  2. loadPlanDefinitions()                                    -- batchedSearch ALL local PlanDefinitions
  3. filter planDefinitions by hasNamedEventTrigger(namedEvent)   -- recursive over action.trigger
  4. for each matching PlanDefinition, collectFromPlanDefinition:
       actionsWithEvent = top-level actions carrying the trigger
       actionsToEvaluate = actionsWithEvent's children if non-empty (strategy PD), else actionsWithEvent itself (leaf PD)
       needsApply = any actionsToEvaluate condition uses a non-FHIRPath language
       ├─ needsApply == true  → collectFromWorkflowApply(planDefinition, patient, options)
       └─ needsApply == false → evaluate each action.passesFhirPathConditions(patient) in-process;
                                 build InterventionOption per passing action;
                                 if still empty AND the PD has any nested actions, fall back to
                                 collectFromWorkflowApply anyway
        │
        ▼
options: List<InterventionOption> (id, title, description, definitionCanonical, planDefinitionId?, questionnaireId?)
        │
        ▼
AlertDialog picker (titles) — empty list shows a "No care available for this client" toast instead
        │  user picks one
        ▼
launchInterventionOption
  - has questionnaireId  → launchQuestionnaire(QuestionnaireConfig(id = questionnaireId, ...))
  - PlanDefinition only  → toast placeholder + Timber log (not yet wired to launch)
```

### 6. `collectFromWorkflowApply` detail

```text
collectFromWorkflowApply(planDefinition, patient, options)
  - builds a throwaway CarePlan(status=DRAFT, intent=PROPOSAL, subject=patient)  -- "output" scratch object
  - workflowCarePlanGenerator.applyPlanDefinitionOnPatient(planDefinition, patient, data=Bundle(),
      output=carePlan, persist=false)
      → runs the real CQL $apply (FhirOperator / PlanDefinitionProcessor)
      → acceptCarePlan(persist=false) classifies produced request resources
        (Task, QuestionnaireResponse, OperationOutcome, MedicationRequest, CarePlan, RequestGroup)
        WITHOUT writing them via DefaultRepository (see
        WorkflowCarePlanGenerator.createProposedRequestResources's `persist` guard)
      → links resources onto the scratch `output` CarePlan: CarePlan/RequestGroup are added to
        `output.contained` (plus an activity reference); other types get an activity reference only
        — see WorkflowCarePlanGenerator.addRequestResourcesToCarePlanOfRecord
  - reads carePlan.contained.filterIsInstance<RequestGroup>() → action → InterventionOption (preferred)
  - fallback: carePlan.activity entries → InterventionOption (when RequestGroup is empty/absent)
```

Actually starting an intervention (accepting a Questionnaire/PlanDefinition from the picker) is a
separate step from discovery and is expected to persist normally — call sites that generate the
CarePlan of record for real (e.g. `FhirCarePlanGenerator`) still use the `persist = true` default.

### 7. `PlanDefinitionActionComponent.toInterventionOption` / `RequestGroupActionComponent.toInterventionOption`

Both extract `title` (or `description`/PD title/name as fallback), and parse `definitionCanonical` /
`resource.reference` to pull out a `Questionnaire/{id}` or `PlanDefinition/{id}` substring into
`questionnaireId` / `planDefinitionId` respectively, via `extractLogicalIdUuid()`.

### 8. Known behavior worth flagging

- **Fixed:** the CQL/`$apply` path used to have side effects even though it's just a "browse"
  operation — `WorkflowCarePlanGenerator.applyPlanDefinitionOnPatient` unconditionally persisted any
  Task, QuestionnaireResponse, OperationOutcome, MedicationRequest, CarePlan, and RequestGroup
  produced by `$apply`, so every "Start care" tap against a CQL-conditioned PlanDefinition wrote
  duplicate resources to the local DB regardless of whether the user selected anything. This is now
  gated behind a `persist: Boolean = true` parameter on `applyPlanDefinitionOnPatient` (threaded
  through `acceptCarePlan`/`createProposedRequestResources`); `NamedEventInterventionService` calls it
  with `persist = false`, so discovery is read-only. Real CarePlan-generation call sites (e.g.
  `FhirCarePlanGenerator`) keep the `persist = true` default, so accepted/actioned CarePlans still
  persist normally.
- **Also fixed as part of the same change:** `addRequestResourcesToCarePlanOfRecord`'s `when` had no
  case for `"CarePlan"`/`"RequestGroup"`, so it fell through to `else -> TODO(...)` and threw
  `NotImplementedError` whenever `$apply` produced a `RequestGroup` — which is the expected/primary
  output for this feature. That exception was silently swallowed by the `runCatching` in
  `collectFromWorkflowApply`, so the CQL path always appeared to return zero interventions (after
  having already persisted the duplicate). `CarePlan`/`RequestGroup` resources are now added to
  `carePlan.contained` (plus an activity reference), which is what lets
  `carePlan.contained.filterIsInstance<RequestGroup>()` actually find anything.
- The fast FHIRPath path still has no side effects (pure read), which is why `needsApply` is
  short-circuited to it whenever possible.
- Nested-PlanDefinition selections (no direct `Questionnaire`) are not fully wired to a launch action
  yet — see the TODO in `launchInterventionOption` / Part IV of `register-tricc.md` §8.4.

### 9. Success criteria

1. Tapping "Start care" on a client register row or profile shows only interventions applicable to
   that specific patient, computed from currently-synced PlanDefinitions.
2. Publishing a new `available-care`-triggered PlanDefinition (+ Questionnaire) and syncing makes it
   appear for matching clients with no app release.
3. A patient with no applicable interventions sees a clear "No care available" message rather than an
   empty/broken dialog.
4. Selecting an intervention backed by a Questionnaire launches that Questionnaire.

### 10. Open items

- Not yet verified on-device — confirm PlanDefinition discovery, FHIRPath fast-path evaluation, and
  the CQL `$apply` fallback all behave correctly against real synced TRICC content on a
  physical device/emulator (this mirrors the open item in `feature/cql-initial-expression.md`). In
  particular, verify the `persist = false` read-only path (§8) against a real CQL-conditioned
  PlanDefinition on-device, since it was only reasoned through statically, not yet exercised with a
  running `FhirOperator`/`PlanDefinitionProcessor`.
- Wire "select a nested PlanDefinition option" to actually apply/launch it, instead of the current
  toast placeholder — that step should call `applyPlanDefinitionOnPatient` with `persist = true` (or
  an explicit accept step) so the chosen intervention's resources are actually saved.
