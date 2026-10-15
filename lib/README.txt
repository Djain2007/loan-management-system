Only needed when running WITHOUT Maven (run.bat / run.sh / VS Code without the Maven extension).

Put the MySQL JDBC driver here:
  mysql-connector-j-<version>.jar
Download: https://dev.mysql.com/downloads/connector/j/  (choose "Platform Independent")

If you build with Maven (mvn package), this folder can stay empty - the driver is
downloaded automatically and bundled into target/loan-management-system.jar.
