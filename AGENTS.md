# tomcat-log-provider

Jahia OSGi module that mounts Tomcat log files as a JCR virtual filesystem via an `ExternalContentStoreProvider` (VFS2 over the local filesystem). Provides a live log viewer for `jahia.log`. Admin UI at `/jahia/administration/tomcatLogProvider` and `/jahia/administration/tomcatLogViewer`.

## Key Facts

- **artifactId**: `tomcat-log-provider` (module key: `tomcatLogProvider`)
- **Java package**: `org.jahia.community.tomcatlogprovider`
- **jahia-depends**: `default,graphql-dxm-provider,serverSettings`
- **OSGi config PID**: `org.jahia.community.tomcatLogProvider`
- `logPath` — resolved from `${catalina.base}/logs` at runtime (read-only)
- `mountPath` — configurable; default: `/sites/systemsite/files/tomcat-logs`

## Architecture

| Class | Role |
|-------|------|
| `TomcatLogMountPointService` | Merged `ManagedService` + mount lifecycle; `updated()`, `remount()`, `@Deactivate` |
| `TomcatLogDataSource` | `ExternalDataSource`; maps VFS2 `FileObject` → `ExternalData` for log files/folders |
| `TomcatLogProviderGraphQLExtensionsProvider` | Registers GraphQL extensions |
| `TomcatLogProviderQueryExtension` | GraphQL settings query + log tail query |
| `TomcatLogProviderMutationExtension` | GraphQL save mutation |

`TomcatLogMountPointService` creates `new ExternalContentStoreProvider()`, sets `dynamicallyMounted = false`.

VFS2 root: `StandardFileSystemManager.resolveFile("file:" + logPath)`.

## GraphQL API

| Operation | Name | Notes |
|-----------|------|-------|
| Query | `tomcatLogSettings` → `{mountPath, logPath}` | `logPath` is read-only |
| Query | `tomcatLogTail(lines?)` → `[String]` | Returns last N lines of `jahia.log`; used for live tail polling |
| Mutation | `tomcatLogSaveSettings(mountPath!)` → Boolean | Writes config + triggers remount |

All require `admin` permission.

## Admin UI

Two-level admin route structure:
- Parent group: `tomcatLog` at `administration-server:99` (`isSelectable: false`)
- Config panel: `tomcatLogProvider` at `administration-server-tomcatLog:1` → `/jahia/administration/tomcatLogProvider`
- Log viewer: `tomcatLogViewer` at `administration-server-tomcatLog:2` → `/jahia/administration/tomcatLogViewer`

CSS prefix: `tlp_`.  
Input id: `#tlp-mount-path`.

### Features

- Save button + **Ctrl+Enter** shortcut (fires when field non-empty)
- **Browse in jContent** button: converts `/sites/{siteKey}/files{rest}` → `/jahia/jcontent/{siteKey}/en/media/files{rest}`; disabled when mount path doesn't match `/sites/*/files/*`
- URL derived from component state (not Apollo cache) — avoids stale URL after mutation
- Live tail viewer: polls `tomcatLogTail` query, auto-scrolls to bottom; pauses when user scrolls up

## Build

```bash
mvn clean install
yarn build
yarn lint
```

## Tests (Cypress Docker)

```bash
cd tests
cp .env.example .env
yarn install
./ci.build.sh && ./ci.startup.sh
```

- Tests: `tests/cypress/e2e/01-tomcatLogProvider.cy.ts`
- Tests cover: GraphQL API (settings fields, roundtrip), log file JCR node, admin UI (configuration + log viewer), Browse in jContent, Ctrl+Enter
- `cy.window().then(win => cy.stub(win, 'open').as('windowOpen'))` used to test Browse in jContent without opening new tab

## Gotchas

- `${catalina.base}/logs` must be accessible from within the Jahia JVM; path is resolved at `@Activate` time — if Tomcat logs are in a non-standard location, the mount will appear empty
- Provisioning manifests use bare filenames (`include: 'provisioning.yml'`) — Jahia engine prepends `assets/` automatically; do **not** add the prefix
- CSS Modules in Cypress: match with `[class*="tlp_..."]`
- The parent route (`tomcatLog`) has `isSelectable: false` — it's a group node only; both child routes must be clicked individually in the admin sidebar
