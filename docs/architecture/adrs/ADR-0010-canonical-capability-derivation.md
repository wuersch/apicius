# ADR-0010: Canonical capability derivation

**Date:** 2026-07-08
**Status:** Superseded — merged into FEAT-005 (§ Derivation), 2026-07-16

This was a requirements decision, not an architecture decision: it defines the derived API
contract (capability → operation mapping, identity rule, wrapper, response conventions), and
requirements live in feature specs. Its full content — updated (wrapper key `items` → `data`)
and extended — is now
[`docs/spec/features/FEAT-005-create-resource.md`](../../spec/features/FEAT-005-create-resource.md),
section *Derivation — capability → operation (canonical)*. The error contract is FEAT-009;
paging is FEAT-010.

References to ADR-0010 in code and frozen mockup notes resolve here.
