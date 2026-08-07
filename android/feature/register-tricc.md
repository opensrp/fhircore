# Flexible Client Register + Dynamic Interventions (TRICC / openSRP)

| Field | Value |
|-------|-------|
| **Status** | Approved |
| **Repos** | openSRP FHIRCore Android (`android/`), TRICC OpenSRPStrategy (`tricc_oo/`) |
| **Related** | `tricc_oo/feature/opensrp-register.md`, `tricc_oo/docs/desing/FHIRcore.md`, `tricc_oo/docs/open-srp-export.md` |
| **Named event (default)** | `available-care` |

Valid status values: `Draft` → `Approved` → `Implemented` → `Superseded`.

---

# Part I — Goals and product model

## 1. Overview

TRICC generates clinical decision support content (Questionnaires, PlanDefinitions, Libraries, StructureMaps) for openSRP / FHIRCore. The mobile app needs a **flexible client experience** that:

1. Shows **all clients on a single register** (Patient-based). **No separate household TRICC register.**
2. On **selecting a client (profile)**, shows **related persons**: parents/guardians and/or children, via RelatedPerson.
3. Starts **interventions without hardcoding** form or PlanDefinition IDs: the app only knows a **named-event**; synced PlanDefinitions drive eligibility and launch targets.
4. Can **add a RelatedPerson** with **`RelatedPerson.patient` always = the child** (simplifies authoring and joins).

Delivery order: **Android-first** (register UI + workflow). TRICC export updates follow a fixed content contract (Part IV).

## 2. Clinical problem

Current openSRP sample registers are **siloed** (child, ANC, household Group, …) and wire **fixed** questionnaire / planDefinition IDs in JSON. That works for static programs but not for TRICC, where:

- New interventions appear when content is published and synced.
- Eligibility is **per client** (under-5, women of reproductive age, prior conditions, …).
- Family links are **person-centric**: mother, father, and guardian are themselves **clients**, not RelatedPerson-only shadows.

## 3. Decisions

| Topic | Decision |
|--------|----------|
| Registers | **One** primary TRICC register: **All clients**. Not “All clients” + “Household TRICC”. |
| Mother / father / guardian | **Always full clients (`Patient`)**. Never RelatedPerson-only people. |
| Where relations appear | **Client profile** after selection (parents/guardians and children). Optional light hint on register row. |
| Link model | **`RelatedPerson` only**; **`patient` always = child** (see Part II). |
| Add relation | Questionnaire / flow creates RelatedPerson with subject = **child**; parent/guardian is Patient + `identifier` PI link. |
| Legacy household Group | Optional for non-TRICC flavors only; not required for TRICC. |
| Intervention discovery | PlanDefinition **`$apply`** for PDs with a given **named-event**. |
| App hardcoding | Only the **named-event** string (and generic workflow). |

---

# Part II — Client and relationship model

## 4. Rules

1. Every mother, father, or guardian is a **`Patient`** and appears on the client register.
2. Every dependent child is a **`Patient`**.
3. The relationship is a **`RelatedPerson`** resource:
   - `RelatedPerson.patient` → **child** Patient  
   - `RelatedPerson.relationship` → mother | father | guardian (standard RoleCode / RoleClass, e.g. `MTH`, `FTH`, [`GUARD`](http://terminology.hl7.org/CodeSystem/v3-RoleClass#GUARD))  
   - `RelatedPerson.identifier` → **Patient URL** of the mother/father/guardian **as a registered client** ([RelatedPerson.identifier](https://build.fhir.org/relatedperson-definitions.html#RelatedPerson.identifier))

No custom extension is required. **RelatedPerson is the source of truth** for mother / father / guardian links.

Household `Group` is **not** required for the TRICC flexible register (may still exist for legacy apps).

## 5. RelatedPerson shape (guardian is also Patient)

```json
{
  "resourceType": "RelatedPerson",
  "id": "rp-mother-of-jean",
  "identifier": [{
    "use": "secondary",
    "type": {
      "coding": [{
        "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
        "code": "PI",
        "display": "Patient internal identifier"
      }]
    },
    "system": "urn:ietf:rfc:3986",
    "value": "Patient/marie"
  }],
  "patient": { "reference": "Patient/jean" },
  "relationship": [{
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/v3-RoleCode",
      "code": "MTH",
      "display": "mother"
    }]
  }]
}
```

| Element | Meaning |
|---------|---------|
| `patient` | Child (subject of care the related person is related **to**) |
| `relationship` | Role toward the child (`MTH` / `FTH` / `GUARD`, etc.) |
| `identifier` | Who this related person **is** as a client: Patient relative ref or absolute URL |

**Identifier convention (Patient client link)**

| Field | Recommended | Notes |
|-------|-------------|--------|
| `type` | **`PI`** (v2-0203 Patient *internal* identifier) | Marks this id as “the Patient in *this* system”. Prefer **PI** over **PT** (external identifier) when `value` is our own `Patient/{id}`. **PT** is still accepted when reading. |
| `use` | **`secondary`** | Structural join key so **`official` / `usual`** stay free for national ID, openSRP ID, phone, etc. on the same RelatedPerson. |
| `system` | `urn:ietf:rfc:3986` | Value is a URI (relative `Patient/{id}` or absolute Patient URL). |
| `value` | `Patient/{id}` or absolute Patient URL | e.g. `Patient/marie` or `https://fhir.example.org/Patient/marie` |

**Why not `use: official` or `usual` by default?**

| `use` | Fit for Patient-link identifier |
|-------|----------------------------------|
| **`secondary`** (recommended) | Linkage / join only; does not compete with the person’s civil or clinical identifiers. |
| `usual` | Reasonable if this is *the* id UIs always show for “linked client” and there is no separate display id on RelatedPerson. |
| `official` | Better for national ID / legal identifier on the person — not for a FHIR resource URL. |
| `temp` / `old` | Avoid for the active Patient link. |

`GUARD` / `MTH` / `FTH` answer *what role*; this `identifier` answers *which Patient client is that person*. They are complementary.

### 5.1 Nesting under guardian Patient `P`

1. Find RelatedPersons whose `identifier` resolves to `Patient/P` (Patient URL / ref).
2. For each, resolve `RelatedPerson.patient` as a nested child row.
3. Show relationship label from `RelatedPerson.relationship`.

### 5.2 Forward view under child Patient `C`

- RelatedPersons with `patient=C` (standard reverse-include `RelatedPerson?patient=`) for “has mother/father/guardian” labels on profile.

### 5.3 Register vs profile

| Screen | Behaviour |
|--------|-----------|
| **All clients register** | Flat list of all Patients. Tap → client profile. Optional “Has parent/guardian” hint. Start care may stay on the row. |
| **Client profile** | Selected person + **Children / dependents** (if parent) + **Parents / guardians** (if child) + **Add related person**. |

No second “household” register: family context is the profile’s related-person sections.

### 5.4 Add RelatedPerson (subject = always the child)

Invariant for every new relationship row:

```text
RelatedPerson.patient = Patient/{childId}     // always the child
RelatedPerson.relationship = MTH | FTH | GUARD
RelatedPerson.identifier (PI, secondary) = Patient/{parentOrGuardianId}
```

| User is viewing | “Add related person” means |
|-----------------|----------------------------|
| **Child** profile | Link/create mother, father, or guardian Patient; RP.patient = this child. |
| **Adult** profile | Link/create a child Patient; RP.patient = **new/selected child**; identifier → this adult. |

Questionnaire/StructureMap should never set `RelatedPerson.patient` to the parent.

### 5.5 Relationship codes

Prefer HL7 v3 RoleCode / RoleClass for interoperability:

| Role | Code system | Code |
|------|-------------|------|
| Mother | `http://terminology.hl7.org/CodeSystem/v3-RoleCode` | `MTH` |
| Father | same | `FTH` |
| Guardian | `http://terminology.hl7.org/CodeSystem/v3-RoleClass` (or RoleCode) | `GUARD` |

Config rules map codes → display strings / icons.

---

# Part III — Register packaging and UI

## 6. Packaging (single client register + profile)

```text
configs/app/
  registers/
    client/
      client_register_config.json    → id clientRegister
  profiles/
    client/
      client_profile_config.json     → id clientProfile
```

| Config | Role |
|--------|------|
| `clientRegister` | Flat list of all Patients |
| `clientProfile` | Relations (children + parents/guardians), Start care, Add related person |

Legacy sample Group **household** register may remain for non-TRICC demos; it is **not** part of the TRICC product path.

### 6.1 UX flow

```text
All clients register
        │ tap client
        ▼
Client profile
  ├── Start care  → APPLY_NAMED_EVENT (available-care)
  ├── Children / dependents   (if this Patient is mother/father/guardian)
  ├── Parents / guardians     (if RelatedPerson.patient = this Patient)
  └── Add related person      (RP.patient always = child)
```

## 7. Nested LIST on register rows

Profile screens already populate nested lists via `RulesExecutor.processListResourceData` → `ResourceData.listResourceDataMap`.

Register rows currently only run `processResourceData` (no `listResourceDataMap`). **Required engine change:** when register card views include `ViewType.LIST`, process list resources the same way as profile and attach `listResourceDataMap` so `List.kt` can render nested kids.

### 7.1 FHIR resource config sketch

```json
"fhirResource": {
  "baseResource": {
    "resource": "Patient",
    "sortConfigs": [
      { "paramName": "_lastUpdated", "dataType": "DATE", "order": "DESCENDING" }
    ]
  },
  "relatedResources": [
    {
      "id": "relatedPersons",
      "resource": "RelatedPerson",
      "searchParameter": "patient"
    },
    {
      "id": "tasks",
      "resource": "Task",
      "searchParameter": "subject"
    },
    {
      "id": "carePlans",
      "resource": "CarePlan",
      "searchParameter": "subject"
    }
  ]
}
```

Nesting under a guardian requires RelatedPersons whose `identifier` holds that guardian’s Patient URL (Part II). Repository currently loads RelatedPersons and joins in memory when Search cannot filter efficiently by identifier value.

### 7.2 Nested LIST view sketch

```json
{
  "viewType": "LIST",
  "id": "dependentChildren",
  "resources": [
    {
      "id": "children",
      "resourceType": "Patient",
      "relatedResourceId": "dependentChildPatients",
      "relatedResources": [
        {
          "resourceType": "RelatedPerson",
          "fhirPathExpression": "RelatedPerson.patient.reference"
        }
      ]
    }
  ],
  "registerCard": {
    "rules": [ /* child name, age, relationship label */ ],
    "views": [ /* compact SERVICE_CARD + Start care button */ ]
  }
}
```

Exact `relatedResourceId` / fact keys depend on how rules materialize “children of this guardian” into the facts map (filter RelatedPersons by Patient URL identifier, then load Patient resources).

---

# Part IV — Dynamic interventions

## 8. Architecture

```text
Register / profile card button
        │
        │  workflow APPLY_NAMED_EVENT
        │  param namedEvent = "available-care"
        ▼
NamedEventInterventionService
        │  1. Discover PlanDefinition(s) with named-event trigger (synced)
        │  2. $apply for subject Patient  (conditions evaluated here)
        │  3. Collect RequestGroup.action → InterventionOption list
        ▼
Bottom sheet / dialog (picker)
        │
        ▼
Selected definitionCanonical
        ├── Questionnaire → LAUNCH_QUESTIONNAIRE
        └── PlanDefinition → $apply / open resulting Tasks
```

### 8.1 Why `$apply` (not hand-rolled condition evaluation)

Applicability is authored on PlanDefinition actions (FHIRPath and/or CQL). Reimplementing that in app rules would:

- Duplicate TRICC / CPG logic.
- Drift from the engine used for CarePlan generation.
- Force app releases when eligibility rules change.

`$apply` is the compute path that already exists (`FhirOperator` / `PlanDefinitionProcessor` / `WorkflowCarePlanGenerator`). The app stays ignorant of which interventions exist and of their conditions.

### 8.2 Named-event contract

| Item | Value |
|------|--------|
| Default event name | `available-care` |
| Where configured | Register/profile action `params` only |
| PlanDefinition trigger | `action.trigger[].type = "named-event"`, `name = "available-care"` |

App binary does **not** list intervention IDs.

### 8.3 ApplicationWorkflow

```kotlin
APPLY_NAMED_EVENT
```

Example action:

```json
{
  "trigger": "ON_CLICK",
  "workflow": "APPLY_NAMED_EVENT",
  "display": "Start care",
  "params": [
    { "paramType": "PARAMDATA", "key": "namedEvent", "value": "available-care" },
    { "paramType": "PARAMDATA", "key": "subjectId", "value": "@{patientLogicalId}" }
  ]
}
```

### 8.4 NamedEventInterventionService

1. **Discover** active PlanDefinitions whose actions have `trigger.type=named-event` and `trigger.name` matching the param (local FHIR Engine / KnowledgeManager after sync).  
   - Prefer a single **strategy / clinical-protocol** PD that nests child recommendations if present.  
   - Else `$apply` each leaf PD with that named-event and merge applicable results.
2. **`$apply`** for subject Patient (+ context Bundle as needed).
3. **Parse recommendations** from apply output:
   - Prefer `CarePlan.contained` **RequestGroup** → `action` (`title`, `description`, `resource` / definition canonicals).
   - Fallback: CarePlan activities / Tasks if RequestGroup is empty (degraded UX).
4. Return `InterventionOption(id, title, description, definitionCanonical, planDefinitionId?, questionnaireId?)`.

### 8.5 RequestGroup persistence

`WorkflowCarePlanGenerator` currently ignores RequestGroup (`"RequestGroup" -> {}`). Implementation must **create/store** RequestGroup resources produced by `$apply` so recommendations can be displayed and audited.

### 8.6 Performance

- Run `$apply` **on button click**, never for every register row on scroll.
- Prefer one **strategy PlanDefinition** (single apply evaluates all nested conditions) when TRICC emits it.
- Optional short-lived in-memory cache for open picker only.

### 8.7 Example leaf PlanDefinition (synced)

```json
{
  "resourceType": "PlanDefinition",
  "id": "etat-triage-PD",
  "status": "active",
  "action": [{
    "title": "ETAT Triage",
    "trigger": [{ "type": "named-event", "name": "available-care" }],
    "condition": [{
      "kind": "applicability",
      "expression": {
        "language": "text/fhirpath",
        "expression": "Patient.birthDate >= today() - 5 years"
      }
    }],
    "definitionCanonical": "https://fhir.tricc.io/Questionnaire/etat-triage"
  }]
}
```

### 8.8 Example strategy PlanDefinition (preferred for compute)

```json
{
  "resourceType": "PlanDefinition",
  "id": "available-care-catalog",
  "status": "active",
  "type": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/plan-definition-type",
      "code": "clinical-protocol"
    }]
  },
  "action": [{
    "title": "Available care",
    "trigger": [{ "type": "named-event", "name": "available-care" }],
    "selectionBehavior": "at-most-one",
    "action": [
      {
        "title": "ETAT Triage",
        "condition": [{
          "kind": "applicability",
          "expression": {
            "language": "text/fhirpath",
            "expression": "Patient.birthDate >= today() - 5 years"
          }
        }],
        "definitionCanonical": "https://fhir.tricc.io/PlanDefinition/etat-triage-PD"
      },
      {
        "title": "Adult assessment",
        "condition": [{
          "kind": "applicability",
          "expression": {
            "language": "text/fhirpath",
            "expression": "Patient.birthDate < today() - 15 years"
          }
        }],
        "definitionCanonical": "https://fhir.tricc.io/PlanDefinition/adult-assessment-PD"
      }
    ]
  }]
}
```

`$apply` of the strategy PD yields a RequestGroup with only **applicable** child actions.

---

# Part V — Hardcoded vs synced

| Concern | Source |
|---------|--------|
| Named-event name | Register/profile config only |
| Intervention catalog | Synced PlanDefinitions |
| Eligibility (age, sex, conditions) | PD `action.condition` via `$apply` |
| Forms | Synced Questionnaires referenced by PD |
| Client list shape | Register config (Patient + RelatedPerson nest) |
| Mother/father/guardian as clients | Registration data model + StructureMaps |
| Relationship codes / labels | Data + display rules in config |

**Success test:** publishing a new intervention PD + Questionnaire and syncing the device makes the intervention appear for matching clients **without an app release**.

---

# Part VI — TRICC OpenSRPStrategy contract (WP5)

Implemented in **tricc_oo** — see **`tricc_oo/feature/opensrp-register.md`**.

| Item | Status |
|------|--------|
| Leaf PDs: process trigger **+** `available-care` | Yes (`OpenSRPStrategy.generate_plandefinition`) |
| Strategy PD `{form_id}-available-care-catalog` | Yes (`generate_available_care_catalog`) |
| RelatedPerson helpers (`PI`, patient=child) | Yes (`converters/fhir/related_person.py`) |
| Binary config: named_events, catalog id, contract | Yes |
| Contract JSON under `contract/` | Yes |
| Full StructureMap auto-extraction for add-related-person form | Future (hints emitted) |

Key files:

| Area | Path |
|------|------|
| Feature design | `tricc_oo/feature/opensrp-register.md` |
| OpenSRP export | `tricc_oo/strategies/output/opensrp.py` |
| RelatedPerson helpers | `tricc_oo/converters/fhir/related_person.py` |
| Specs | `tricc_oo/docs/desing/FHIRcore.md`, `tricc_oo/docs/open-srp-export.md` |
| Tests | `tricc_oo/tests/test_strategies/test_opensrp_strategy.py` |

---

# Part VII — Android implementation work packages

| WP | Scope | Status |
|----|--------|--------|
| **WP0** | This document (`feature/register-tricc.md`) | Done |
| **WP1** | Client (+ household TRICC) register config; `listResourceDataMap` on register path; RelatedPerson nesting | Done (initial) |
| **WP2** | `APPLY_NAMED_EVENT`, `NamedEventInterventionService`, RequestGroup persist, picker UI | Done (initial) |
| **WP3** | Wire Start care on top-level and nested cards; profile parity | Done (initial) |
| **WP4** | Sync / KnowledgeManager smoke path for PlanDefinitions offline `$apply` | Pending |
| **WP5** | TRICC export + registration RelatedPerson contract (`tricc_oo/feature/opensrp-register.md`) | Done (initial) |

### Key Android files

| Area | Paths |
|------|--------|
| Register configs | `quest/src/main/assets/configs/app/registers/` |
| Nav / composition | `navigation_config.json`, `composition_config.json` |
| Register data | `quest/.../register/RegisterViewModel.kt`, `RegisterPagingSource.kt` |
| List rules | `engine/.../rulesengine/RulesExecutor.kt`, `quest/.../shared/components/List.kt` |
| Workflows | `engine/.../workflow/ApplicationWorkflow.kt`, `quest/.../ConfigExtensions.kt` |
| Apply / RequestGroup | `engine/.../task/WorkflowCarePlanGenerator.kt`, `FhirCarePlanGenerator.kt` |

---

# Part VIII — Migration from sample household Group register

| Sample (today) | TRICC target |
|----------------|--------------|
| Base `Group` (household) + household register | **One** Patient register + **profile** for family links |
| Members via Group.member | RelatedPerson (`patient`=child, `identifier`=parent Patient URL) |
| Separate disease registers | Optional; not required for TRICC core path |
| Fixed questionnaire ids | `APPLY_NAMED_EVENT` + synced PDs |

Do **not** add a second “Household TRICC” register that duplicates All clients.

---

# Part IX — Open questions

1. Whether Search DSL can filter RelatedPerson by `identifier` (Patient URL) efficiently offline; if not, in-memory join after loading candidates (current approach).
2. CQL vs FHIRPath for applicability on first TRICC content wave (engine support matrix).
3. Whether nested children should be **hidden** from top-level list (config flag) after field feedback.
4. Composition Binary folder layout vs flat ids for assets loader in this fork.

---

# Part X — Success criteria

1. **One** All clients register; selecting a client opens profile with parents/guardians and/or children.
2. RelatedPerson always has `patient` = child; parent/guardian linked via `identifier` (PI) + relationship codes.
3. Can add RelatedPerson from profile (subject = child).
4. Client Start care opens only **applicable** interventions without app knowing PD IDs.
5. New synced intervention PD appears when conditions match — no app release for listing.

---

# Part XI — Implementation notes (landed in android)

| Area | Location |
|------|----------|
| Design | `feature/register-tricc.md` |
| RelatedPerson helpers | `engine/.../util/extension/RelatedPersonAsPatient.kt` (guardian via `identifier` Patient URL) |
| Dependent enrichment | `RegisterRepository.enrichDependentChildrenFromRelatedPersons` |
| Register LIST processing | `RulesExecutor.processResourceDataWithLists`, `RegisterPagingSource` |
| Workflow | `ApplicationWorkflow.APPLY_NAMED_EVENT` |
| Intervention service | `NamedEventInterventionService` |
| Click handler / picker | `ConfigExtensions.handleApplyNamedEvent` |
| RequestGroup persist | `WorkflowCarePlanGenerator` |
| Configs | `registers/client/client_register_config.json`, `profiles/client/client_profile_config.json` |
| Nav | `navigation_config.json` — **All clients** (no separate Household TRICC) |

**Config asset keys** come from the filename before `_config` (camelCase), not the folder name. Folders document packaging only.
