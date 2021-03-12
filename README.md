# keycloak-web

Deploy KeyCloak as WAR in your WildFly server. 

## Modules

The project consists of the following modules:

### application

Contains the basic KeyCloak-Application class and defines the dependencies for the WAR

### war

Example WAR configuration: By default creates a demo-realm with users and installs admin user.
Data is kept in H2 database ${jboss.server.data.dir}/keycloak.db

# Deployment

Just copy the WAR into the deployments/ directory of your WildFly/EAP server and start it.
Then point your browser to

      http://localhost:8080/auth/admin/master/console/

and login as admin.

## Users

Admin user is kept in file META-INF/keycloak-add-user.json which is copied at startup of the WAR to ${jboss.server.config.dir}/

Username: admin
Password: RcSTU63zAMkpUqhGGuSCmU9wIbXuDSpv+

# Export realm

1. Start Server

   ```./standalone.sh -Dkeycloak.connectionsJpa.url='jdbc:h2:${jboss.server.data.dir}/keycloak'```   
      
2. Configure realm in running KeyCloak and STOP server via Control-C or kill

3. Run KeyCloak/WildFly again

   ```sh standalone.sh -Dkeycloak.connectionsJpa.url='jdbc:h2:${jboss.server.data.dir}/keycloak' -Dkeycloak.migration.action="export" -Dkeycloak.migration.provider="singleFile" -Dkeycloak.migration.file="/tmp/master.json" -Dkeycloak.migration.realmName="master" -Dkeycloak.migration.usersExportStrategy="REALM_FILE"```

After export is done SHUTDOWN the process via Control-C or kill

# Run with volatile in-memory database

If you do not want to keep data between restarts use a in-memory data base

   ```./standalone.sh -Dkeycloak.connectionsJpa.url='jdbc:h2:mem:keycloak;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'```

