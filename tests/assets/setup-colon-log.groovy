// Creates a log file whose name contains ':' (ISO 8601 timestamp) inside the
// Tomcat log directory so the Cypress regression test for the JCR path-escaping
// fix can verify that colon-named entries are accessible via the mount point.
def logDir = System.getProperty("catalina.base") + "/logs"
def colonLog = new File(logDir + "/localhost_access_log.2026-01-22T14:15:28.log")
if (!colonLog.exists()) {
    colonLog.text = "test log entry\n"
}
