package dod.p1.keycloak.registration;

import dod.p1.keycloak.common.CommonConfig;
import dod.p1.keycloak.utils.Utils;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.authenticators.x509.UserIdentityExtractor;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.authentication.authenticators.x509.X509ClientCertificateAuthenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.services.x509.X509ClientCertificateLookup;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static dod.p1.keycloak.utils.Utils.setupFileMocks;
import static org.keycloak.services.x509.DefaultClientCertificateLookup.JAVAX_SERVLET_REQUEST_X509_CERTIFICATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;


@RunWith(PowerMockRunner.class)
class UpdateX509Test {

    @Mock
    KeycloakSession keycloakSession;
    @Mock
    KeycloakContext keycloakContext;
    @Mock
    AuthenticationSessionModel authenticationSessionModel;
    @Mock
    RootAuthenticationSessionModel rootAuthenticationSessionModel;
    @Mock
    HttpRequest httpRequest;
    @Mock
    RealmModel realmModel;
    @Mock
    X509ClientCertificateLookup x509ClientCertificateLookup;
    @Mock
    AuthenticatorConfigModel authenticatorConfigModel;
    @Mock
    X509ClientCertificateAuthenticator x509ClientCertificateAuthenticator;
    @Mock
    UserIdentityExtractor userIdentityExtractor;
    @Mock
    UserProvider userProvider;
    @Mock
    UserModel userModel;
    @Mock
    RequiredActionContext requiredActionContext;
    @Mock
    LoginFormsProvider loginFormsProvider;
    @Mock
    Config.Scope scope;

    public UpdateX509Test() {}

    @Before
    public void setupMockBehavior() throws Exception {

        setupFileMocks();

        // common mock implementations
        PowerMockito.when(requiredActionContext.getSession()).thenReturn(keycloakSession);
        PowerMockito.when(keycloakSession.getContext()).thenReturn(keycloakContext);
        PowerMockito.when(keycloakSession.getContext().getAuthenticationSession()).thenReturn(authenticationSessionModel);
        PowerMockito.when(authenticationSessionModel.getParentSession()).thenReturn(rootAuthenticationSessionModel);
        PowerMockito.when(rootAuthenticationSessionModel.getId()).thenReturn("xxx");
        PowerMockito.when(requiredActionContext.getHttpRequest()).thenReturn(httpRequest);
        PowerMockito.when(requiredActionContext.getRealm()).thenReturn(realmModel);

        // setup X509Tools
        PowerMockito.when(keycloakSession.getProvider(X509ClientCertificateLookup.class)).thenReturn(x509ClientCertificateLookup);

        // create cert array and add the cert
        X509Certificate[] certList = new X509Certificate[1];
        X509Certificate x509Certificate2 = Utils.buildTestCertificate();
        certList[0] = x509Certificate2;
        PowerMockito.when(x509ClientCertificateLookup.getCertificateChain(httpRequest)).thenReturn(certList);

        PowerMockito.when(realmModel.getAuthenticatorConfigsStream()).thenAnswer((stream) -> {
            return Stream.of(authenticatorConfigModel);
        });

        // create map
        Map<String, String> mapSting = new HashMap<>();
        mapSting.put("x509-cert-auth.mapper-selection.user-attribute-name", "test");
        PowerMockito.when(authenticatorConfigModel.getConfig()).thenReturn(mapSting);

        PowerMockito.when(x509ClientCertificateAuthenticator
                .getUserIdentityExtractor(any(X509AuthenticatorConfigModel.class))).thenReturn(userIdentityExtractor);
        PowerMockito.when(keycloakSession.users()).thenReturn(userProvider);
        PowerMockito.when(userProvider.searchForUserByUserAttributeStream(any(RealmModel.class), anyString(), anyString()))
                .thenAnswer((stream) -> {
                    return Stream.of(userModel);
                });
    }

    @Test
    @PrepareForTest({X509Tools.class, CommonConfig.class})
    public void testEvaluateTriggers() throws Exception {
        PowerMockito.when(requiredActionContext.getAuthenticationSession()).thenReturn(authenticationSessionModel);
        PowerMockito.when(requiredActionContext.getAuthenticationSession().getAuthNote("IGNORE_X509")).thenReturn("authNote");

        // create cert array and add the cert
        X509Certificate[] certList = new X509Certificate[1];
        X509Certificate x509Certificate2 = Utils.buildTestCertificate();
        certList[0] = x509Certificate2;

        PowerMockito.when(requiredActionContext.getHttpRequest().getAttribute(JAVAX_SERVLET_REQUEST_X509_CERTIFICATE))
            .thenReturn(certList);
        PowerMockito.when(requiredActionContext.getUser()).thenReturn(userModel);

        UpdateX509 updateX509 = new UpdateX509();
        updateX509.evaluateTriggers(requiredActionContext);
    }

    @Test
    @PrepareForTest({X509Tools.class, CommonConfig.class})
    public void testRequiredActionChallenge() throws Exception {
        PowerMockito.when(requiredActionContext.form()).thenReturn(loginFormsProvider);

        UpdateX509 updateX509 = new UpdateX509();
        updateX509.requiredActionChallenge(requiredActionContext);
    }

    @Test
    @PrepareForTest({X509Tools.class, CommonConfig.class})
    public void testProcessActionCancel() throws Exception {
        MultivaluedMapImpl<String, String> formData = new MultivaluedMapImpl<>();
        formData.add("cancel", "");

        PowerMockito.when(requiredActionContext.getHttpRequest().getDecodedFormParameters()).thenReturn(formData);
        PowerMockito.when(requiredActionContext.getAuthenticationSession()).thenReturn(authenticationSessionModel);

        UpdateX509 updateX509 = new UpdateX509();
        updateX509.processAction(requiredActionContext);
    }

    @Test
    @PrepareForTest({X509Tools.class, CommonConfig.class})
    public void testProcessAction() throws Exception {
        MultivaluedMapImpl<String, String> formData = new MultivaluedMapImpl<>();

        PowerMockito.when(requiredActionContext.getHttpRequest().getDecodedFormParameters()).thenReturn(formData);
        PowerMockito.when(requiredActionContext.getAuthenticationSession()).thenReturn(authenticationSessionModel);
        PowerMockito.when(requiredActionContext.getUser()).thenReturn(userModel);

        UpdateX509 updateX509 = new UpdateX509();
        updateX509.processAction(requiredActionContext);
    }

    @Test
    @PrepareForTest({X509Tools.class, CommonConfig.class})
    public void testGetDisplayText() {
        UpdateX509 updateX509 = new UpdateX509();
        updateX509.getDisplayText();
    }

    @Test
    @PrepareForTest({X509Tools.class, CommonConfig.class})
    public void testIsOneTimeAction() {
        UpdateX509 updateX509 = new UpdateX509();
        updateX509.isOneTimeAction();
    }

    @Test
    @PrepareForTest({X509Tools.class, CommonConfig.class})
    public void testCreate() {
        UpdateX509 updateX509 = new UpdateX509();
        updateX509.create(keycloakSession);
    }

    @Test
    @PrepareForTest({X509Tools.class, CommonConfig.class})
    public void testPostInit() {
        UpdateX509 updateX509 = new UpdateX509();
        updateX509.postInit(keycloakSession.getKeycloakSessionFactory());
    }

    @Test
    @PrepareForTest({X509Tools.class, CommonConfig.class})
    public void testClose() {
        UpdateX509 updateX509 = new UpdateX509();
        updateX509.close();
    }

    @Test
    @PrepareForTest({X509Tools.class, CommonConfig.class})
    public void testGetId() {
        UpdateX509 updateX509 = new UpdateX509();
        updateX509.getId();
    }

}
