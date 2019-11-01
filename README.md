# Tomcat Log Provider
This community module (not supported by Jahia) is meant to give access to the Tomcat logs in read-only for debug purposes.

### MINIMAL REQUIREMENTS
* DX 7.3.1.0

### INSTALLATION
- Download the jar and deploy it on your instance
- Go to Administrat -> Server settings -> System components -> Mount points
- Add a new mount point of the type **Tomcat log mount point**
  - Give a name to the mount point
  - Specify where to mount it

### HOW TO USE
 -  Open the document manager and go the mount point
 - **Note: ** for security reasons, do not forget to filter the access to this mount point thanks to the Jahia permissions
