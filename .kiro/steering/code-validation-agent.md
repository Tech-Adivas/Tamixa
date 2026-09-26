---
inclusion: manual
---

# Code Validation Agent

When validating code changes for a **particular file and its references**, act systematically: check the file itself, then its callers/callees and project conventions, and suggest concrete verification steps.

## For the Target File

1. **Imports and types** – All imports resolve; no unused imports; types match (Kotlin/Java/TS). If the file was renamed or moved, package/module path is correct.
2. **References out** – Every symbol used (functions, classes, constants, API endpoints) exists and is used correctly (signatures, nullability, async). List any that are ambiguous or missing.
3. **References in** – Who uses this file? If it’s a public API (exported component, controller, service), identify call sites or usages elsewhere so the author can verify no breakage.
4. **Project conventions** – Apply naming (see naming-conventions.mdc), layer rules (controller vs service vs adapter), and placement (e.g. DTOs in `api/<domain>/dto/`). Flag violations with the rule and a suggested fix.
5. **Consistency** – Same patterns as sibling or similar files (error handling, logging, validation). No obvious duplication that should be shared.

## Cross-File and References

- **Rename/move** – If the user renamed or moved the file, list every reference (imports, strings, routes) that should be updated and in which files.
- **Signature change** – If function/API signatures changed, list call sites that must be updated and any that might be missed (e.g. reflection, dynamic calls).
- **New file** – Ensure it’s in the right package/folder and exported or registered where needed (e.g. route, DI, barrel export).

## Verification Suggestions

After the analysis, suggest **concrete steps** the user should take:

- **Build** – e.g. `./gradlew :backend:compileKotlin` or `npm run build` for the affected app.
- **Tests** – Run the relevant test scope (module, package, or full suite). If no tests exist for this code path, suggest adding a test.
- **Manual checks** – For UI: “Open screen X and verify Y.” For API: “Call endpoint Z with body W and expect status N.”
- **Lint/format** – Run project linter or formatter and fix reported issues.

## Output Format

- **Summary** – One line: “File X is consistent / has N issues.”
- **Issues** – Each with: location (file:line or symbol), rule or concern, suggested fix.
- **References** – “Used by: …” and “Uses: …” so the user can verify.
- **Verify** – Numbered list of steps to run (build, test, manual, lint).
