---
name: error-handling
description: >
  Implement and review error handling. Use when adding or changing domain or application failures,
  validation, REST endpoints, exception advice, Problem Details, error codes, message bundles, or error contract tests.
---

# Error handling

## Start with the contract

Inspect existing error codes, handlers and message bundles before introducing new ones. If a contract change is
necessary, agree on it and update the relevant issue first.

## Classify failures at their source

- REST: use typed request DTOs, the shared Spring Boot Jackson mapper, and `@Valid` with Jakarta Validation. Reject
  malformed JSON, unknown fields, missing required fields, forbidden nulls and wrong types. Let deserialization fail on
  the first error; do not build a custom parser to aggregate errors. Where absent and explicit null require distinct
  pointers, use a nullable DTO field defaulting to null with `@NotNull` and `@JsonSetter(nulls = Nulls.FAIL)`;
  Jackson rejects explicit null and Bean Validation detects absence. Keep the domain value non-null.
- Domain: enforce invariants even outside HTTP. Use a shared `InvalidInputException` with a typed error code and a
  diagnostic message for expected invalid input. Do not create one exception class per value object or rule.
- Application: distinguish addressed-resource absence, current-state conflicts and stale revisions. Introduce a small
  exception type only when the category is needed. Validate revision preconditions before state-dependent business
  decisions as specified.
- Persistence: distinguish invalid recorded history, confirmed failed writes and unknown commit outcomes. Never
  translate corrupted persisted values into client validation errors or automatically retry uncertain writes.
- Keep HTTP statuses, problem URIs, JSON pointers, Spring and localization out of domain and application code. Never
  infer that every `IllegalArgumentException` is a client error.

## Stable error identifiers and messages

Use `error.<category>.<kebab-case-code>` for project-owned error identifiers and message keys. For example:

| Category   | Example                                                                    |
| ---------- | -------------------------------------------------------------------------- |
| Validation | `error.validation.required-field`, `error.validation.unsupported-currency` |
| Business   | `error.business.currency-blocked`, `error.business.revision-mismatch`      |
| Internal   | `error.internal.unknown`                                                   |

Reuse existing codes; centralize each identifier in its owning layer. Prefer enums with explicit stable string values
where useful. Never derive wire values from enum names or exception class names. Do not duplicate domain codes in a REST
enum just to forward them.

Use the same identifier as the `errors[].code` value and the `MessageSource` key for field failures. For non-field
failures, resolve the public detail using a namespaced key and let `type` identify the problem to clients. Do not add a
top-level `code` to the response.

Keep public messages in `infrastructure/src/main/resources/messages.properties` (English initially). Use safe arguments
and a safe default if a message is missing. Exception messages are diagnostic only; never copy them into public
responses. Do not translate machine-readable codes, problem URIs or JSON pointers. Preserve Spring's own message
resolution conventions for built-in exceptions where applicable.

## REST translation

Use one global `@RestControllerAdvice` and Spring's `ResponseEntityExceptionHandler`/`ProblemDetail`. Customize the
inherited MVC exception hooks; add handlers for real application/domain failures only. Check advice ordering and Boot
auto-configuration to avoid overlapping handlers. Preserve framework response statuses and headers, including 405 Allow.
Use the actual configured MVC mapper. See
[Spring MVC error responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html).

Return RFC 9457 `application/problem+json`: `type`, `title`, `status`, `detail`, optional `instance`, and `errors` for
field failures. Each field error contains `pointer`, `code`, and `detail`. Use
`ProblemDetail.setProperty("errors", ...)`, not an independent response envelope. Derive RFC 6901 fragment pointers at
the REST boundary; escape path segments, and point missing fields to their containing object (`#` at root).

Use the configured public documentation origin and the fixed problem paths in #13, not the request Host. Publish the
corresponding problem type documentation. Do not invent a new type for each field code.

| Status | Meaning                                                        |
| ------ | -------------------------------------------------------------- |
| 400    | Structural request errors, including malformed preconditions   |
| 404    | Addressed resource missing                                     |
| 409    | Business conflict at the current revision                      |
| 412    | Stale Group revision, including at append                      |
| 422    | Structurally valid input violates business rules               |
| 428    | Required If-Match absent                                       |
| 500    | Unexpected failure, invalid history or uncertain write outcome |

Classify by the meaning of the violated rule: validation covers structure, format, type and intrinsic single-value
constraints; business covers cross-field, eligibility, temporal and current-state rules. Categories do not determine
status by themselves: validation errors can be 400 or 422; business errors can be 404, 409, 412 or 422. Follow the
operation contract and preserve standard MVC statuses for other protocol failures.

Log unexpected failures with useful context using SLF4J. Return a safe public message; never expose exception messages,
stack traces, SQL, or rejected secrets. Use only `error.internal.unknown` for public 500 details. Return no field
errors, top-level code or cause-specific detail. After any 500 for a write, the client must refresh and inspect state
before considering resubmission; do not automatically retry. Preserve the diagnostic distinction in server-side logs. Do
not catch `Throwable` or swallow fatal errors.

Avoid custom request readers, manual Validator calls, feature-specific interceptors, error mapping registries, generic
factories and redundant wrappers. Add helpers only when repetition warrants them. Do not introduce speculative error
codes or classes before a feature needs them.

## Verify observable behavior

- Exercise domain invariants and application preconditions with focused tests.
- Test actual REST serialization, validation and advice with MVC integration tests. Assert status, media type, stable
  type URI, codes and pointers; verify messages are safe and that invalid input differs from invalid history.
- Cover malformed JSON, missing/null/wrong-type values, unknown fields, business failures, stale revisions, missing
  preconditions and unexpected/uncertain outcomes where applicable. Do not require incidental error ordering or complete
  deserialization error aggregation.
- Keep behavioral acceptance tests black-box and user-oriented; do not move the technical error matrix into them. Run
  acceptance tests on demand against an already running application.
- Follow repository verification and quality rules in `AGENTS.md`. Report any unverified behavior or contract
  deviations.
