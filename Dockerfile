ARG APP_INSIGHTS_AGENT_VERSION=2.5.1

# Application image

FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/cpo-update-service.jar /opt/app/

EXPOSE 4550
CMD [ \
    "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
    "cpo-update-service.jar" \
    ]
