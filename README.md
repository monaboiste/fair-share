# Fair Share

Fair Share helps groups of people settle shared expenses in a simple and transparent way.

Imagine a group of friends going on a weekend trip to the mountains: one person pays for the accommodation, another for
groceries, someone else for fuel, and some expenses are shared only between part of the group. Fair Share keeps track of
who paid for what, applies the appropriate split rules, and calculates who owes whom at the end of the trip.

Expenses can also be made in different currencies, while preserving the exchange rate used at the time of each expense
so that historical settlements remain consistent.

## Backend

With the JDK from `.sdkmanrc`, run `./gradlew :bootstrap:bootRun`. The backend defaults to port 8080. Group IDs are
public identifiers, not credentials.

- `POST /groups` with `{"displayName":"Weekend trip","settlementCurrency":"PLN"}` returns `201`, `Location`, and `ETag`.
- `GET /groups/{groupId}` returns the Group and its current `ETag`.
- `PATCH /groups/{groupId}/currency` with `{"settlementCurrency":"EUR"}` and the current strong `If-Match` returns `200`
  and the committed `ETag`. Refresh after `412`; refresh and inspect after a `500` or a lost write response, since the
  write may have committed. The backend does not retry commands automatically.

See [backend boundaries, storage, and Problem Details](docs/backend.md).
