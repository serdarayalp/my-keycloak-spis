package de.mydomain.authentication;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.models.*;
import org.keycloak.services.messages.Messages;
import org.keycloak.theme.Theme;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;

public class ContractLoginAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(ContractLoginAuthenticator.class);

    /**
     * Wird von Keycloak aufgerufen, wenn dieser Authenticator an der Reihe ist.
     * Hier zeigt man normalerweise ein Login-Formular oder eine Eingabemaske an.
     *
     * @param context
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        Response loginUsernamePassword = context.form().createLoginUsernamePassword();
        context.challenge(loginUsernamePassword);
    }

    /**
     * Wird aufgerufen, wenn der Benutzer das Formular abgeschickt hat, also eine "Aktion" ausgeführt wurde.
     * Hier liest man die Formulardaten (z.B. Vertragsnummer, Passwort etc.) aus und prüft den Benutzer.
     *
     * @param context
     */
    @Override
    public void action(AuthenticationFlowContext context) {

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String username = formData.getFirst("username");
        String password = formData.getFirst("password");

        RealmModel realm = context.getRealm();

        KeycloakSession keycloakSession = context.getSession();

        UserModel user = keycloakSession.users().getUserByUsername(realm, username);

        if (user == null) {
            context.failureChallenge(AuthenticationFlowError.INVALID_USER,
                    context.form().setError(Messages.INVALID_USER).createLoginUsernamePassword());
            return;
        }

        if (!user.credentialManager().isValid(UserCredentialModel.password(password))) {
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    context.form().setError(Messages.INVALID_USER).createLoginUsernamePassword()
            );
            return;
        }

        // Benutzer deaktiviert oder noch nicht verifiziert?
        if (!user.isEnabled() || !user.isEmailVerified()) {

            context.form().setInfo("Activation email sent to: " + user.getEmail());

            // E-Mail senden
            sendMail(realm, user, keycloakSession, "mailSubject", "mailTextBody", "mailHTMLBody");

            // die Authentifizierung versucht wurde, aber weitere Schritte, wie E-Mail-Verifizierung, erforderlich!
            context.attempted();
            return;

        }

        context.setUser(user);
        context.success();
    }

    /**
     * Gibt an, ob dieser Authenticator einen bereits gesetzten Benutzer benötigt.
     * Da wir den Benutzer selbst identifizieren (z.B. per Vertragsnummer), geben wir hier false zurück.
     *
     * @return
     */
    @Override
    public boolean requiresUser() {
        return false;
    }

    /**
     * Ist dieser Authentifikator für diesen Benutzer konfiguriert.
     *
     * @param session
     * @param realm
     * @param user
     * @return
     */
    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    /**
     * Kann verwendet werden, um automatisch "Required Actions" hinzuzufügen,
     * falls der Benutzer bestimmte Aktionen noch ausführen muss (z. B. Passwort ändern).
     * Hier in unserem Fall nicht erforderlich.
     *
     * @param session
     * @param realm
     * @param user
     */
    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Keine zusätzlichen Aktionen nötig
    }

    /**
     * Wird aufgerufen, wenn der Authenticator beendet oder aus dem Speicher entladen wird.
     * Kann genutzt werden, um Ressourcen freizugeben (z.B. Datenbankverbindungen, Cache).
     */
    @Override
    public void close() {
        // Keine Ressourcen zu schließen
    }

    /**
     * Sends a welcome email to a newly registered user in a specific realm.
     * The email includes a subject, text content, and HTML content, all retrieved
     * from the Keycloak theme based on the user's locale.
     *
     * @param realmModel the realm model representing the user's realm and containing SMTP configuration
     * @param userModel  the user model representing the newly registered user to whom the email is sent
     */
    private void sendMail(RealmModel realmModel, UserModel userModel, KeycloakSession keycloakSession, String subject, String textBody, String htmlBody) {

        final String _subject = getMessagesFromKeycloakTheme(realmModel, userModel, subject, keycloakSession);
        final String _textBody = getMessagesFromKeycloakTheme(realmModel, userModel, textBody, keycloakSession);
        final String _htmlBody = getMessagesFromKeycloakTheme(realmModel, userModel, htmlBody, keycloakSession);

        DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(keycloakSession);
        try {
            senderProvider.send(realmModel.getSmtpConfig(),
                    userModel,
                    _subject,
                    _textBody,
                    _htmlBody);
        } catch (EmailException e) {
            logger.error("Error sending Email.", e);
        }
    }

    /**
     * Retrieves a localized message from the Keycloak theme based on the provided message source key.
     *
     * @param realmModel    the realm model used to identify the theme context
     * @param userModel     the user model from which the locale is resolved
     * @param messageSource the key for the desired message to retrieve from the theme
     * @return the localized message corresponding to the provided key and user's locale,
     * or null if the theme or locale is unavailable or an error occurs
     */
    private String getMessagesFromKeycloakTheme(RealmModel realmModel, UserModel userModel, String messageSource, KeycloakSession keycloakSession) {

        Theme theme = getTheme(realmModel, userModel, keycloakSession);

        Locale locale = getLocale(userModel, keycloakSession);

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
     * @param userModel  the user model that can be used for specific user-related theme contexts
     * @return the email theme associated with the given realm, or null if an error occurs
     */
    private Theme getTheme(RealmModel realmModel, UserModel userModel, KeycloakSession keycloakSession) {
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
     * @return the locale associated with the given user
     */
    private Locale getLocale(UserModel userModel, KeycloakSession keycloakSession) {
        return keycloakSession.getContext().resolveLocale(userModel);
    }

}
