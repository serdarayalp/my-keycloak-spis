package de.mydomain.authentication;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * Benutzerdefinierter Authenticator, der einen eigenen Login-Form rendert.
 */
public class ContractLoginForm implements Authenticator {

    public static final String CONTRACT_LOGIN = "contract-login.ftl";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // Holt den Login-Form-Provider (zuständig für Rendering)
        LoginFormsProvider form = context.form();

        // Rendert unser eigenes Template unter themes/<theme>/login/contract-login.ftl
        Response challenge = form.createForm("contract-login.ftl");
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String username = formData.getFirst("username");
        String password = formData.getFirst("password");

        RealmModel realm = context.getRealm();
        UserModel user = context.getSession().users().getUserByUsername(realm, username);

        if (user == null) {
            context.failureChallenge(AuthenticationFlowError.INVALID_USER,
                    context.form().setError(Messages.INVALID_USER).createForm(CONTRACT_LOGIN));
            return;
        }

        if (!user.credentialManager().isValid(UserCredentialModel.password(password))) {
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    context.form().setError(Messages.INVALID_USER).createForm(CONTRACT_LOGIN)
            );
            return;
        }

        // Erfolg!
        context.setUser(user);
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        // Keine speziellen Aktionen nötig
    }

    @Override
    public void close() {

    }
}
