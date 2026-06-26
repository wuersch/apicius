# Glossary

> Core domain terms, defined once. These are the words the UI and the docs use; when a term
> below appears in a spec, it means exactly this.

- **Resource** — a noun the API is about (Product, Order). The primary unit of organisation;
  operations are *derived* from resources, not authored as paths (PRIN-001).
- **Shape** — the structure of a resource: its fields, types, formats, identity field, and links
  to other resources. One Shape feeds every operation that uses the resource.
- **Capability** — a plain-language thing the API lets people do ("Look up one product"). The
  primary expression of an operation; the verb/path/params are derived detail (PRIN-002).
- **Operation** — the derived HTTP realisation of a capability (`GET /products/{id}`), with its
  method, path, parameters, and responses.
- **Answer** — a response, framed in plain language. The de-emphasised, derived counterpart to a
  capability (a 200 with the resource, a 404, …).
- **Inheritance / override** — examples, descriptions, validation, and deprecation attach to the
  resource (the noun) and flow down to every operation; the inherited value is always shown, and
  overriding is a deliberate local act (PRIN-005).
- **House rule** — a best practice the tool applies, explains, and lets you override; silently
  enforced, never a style guide to memorise (PRIN-006; FEAT-001).
- **Opinionated lens** — the curated UI projected over the model. The lens may be as opinionated
  as we like; the underlying model stays lossless (PRIN-003).
- **Preservation bag** — where the model keeps spec nodes it doesn't model first-class, verbatim,
  so import → export round-trips unmodeled content untouched (PRIN-003).
- **Derivation** — the deterministic mapping from a capability (+ its resource) to a concrete
  operation: method, path, params, conventional responses, `operationId` (PRIN-001/002).
- **Intent** — what the user means, independent of OpenAPI version encoding. The model and UI
  speak intent; version-specific serialization lives only in import/export (PRIN-007).
