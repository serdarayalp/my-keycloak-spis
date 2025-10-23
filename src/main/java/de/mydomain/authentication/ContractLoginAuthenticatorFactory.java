package de.mydomain.authentication;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class ContractLoginAuthenticatorFactory implements AuthenticatorFactory {

    /**
     * Diese ID wird in Keycloak verwendet, um den Authenticator in Flows zu identifizieren.
     */
    public static final String PROVIDER_ID = "contract-login-authenticator";

    /**
     * Eine Singleton-Instanz des Authenticators halten.
     * Authenticator-Klassen sind i. d. R. zustandslos (stateless),
     * daher kann man eine einzige Instanz für alle Requests wiederverwenden.
     */
    private static final ContractLoginAuthenticator SINGLETON = new ContractLoginAuthenticator();

    /**
     * Hier wird festgelegt, welches Authenticator-Objekt Keycloak tatsächlich verwendet.
     * Da unser Authenticator stateless ist, kann man die Singleton-Instanz zurückgeben.
     *
     * @param session
     * @return
     */
    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    /**
     * Wird beim Hochfahren von Keycloak einmalig aufgerufen
     * Hier könnten statische Ressourcen, externe Dienste etc. geladen werden.
     *
     * @param config
     */
    @Override
    public void init(Config.Scope config) {

    }

    /**
     * Wird aufgerufen, nachdem Keycloak alle Provider initialisiert hat.
     * Praktisch für Registrierung von Events oder globalen Caches.
     *
     * @param factory
     */
    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    /**
     * Wird aufgerufen, wenn Keycloak heruntergefahren wird.
     * Hier könnte man z.B. externe Verbindungen schließen.
     */
    @Override
    public void close() {

    }

    /**
     * @return
     */
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /**
     * Name, der in der Admin-Konsole angezeigt wird
     *
     * @return
     */
    @Override
    public String getDisplayType() {
        return "Contract Number Login";
    }

    /**
     * Retrieves the reference category associated with the current authenticator.
     *
     * @return a string representing the type of credential used, typically indicating the category of authentication.
     */
    @Override
    public String getReferenceCategory() {
        return PasswordCredentialModel.TYPE;
    }

    /**
     * Gibt an, ob dieser Authenticator in Flows als "configurable" gilt
     *
     * @return
     */
    @Override
    public boolean isConfigurable() {
        // kein eigenes Config-UI in der Admin-Konsole nötig
        return false;
    }

    /**
     * Ob dieser Authenticator für "User Setup" verwendet werden kann?
     * Wird z. B. für 2FA-Setup-Flows genutzt. Hier nicht relevant.
     *
     * @return
     */
    @Override
    public Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    /**
     * Gibt es für diesen Authentifikator erforderliche Aktionen, die festgelegt werden können, wenn der Benutzer diesen Authentifikator nicht eingerichtet hat?
     *
     * @return
     */
    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Authentifiziert Benutzer basierend auf ihrer Vertragsnummer anstelle eines Benutzernamens.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }
}
