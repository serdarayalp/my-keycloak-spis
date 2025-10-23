package de.mydomain.events;

import org.jboss.logging.Logger;
import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.*;
import org.keycloak.theme.Theme;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;

public class MyKeycloakEventListenerProvider implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(MyKeycloakEventListenerProvider.class);

    private final KeycloakSession keycloakSession;
    private final RealmProvider realmProvider;

    public MyKeycloakEventListenerProvider(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
        this.realmProvider = keycloakSession.realms();
    }

    /**
     * Handles Keycloak events and performs specific actions based on the event type.
     * For a `REGISTER` event type, this method retrieves the realm and user information
     * and triggers the sending of a welcome email to the newly registered user.
     *
     * @param event the event object containing details about the user action or system event,
     *              such as the type of event, the realm ID, and the user ID
     */
    @Override
    public void onEvent(Event event) {
        if (EventType.REGISTER.equals(event.getType())) {
            RealmModel realm = realmProvider.getRealm(event.getRealmId());
            UserModel newUser = keycloakSession.users().getUserById(realm, event.getUserId());

            sendWelcomeMail(realm, newUser);
        }

    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
    }

    @Override
    public void close() {
    }

    /**
     * Sends a welcome email to a newly registered user in a specific realm.
     * The email includes a subject, text content, and HTML content, all retrieved
     * from the Keycloak theme based on the user's locale.
     *
     * @param realmModel the realm model representing the user's realm and containing SMTP configuration
     * @param userModel the user model representing the newly registered user to whom the email is sent
     */
    private void sendWelcomeMail(RealmModel realmModel, UserModel userModel) {
        final String welcomeMailSubject = getMessagesFromKeycloakTheme(realmModel, userModel, "welcomeMailSubject");
        final String welcomeTextMail = getMessagesFromKeycloakTheme(realmModel, userModel, "welcomeMailBody");
        final String welcomeHtmlMail = getMessagesFromKeycloakTheme(realmModel, userModel, "welcomeMailBodyHtml");

        DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(keycloakSession);
        try {
            senderProvider.send(realmModel.getSmtpConfig(),
                    userModel,
                    welcomeMailSubject,
                    welcomeTextMail,
                    welcomeHtmlMail);
        } catch (EmailException e) {
            logger.error("Error sending Email.", e);
        }
    }

    /**
     * Retrieves a localized message from the Keycloak theme based on the provided message source key.
     *
     * @param realmModel the realm model used to identify the theme context
     * @param userModel the user model from which the locale is resolved
     * @param messageSource the key for the desired message to retrieve from the theme
     *
     * @return the localized message corresponding to the provided key and user's locale,
     *         or null if the theme or locale is unavailable or an error occurs
     */
    private String getMessagesFromKeycloakTheme(RealmModel realmModel, UserModel userModel, String messageSource) {
        Theme theme = getTheme(realmModel, userModel);
        Locale locale = getLocale(userModel);

        if (theme != null && locale != null) {
            try {
                Properties p = theme.getMessages(locale);
                return new MessageFormat(p.getProperty(messageSource, messageSource), locale).format(Collections.emptyList().toArray());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        return null;
    }

    /**
     * Retrieves and returns the email theme for a given realm.
     *
     * @param realmModel the realm model used to identify the email theme
     * @param userModel the user model that can be used for specific user-related theme contexts
     *
     * @return the email theme associated with the given realm, or null if an error occurs
     */
    private Theme getTheme(RealmModel realmModel, UserModel userModel) {
        ThemeManager themeManager = keycloakSession.theme();
        try {
            return themeManager.getTheme(realmModel.getEmailTheme(), Theme.Type.EMAIL);
        } catch (IOException e) {
            logger.error("Error retrieving email theme", e);
        }
        return null;
    }

    /**
     * Resolves and returns the locale for a given user.
     *
     * @param userModel the user model from which the locale is resolved
     *
     * @return the locale associated with the given user
     */
    private Locale getLocale(UserModel userModel) {
        return keycloakSession.getContext().resolveLocale(userModel);
    }
}
