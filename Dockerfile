ARG APP_INSIGHTS_AGENT_VERSION=2.5.1

# Application image

FROM hmctspublic.azurecr.io/base/java:11-distroless

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/cpo-update-service.jar /opt/app/

EXPOSE 4550
CMD [ "cpo-update-service.jar" ]
