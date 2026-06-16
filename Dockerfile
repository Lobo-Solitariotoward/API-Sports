FROM tomcat:10.1-jdk17

# Limpia las apps de ejemplo de Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Copia tu WAR como ROOT para que quede en la raíz (/)
COPY dist/API-Sports.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
