# Build Fair Share from local archetype modules

Fair Share will initially copy the `product`, `pricing`, `rules`, and `graphs` archetype sources into separate Gradle
modules under `core`, together with their supporting `common` and `quantity` modules. The application remains in a
separate `app` module and depends only on the building blocks it uses; `graphs` is required for netting from the first
version. This makes the archetypes locally adaptable, at the accepted cost of temporarily inheriting code and
dependencies that Fair Share may later remove or reshape.
