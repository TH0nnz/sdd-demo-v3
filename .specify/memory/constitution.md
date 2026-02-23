<!--
SYNC IMPACT REPORT
==========================================
Version Change: N/A → 1.0.0 (INITIAL RATIFICATION)
Modified Principles: None (initial creation from template)
Added Sections:
  - Core Principles (I. Code Quality, II. Testing Standards,
    III. User Experience Consistency, IV. Performance Requirements)
  - Quality Gates & Compliance
  - Development Workflow
  - Governance
Removed Sections: None
Templates Updated:
  ✅ .specify/memory/constitution.md (this file — filled from template)
  ✅ .specify/templates/plan-template.md — Constitution Check gate is generic; aligns with all 4 principles
  ✅ .specify/templates/spec-template.md — Success Criteria section supports measurable UX/performance outcomes
  ✅ .specify/templates/tasks-template.md — Phase structure accommodates test-first and quality gate tasks
  ✅ .github/prompts/ — No agent-specific (CLAUDE-only) references found; all guidance is generic
Follow-up TODOs: None — all placeholders resolved
==========================================
-->

# sdd-demo-v3 Constitution

## Core Principles

### I. Code Quality (NON-NEGOTIABLE)

Every line of production code MUST meet the following standards:

- Code MUST pass linting and static analysis checks before merge; zero warnings are tolerated in CI.
- Functions MUST be short, focused, and single-responsibility; cyclomatic complexity MUST NOT exceed 10 per function.
- Magic numbers and hardcoded strings MUST be extracted into named constants; no inline literals in logic paths.
- Pull requests MUST be reviewed by at least one peer before merge; all review comments MUST be resolved or explicitly accepted with justification.
- Dead code, commented-out blocks, and untracked TODO stubs MUST NOT be merged; every TODO requires a corresponding tracked issue.

**Rationale**: Consistent code quality reduces cognitive load, accelerates onboarding, and prevents defect accumulation as the codebase scales.

### II. Testing Standards (NON-NEGOTIABLE)

All features MUST follow a test-first development discipline:

- Tests MUST be written and stakeholder-approved before implementation begins (TDD: Red → Green → Refactor).
- Unit test coverage MUST meet or exceed 80% line coverage for all new and modified code.
- Integration tests MUST cover every public API contract, inter-module boundary, and data persistence operation.
- Tests MUST be deterministic; flaky tests MUST be fixed or quarantined within one sprint of detection.
- End-to-end tests MUST cover every P1 user story acceptance scenario defined in the feature specification.

**Rationale**: Testing standards protect against regressions, serve as living specifications, and enforce compliance with the feature contract.

### III. User Experience Consistency (NON-NEGOTIABLE)

All user-facing features MUST adhere to a unified interaction model:

- UI components MUST be drawn from the approved design system; custom one-off components require design review sign-off.
- Error messages MUST follow the format: [context] + [what went wrong] + [what the user can do]; raw stack traces MUST NOT be exposed to end users.
- Navigation flows MUST follow established platform patterns; deviations require a documented UX justification approved before implementation.
- All interactive elements MUST meet WCAG 2.1 AA accessibility standards (contrast ratios, keyboard navigation, ARIA labels).
- Loading, empty, and error states MUST be explicitly designed and implemented for every feature screen or component — they are not optional.

**Rationale**: UX consistency builds user trust, reduces support burden, and ensures the product feels coherent as features accumulate.

### IV. Performance Requirements (NON-NEGOTIABLE)

All features shipped to production MUST meet measurable performance thresholds:

- API endpoint p95 response time MUST be ≤ 200 ms under expected load; p99 MUST be ≤ 500 ms.
- Frontend First Contentful Paint (FCP) MUST be ≤ 1.5 s; Time to Interactive (TTI) MUST be ≤ 3.0 s on a 4G network baseline.
- Database queries introduced by a feature MUST be validated with query analysis tooling (e.g., EXPLAIN ANALYZE); full-table scans are prohibited on tables with more than 10 000 rows without explicit pagination or indexing strategy.
- Memory consumption per feature MUST NOT regress the established baseline by more than 5%.
- Performance benchmarks MUST be run and reported in the PR description for any change touching hot paths.

**Rationale**: Performance is a product feature. Without measurable thresholds, degradation is invisible until it becomes a user-visible crisis.

## Quality Gates & Compliance

All pull requests MUST pass the following gates before merge:

1. **Linting & Formatting**: CI lint check passes with zero warnings.
2. **Test Coverage**: Coverage report confirms ≥ 80% line coverage for all changed files.
3. **Performance Benchmark**: Benchmark results are attached for hot-path changes; no regressions reported against the established baseline.
4. **Constitution Check**: The plan and spec documents explicitly reference and validate against all four core principles.
5. **Accessibility Scan**: Automated accessibility scan passes for any UI-bearing changes.

Non-compliance MUST be documented with a technical justification and must be tracked as a follow-up issue before merge; it MUST NOT be silently accepted as permanent.

## Development Workflow

Feature development MUST follow the speckit workflow in sequence:

1. `/speckit.clarify` — Resolve ambiguities before authoring a specification.
2. `/speckit.specify` — Write a feature spec with user stories and acceptance scenarios.
3. `/speckit.plan` — Produce an implementation plan including a Constitution Check gate against all four principles.
4. `/speckit.tasks` — Decompose the plan into independently testable, deliverable tasks.
5. `/speckit.implement` — Implement tasks under TDD discipline.

All agents and contributors MUST consult `.specify/memory/constitution.md` before beginning plan or implementation work on any feature.

## Governance

- This constitution supersedes all other coding standards and development practices within this repository.
- Amendments require: a written rationale, a version bump per the semantic versioning policy below, and a documented impact assessment on existing active specs and plans.
- **Versioning Policy**:
  - MAJOR — removal or incompatible redefinition of an existing principle.
  - MINOR — addition of a new principle or material expansion of existing guidance.
  - PATCH — clarifications, wording refinements, or typo fixes with no semantic change.
- All PRs and reviews MUST include a Constitution Check confirming compliance with all four core principles.
- Compliance review MUST occur at each sprint retrospective; unresolved violations MUST be assigned as P1 issues before the next sprint begins.

##  Documentation Language:
- All specifications, plans, and user-facing documentation MUST be written in Traditional Chinese (zh-TW)
- Code comments and technical documentation MAY use English for technical clarity
- Commit messages and internal development notes MAY use English

**Version**: 1.0.0 | **Ratified**: 2026-02-23 | **Last Amended**: 2026-02-23
