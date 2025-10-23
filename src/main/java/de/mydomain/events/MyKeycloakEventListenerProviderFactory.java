package de.mydomain.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class MyKeycloakEventListenerProviderFactory implements EventListenerProviderFactory {

    @Override
    public String getId() {
        return "my-keycloak-event-listener";
    }

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new MyKeycloakEventListenerProvider(keycloakSession);
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }
}
