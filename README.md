# Tomcat Log Provider

A Jahia community module (not officially supported by Jahia) that mounts the Tomcat log directory into the JCR content tree, making log files accessible in read-only mode through the document manager.

## Requirements

- Jahia 8.2.1 or later
- `external-provider` module
- `graphql-dxm-provider` module

## Installation

1. In Jahia, go to **Administration → Server settings → System components → Modules**
2. Upload the JAR **tomcat-log-provider-X.X.X.jar**
3. Verify the module status is **Started**

The provider mounts automatically at the default JCR path `/sites/systemsite/files/tomcat-logs`.

## Configuration

The module reads its settings from the OSGi configuration file:

```
$JAHIA_DATA_DIR/karaf/etc/org.jahia.modules.tomcatlogprovider.cfg
```

| Property    | Default                               | Description                                             |
|-------------|---------------------------------------|---------------------------------------------------------|
| `mountPath` | `/sites/systemsite/files/tomcat-logs` | JCR path where the Tomcat log directory will be mounted |

Changes to the configuration file are applied immediately — the provider remounts at the new path without a restart.

### Admin UI

Settings can also be edited in the Jahia administration panel:

**Administration → Tomcat Log Provider → Configuration**

The panel shows:
- The resolved Tomcat log directory path on disk (`${catalina.base}/logs`) — read-only
- The JCR mount path — editable, saved via the **Save settings** button

## GraphQL API

The module exposes two queries and one mutation under a single hierarchical
`tomcatLog` namespace on the root `Query`/`Mutation` types (registered through the
`graphql-dxm-provider` extension point).

### Queries

```graphql
# Returns current settings
query {
    tomcatLog {
        settings {
            mountPath   # current JCR mount path
            logPath     # resolved on-disk log directory
        }
    }
}

# Returns the last N lines of jahia.log (default: 200, capped at 5000)
query {
    tomcatLog {
        tail(lines: 200)
    }
}
```

### Mutation

```graphql
# Saves the mount path and triggers a remount; returns true on success
mutation {
    tomcatLog {
        saveSettings(mountPath: "/sites/systemsite/files/tomcat-logs")
    }
}
```

All operations require the `tomcatLogProviderAdmin` permission, which is granted by
the module-shipped `tomcat-log-provider-administrator` role.

## Live log viewer

A dedicated administration page streams the last 200 lines of `jahia.log` in real time, refreshing every 2 seconds:

**Administration → Tomcat Log Provider → Live log viewer**

The terminal auto-scrolls to the latest entries. Scrolling up pauses auto-scroll; scrolling back to the bottom resumes it.

## How it works

- The module registers an `ExternalContentStoreProvider` backed by Apache Commons VFS2, pointing at `${catalina.base}/logs`
- Log files appear as `jnt:file` nodes and subdirectories as `jnt:folder` nodes under the configured mount path
- All write operations are rejected — the provider is strictly read-only
- Access is restricted to users granted the `tomcat-log-provider-administrator` role
  (which carries the fine-grained `tomcatLogProviderAdmin` permission); full server
  administrators inherit it, but the role can be assigned without granting full admin rights

## Security

> **Note:** the mount path is accessible through the Jahia document manager. Restrict access to the mount point using Jahia's built-in ACL system to prevent non-admin users from browsing server log files.
