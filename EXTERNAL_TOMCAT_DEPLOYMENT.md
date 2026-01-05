# External Tomcat Deployment Guide

This guide explains how to deploy the TummyGo application to an external Tomcat server.

## Prerequisites

1. **Java 17 or higher** - Ensure your Tomcat server is running Java 17+
2. **MySQL Database** - Database must be accessible from the Tomcat server
3. **Tomcat 10.x or higher** - Required for Jakarta EE 9+ support (Spring Boot 3.x requirement)

## Build the WAR File

```bash
./mvnw clean package -DskipTests
```

The WAR file will be created at: `target/TummyGo-1.war`

## Deployment Steps

### Option 1: Deploy as ROOT Application (Recommended)

1. **Stop Tomcat** (if running)
2. **Remove existing ROOT application** (if any):
   ```bash
   rm -rf $CATALINA_HOME/webapps/ROOT
   rm -f $CATALINA_HOME/webapps/ROOT.war
   ```
3. **Copy WAR file**:
   ```bash
   cp target/TummyGo-1.war $CATALINA_HOME/webapps/ROOT.war
   ```
4. **Start Tomcat**
5. **Access application** at: `http://localhost:8080/`

### Option 2: Deploy with Context Path

1. **Stop Tomcat** (if running)
2. **Copy WAR file**:
   ```bash
   cp target/TummyGo-1.war $CATALINA_HOME/webapps/tummygo.war
   ```
3. **Start Tomcat**
4. **Access application** at: `http://localhost:8080/tummygo/`

## Configuration

### Database Configuration

Update `src/main/resources/application.properties` before building:

```properties
# Update these for your production database
spring.datasource.url=jdbc:mysql://YOUR_DB_HOST:3306/tummygo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD
```

**OR** create a `context.xml` file in `$CATALINA_HOME/conf/Catalina/localhost/ROOT.xml` (or your context path):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Context>
    <Resource name="jdbc/tummygo"
              auth="Container"
              type="javax.sql.DataSource"
              driverClassName="com.mysql.cj.jdbc.Driver"
              url="jdbc:mysql://YOUR_DB_HOST:3306/tummygo?useSSL=false&amp;allowPublicKeyRetrieval=true&amp;serverTimezone=UTC"
              username="YOUR_DB_USER"
              password="YOUR_DB_PASSWORD"
              maxTotal="20"
              maxIdle="10"
              maxWaitMillis="10000"/>
</Context>
```

Then update `application.properties` to use JNDI:

```properties
spring.datasource.jndi-name=java:comp/env/jdbc/tummygo
```

### Email Configuration

Update email settings in `application.properties` before building:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

## Common Issues and Solutions

### Issue 1: ClassNotFoundException or NoClassDefFoundError

**Solution**: Ensure Tomcat has access to all required JAR files. The WAR file includes all dependencies in `BOOT-INF/lib/`, so this should work automatically.

### Issue 2: Static Resources Not Loading (CSS/JS/Images)

**Solution**: 
- Check that the WAR file includes static resources
- Verify the context path matches your deployment
- Check Tomcat logs for resource loading errors
- Ensure `WebConfig.java` resource handlers are correct

### Issue 3: Database Connection Errors

**Solution**:
- Verify MySQL is running and accessible
- Check database credentials in `application.properties`
- Ensure MySQL connector JAR is available (included in WAR)
- Check firewall rules if database is on a different server

### Issue 4: 404 Errors on All Pages

**Solution**:
- Check context path configuration
- Verify `TummyGoApplication` extends `SpringBootServletInitializer`
- Check Tomcat logs for startup errors
- Ensure WAR file was built correctly

### Issue 5: Thymeleaf Template Not Found

**Solution**:
- Verify templates are in `src/main/resources/templates/`
- Check Thymeleaf configuration in `application.properties`
- Ensure template files have `.html` extension

### Issue 6: Security/Login Issues

**Solution**:
- Verify Spring Security configuration
- Check that user roles are correctly set in database
- Review `SecurityConfig.java` for path mappings

## Verification

After deployment, check:

1. **Tomcat Logs**: `$CATALINA_HOME/logs/catalina.out`
2. **Application Logs**: Check for Spring Boot startup messages
3. **Health Endpoint**: `http://localhost:8080/actuator/health` (if deployed as ROOT)
4. **Home Page**: `http://localhost:8080/` (or your context path)

## Troubleshooting

### View Tomcat Logs

```bash
tail -f $CATALINA_HOME/logs/catalina.out
```

### Check Application Status

```bash
curl http://localhost:8080/actuator/health
```

### Restart Tomcat

```bash
$CATALINA_HOME/bin/shutdown.sh
$CATALINA_HOME/bin/startup.sh
```

## Notes

- The application uses **Thymeleaf** templates, not JSP
- All dependencies are packaged in the WAR file (no external lib folder needed)
- Flyway will automatically run database migrations on startup
- The application requires MySQL 8.0+ database
- Ensure Tomcat has sufficient memory: `-Xmx512m` minimum recommended

## Production Recommendations

1. **Use JNDI DataSource** instead of direct JDBC URL in properties
2. **Enable SSL/TLS** for secure connections
3. **Configure proper logging** (Logback/Log4j2)
4. **Set up monitoring** (Spring Boot Actuator)
5. **Use environment-specific properties** files
6. **Configure connection pooling** properly
7. **Set up reverse proxy** (Apache/Nginx) if needed

