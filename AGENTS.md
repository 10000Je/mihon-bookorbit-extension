# BookOrbit Mihon extension — project instructions

## Purpose and completion
Build an independently implemented, installable Android extension APK (`app.bookorbit.mihonext`) without changing Mihon or BookOrbit. `CODEX_PROMPT.md` is the principal specification; verify the cases in `docs/ACCEPTANCE_TESTS.md`. Never clone, inspect, copy, patch, or depend on the Keiyoushi BookOrbit extension. Do not declare unexecuted tests or unresolved functionality complete.

## Structure and workflow
- `app/`: Android APK, SourceFactory, catalogue sources, configuration and API integration.
- `app/src/main/kotlin/app/bookorbit/mihonext/`: independent Kotlin implementation; place wire models and pure policies in separate files.
- `app/src/test/`: meaningful format, identity, pagination, authentication and protocol tests.
- `docs/`: API evidence, architecture decisions, acceptance results and build/install instructions.
- `gradle/wrapper/`: pinned, checksum-verified reproducible toolchain.
- Use Git for this project. Commit coherent milestones; exclude credentials, local SDK configuration, caches and generated build outputs. Do not rewrite existing history.

## Official API research
Use only official Mihon, tachiyomix and BookOrbit sources for contracts. Record repository, exact commit/version, controller/DTO paths and verified endpoints in `docs/API_DISCOVERY.md`. Review upstream instructions and license/additional terms without copying their project instructions. Never invent a missing endpoint or substitute another server project. If an authoritative source is unavailable, document the blocker and continue independently verifiable work; do not present guesses as supported integration.

## Source and identity rules
Provide exactly three configurable fixed sources, preserving the existing IDs for slots 1–3. No network or long blocking operation in `createSources()` or `getFilterList()`. Each source supports multiple libraries through Mihon's native Library filter; disable latest so the browse controls are Popular and Filter. Popular lists the configured default library. Source IDs must survive library renames and reordering. Bind stored manga/chapter identities to server and library identifiers, and keep saved works readable across filter/default-library changes without silent cross-server/library remapping. Explain reload and remapping limitations in settings and docs. These requirements supersede the original six-slot, one-library-per-source specification in `CODEX_PROMPT.md`.

## Data and network rules
Allow only CBZ/CBR/CB7, case-insensitively; reject EPUB/PDF/unknown formats. Exclude unreadable-only and empty series from both catalogue and search. Prefer verified server filters; otherwise use bounded look-ahead and bounded concurrency/caches, preserving pagination order and deduplication. Distinguish empty results from failures. Verify authenticated covers/pages, cancellation, timeouts, 401/403/404/429/5xx, bounded retry/backoff and API-version limitations. Never hide a protocol failure as an empty successful page.

## Security and attribution
Never log credentials, cookies, tokens, private server URLs or request bodies containing secrets. Never put credentials in URLs. Use appropriate private/encrypted storage and disable backups. Keep authorization scoped to the configured server; guard redirects and third-party images. Do not commit real servers, accounts, keys or signing secrets. Verify upstream licenses and attribution before redistributing copied material; prefer original client code. Keep release signing reproducible for users without committing private keys.

## Build and verification
Pin compatible AGP/Kotlin/Gradle/JDK/SDK and official tachiyomix versions after checking actual sources. Include a working wrapper. **Run basic local JVM unit tests only** (`./gradlew :app:testDebugUnitTest`). Exclude `LiveServerTest` from the default test task. Do not invoke, start, connect to, install into, or test with an emulator or physical device; do not run live-server/integration/instrumentation tests, APK assembly or release verification unless the user explicitly requests a broader scope later. Synthetic unit-test responses must be labelled as such, never presented as actual server verification. Clearly separate current unit results from historical build/device/server evidence. Do not add tests that merely repeat trivial implementation.

## Handoff and unresolved risks
Report implementation files, dynamic-vs-slot evidence, verified server versions, executed checks, APK absolute/relative path and checksum, installation/trust steps, Git milestones and remaining limitations. Record missing official sources, incompatible APIs, absent devices/SDK/server credentials and security/reload concerns explicitly. Do not label the extension functional until its required API contract is supported by evidence.
