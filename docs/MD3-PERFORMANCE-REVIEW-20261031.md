# GitHubM v1.0.20261031.6 — Material 3 / performance pass

## Baseline

Based on the **user-uploaded v1.0.20261031.6 repaired source snapshot**, not an older GitHubM archive or an assumption about live GitHub main. Changes target predictable startup cost and Android WebView compatibility before wholesale UI replacement.

## Implemented

1. **Offline app source index, complete and faster**: canonical `src/data/app-source-index.json` stays intact. Previously `packageSources.ts` statically imported a 7,065,684-byte JSON file into the JS dependency graph. It now imports a 623,226-byte compact source index. Historical APK builds/signatures/SHA-256/summaries are split into 64 plain `.js` assets, fetched via classic script tags only on the app's "匹配溯源" view. This is deliberately not `fetch(file://)` or ESM dynamic import (those can fail under some embedded WebViews). All 5,819 entries were validated, without dropping data. **This is a 91.18% reduction in statically referenced catalog data, NOT a measured 91% reduction in total APK size or startup time.**
2. **Reliable regeneration**: `node scripts/split-source-index.mjs` runs before `vite dev` / `vite build`. Keep the original complete index in source control for reproduction. Run `python3 scripts/check-catalog-parity.py` to cross-check URLs, catalogs, builds, signatures and hashes.
3. **Avoid redundant UI calculations**: memoize visible application sets, status counts, scanned-app comparison and search filters. Remounts and toggle changes still update correctly, but unrelated progress text and modal changes no longer recompute every list.
4. **Material 3-inspired adaptive surfaces**: application-list cards, filter chips and segmented tabs use consistent rounded surfaces, selection states, touch targets, visible focus and reduced-motion handling. Existing React/Tailwind/shadcn components are retained; no incompatible rewrite of the Android WebView shell.
5. **Noncritical image compression**: the contributor assistant avatar now uses WebP (~59 KB vs. 780 KB PNG). The original image remains in source as a backup; only WebP is imported into the running application.

## Identified but deliberately not removed or rewritten

- `src/pages/CodeBrowserPage.tsx.bak` (unused backup), a number of historical i18n scripts with absolute `/workspace/...` paths, and many components loaded from the original route registry. No application business function was removed to reduce file count. Future route-level splitting requires a separate Android file:// import-compatibility test.
- `vite` is an unpinned `npm:rolldown-vite@latest` alias. Pinning/upgrading toolchains without dependency installation and real compile testing risks breaking Web, Tauri and Android builds. Recommend pinning only after a successful dependency lockfile/CI verification.
- The client stores up to 1,000 app records using synchronous localStorage. A future IndexedDB migration might help bigger accounts, but must include a tested, lossless data migration and backup restore; not attempted here.

## Validation and release checklist

- PASS: 5,819/5,819 package mappings, URL lists, catalog labels, builds, signer fingerprints, version codes and APK SHA-256 values verified against the canonical index.
- PASS: changed TS and TSX sources parsed/transpiled with TypeScript 5.9-compatible syntax.
- PASS: index split reproducible from the canonical JSON using built-in Node.js modules.
- UNVERIFIED: full Vite dependency build, Android Gradle build, real WebView script loading, device memory/performance and end-to-end app-library matching. Dependencies cannot currently be downloaded in this execution environment (`EAI_AGAIN`).

### Manual smoke checks before updating main

1. Open home, repos, AI assistant and app library; verify no white screen.
2. Select a previously imported app with a known F-Droid source, open Match/Trace, confirm historical version/signature evidence still appears; test Android offline mode (assets must be shipped with APK).
3. For 400+ apps, switch confirmed/related/pending/unmatched filters and show/hide system apps. Confirm manual links and self-signing settings are preserved after restart.
4. Test web and native Android on dark/light themes and a narrow screen; verify filter tap targets and navigation labels.
5. Run `npm run build` and Android Gradle workflow before promoting the branch; check generated `dist/catalog-builds` includes 64 shards.
