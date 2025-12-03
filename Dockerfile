# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.6.2

# Application image

FROM hmctspublic.azurecr.io/base/java:21-distroless

USER hmcts
COPY lib/applicationinsights.json /opt/app/
COPY build/libs/cpo-update-service.jar /opt/app/

EXPOSE 4550
CMD [ \
    "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
    "cpo-update-service.jar" \
    ]
