/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.identity.authenticator.smsotp.test;

import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.mockito.MockitoAnnotations.initMocks;

import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import static org.mockito.Mockito.verify;

import org.wso2.carbon.extension.identity.helper.FederatedAuthenticatorUtil;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.InvalidCredentialsException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.authenticator.smsotp.SMSOTPAuthenticator;
import org.wso2.carbon.identity.authenticator.smsotp.SMSOTPConstants;
import org.wso2.carbon.identity.authenticator.smsotp.SMSOTPUtils;
import org.wso2.carbon.identity.authenticator.smsotp.exception.SMSOTPException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurationFacade.class, SMSOTPUtils.class, FederatedAuthenticatorUtil.class, FrameworkUtils.class,
        IdentityTenantUtil.class})
public class SMSOTPAuthenticatorTest {
    private SMSOTPAuthenticator smsotpAuthenticator;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Spy
    private AuthenticationContext context;

    @Spy
    private SMSOTPAuthenticator spy;

    @Mock
    SMSOTPUtils smsotpUtils;

    @Mock
    private ConfigurationFacade configurationFacade;

    @Mock
    private UserStoreManager userStoreManager;

    @Mock
    private UserRealm userRealm;

    @Mock
    private RealmService realmService;

    @BeforeMethod
    public void setUp() throws Exception {
        smsotpAuthenticator = new SMSOTPAuthenticator();
        initMocks(this);
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }


    @Test
    public void testGetFriendlyName() {
        Assert.assertEquals(smsotpAuthenticator.getFriendlyName(), SMSOTPConstants.AUTHENTICATOR_FRIENDLY_NAME);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals(smsotpAuthenticator.getName(), SMSOTPConstants.AUTHENTICATOR_NAME);
    }

    @Test
    public void testRetryAuthenticationEnabled() throws Exception {
        SMSOTPAuthenticator smsotp = PowerMockito.spy(smsotpAuthenticator);
        Assert.assertTrue((Boolean) Whitebox.invokeMethod(smsotp, "retryAuthenticationEnabled"));
    }

    @Test
    public void testGetContextIdentifierPassed() {
        when(httpServletRequest.getParameter(FrameworkConstants.SESSION_DATA_KEY)).thenReturn
                ("0246893");
        Assert.assertEquals(smsotpAuthenticator.getContextIdentifier(httpServletRequest), "0246893");
    }

    @Test
    public void testCanHandleTrue() {
        when(httpServletRequest.getParameter(SMSOTPConstants.CODE)).thenReturn(null);
        when(httpServletRequest.getParameter(SMSOTPConstants.RESEND)).thenReturn("resendCode");
        Assert.assertEquals(smsotpAuthenticator.canHandle(httpServletRequest), true);
    }

    @Test
    public void testCanHandleFalse() {
        when(httpServletRequest.getParameter(SMSOTPConstants.CODE)).thenReturn(null);
        when(httpServletRequest.getParameter(SMSOTPConstants.RESEND)).thenReturn(null);
        when(httpServletRequest.getParameter(SMSOTPConstants.MOBILE_NUMBER)).thenReturn(null);
        Assert.assertEquals(smsotpAuthenticator.canHandle(httpServletRequest), false);
    }

    @Test
    public void testGetURL() throws Exception {
        SMSOTPAuthenticator smsotp = PowerMockito.spy(smsotpAuthenticator);
        Assert.assertEquals(Whitebox.invokeMethod(smsotp, "getURL",
                SMSOTPConstants.LOGIN_PAGE, null),
                "authenticationendpoint/login.do?authenticators=SMSOTP");
    }

    @Test
    public void testGetURLwithQueryParams() throws Exception {
        SMSOTPAuthenticator smsotp = PowerMockito.spy(smsotpAuthenticator);
        Assert.assertEquals(Whitebox.invokeMethod(smsotp, "getURL",
                SMSOTPConstants.LOGIN_PAGE, "n=John&n=Susan"),
                "authenticationendpoint/login.do?n=John&n=Susan&authenticators=SMSOTP");
    }


    @Test
    public void testGetMobileNumber() throws Exception {
        mockStatic(SMSOTPUtils.class);
        when(SMSOTPUtils.getMobileNumberForUsername(anyString())).thenReturn("0775968325");
        Assert.assertEquals(Whitebox.invokeMethod(smsotpAuthenticator, "getMobileNumber",
                httpServletRequest, httpServletResponse, any(AuthenticationContext.class),
                "Kanapriya", "carbon.super", "queryParams"), "0775968325");
    }

    @Test
    public void testGetLoginPage() throws Exception {
        mockStatic(SMSOTPUtils.class);
        mockStatic(ConfigurationFacade.class);
        when(ConfigurationFacade.getInstance()).thenReturn(configurationFacade);
        when(configurationFacade.getAuthenticationEndpointURL()).thenReturn("/authenticationendpoint/login.do");
        when(SMSOTPUtils.getLoginPageFromXMLFile(any(AuthenticationContext.class), anyString())).
                thenReturn(null);
        Assert.assertNotEquals(Whitebox.invokeMethod(smsotpAuthenticator, "getLoginPage",
                new AuthenticationContext()), "/authenticationendpoint/login.do");
        Assert.assertEquals(Whitebox.invokeMethod(smsotpAuthenticator, "getLoginPage",
                new AuthenticationContext()), "/smsotpauthenticationendpoint/smsotp.jsp");
    }

    @Test
    public void testGetErrorPage() throws Exception {
        mockStatic(SMSOTPUtils.class);
        mockStatic(ConfigurationFacade.class);
        when(ConfigurationFacade.getInstance()).thenReturn(configurationFacade);
        when(configurationFacade.getAuthenticationEndpointURL()).thenReturn("/authenticationendpoint/login.do");
        when(SMSOTPUtils.getErrorPageFromXMLFile(any(AuthenticationContext.class), anyString())).
                thenReturn(null);
        Assert.assertNotEquals(Whitebox.invokeMethod(smsotpAuthenticator, "getErrorPage",
                new AuthenticationContext()), "/authenticationendpoint/login.do");
        Assert.assertEquals(Whitebox.invokeMethod(smsotpAuthenticator, "getErrorPage",
                new AuthenticationContext()), "/smsotpauthenticationendpoint/smsotpError.jsp");
    }

    @Test
    public void testRedirectToErrorPage() throws Exception {
        mockStatic(SMSOTPUtils.class);
        AuthenticationContext authenticationContext = new AuthenticationContext();
        when(SMSOTPUtils.getErrorPageFromXMLFile(authenticationContext, SMSOTPConstants.AUTHENTICATOR_NAME))
                .thenReturn("/smsotpauthenticationendpoint/smsotpError.jsp");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Whitebox.invokeMethod(smsotpAuthenticator, "redirectToErrorPage",
                httpServletResponse, authenticationContext, null, null);
        verify(httpServletResponse).sendRedirect(captor.capture());
        Assert.assertTrue(captor.getValue().contains(SMSOTPConstants.AUTHENTICATOR_NAME));
    }

    @Test
    public void testRedirectToMobileNumberReqPage() throws Exception {
        mockStatic(SMSOTPUtils.class);
        AuthenticationContext authenticationContext = new AuthenticationContext();
        when(SMSOTPUtils.isEnableMobileNoUpdate(authenticationContext, SMSOTPConstants.AUTHENTICATOR_NAME))
                .thenReturn(true);
        when(SMSOTPUtils.getMobileNumberRequestPage(authenticationContext, SMSOTPConstants.AUTHENTICATOR_NAME))
                .thenReturn("/smsotpauthenticationendpoint/mobile.jsp");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Whitebox.invokeMethod(smsotpAuthenticator, "redirectToMobileNoReqPage",
                httpServletResponse, authenticationContext, null);
        verify(httpServletResponse).sendRedirect(captor.capture());
        Assert.assertTrue(captor.getValue().contains(SMSOTPConstants.AUTHENTICATOR_NAME));
    }

    @Test
    public void testCheckStatusCode() throws Exception {
        mockStatic(SMSOTPUtils.class);
        context.setProperty(SMSOTPConstants.STATUS_CODE, "");
        when(SMSOTPUtils.isRetryEnabled(context, SMSOTPConstants.AUTHENTICATOR_NAME))
                .thenReturn(true);
        when(SMSOTPUtils.getLoginPageFromXMLFile(any(AuthenticationContext.class), anyString())).
                thenReturn("/smsotpauthenticationendpoint/smsotp.jsp");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Whitebox.invokeMethod(smsotpAuthenticator, "checkStatusCode",
                httpServletResponse, context, null, SMSOTPConstants.ERROR_PAGE);
        verify(httpServletResponse).sendRedirect(captor.capture());
        Assert.assertTrue(captor.getValue().contains(SMSOTPConstants.AUTHENTICATOR_NAME));
    }

    @Test
    public void testCheckStatusCodeWithNullValue() throws Exception {
        mockStatic(SMSOTPUtils.class);
        context.setProperty(SMSOTPConstants.STATUS_CODE, null);
        when(SMSOTPUtils.isRetryEnabled(context, SMSOTPConstants.AUTHENTICATOR_NAME))
                .thenReturn(true);
        when(SMSOTPUtils.getLoginPageFromXMLFile(any(AuthenticationContext.class), anyString())).
                thenReturn("/smsotpauthenticationendpoint/smsotp.jsp");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Whitebox.invokeMethod(smsotpAuthenticator, "checkStatusCode",
                httpServletResponse, context, null, SMSOTPConstants.ERROR_PAGE);
        verify(httpServletResponse).sendRedirect(captor.capture());
        Assert.assertTrue(captor.getValue().contains(SMSOTPConstants.AUTHENTICATOR_NAME));
    }

    @Test
    public void testCheckStatusCodeWithMismatch() throws Exception {
        mockStatic(SMSOTPUtils.class);
        context.setProperty(SMSOTPConstants.CODE_MISMATCH, "true");
        when(SMSOTPUtils.isRetryEnabled(context, SMSOTPConstants.AUTHENTICATOR_NAME))
                .thenReturn(false);
        when(SMSOTPUtils.getLoginPageFromXMLFile(any(AuthenticationContext.class), anyString())).
                thenReturn("/smsotpauthenticationendpoint/smsotp.jsp");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Whitebox.invokeMethod(smsotpAuthenticator, "checkStatusCode",
                httpServletResponse, context, null, SMSOTPConstants.ERROR_PAGE);
        verify(httpServletResponse).sendRedirect(captor.capture());
        Assert.assertTrue(captor.getValue().contains(SMSOTPConstants.ERROR_CODE_MISMATCH));
    }

    @Test
    public void testProcessSMSOTPFlow() throws Exception {
        mockStatic(SMSOTPUtils.class);
        when(SMSOTPUtils.isSMSOTPDisableForLocalUser("John", context, SMSOTPConstants.AUTHENTICATOR_NAME))
                .thenReturn(true);
        when(SMSOTPUtils.getErrorPageFromXMLFile(any(AuthenticationContext.class), anyString())).
                thenReturn(SMSOTPConstants.ERROR_PAGE);
        when(SMSOTPUtils.isEnableMobileNoUpdate(any(AuthenticationContext.class), anyString())).
                thenReturn(true);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Whitebox.invokeMethod(smsotpAuthenticator, "processSMSOTPFlow", context,
                httpServletRequest, httpServletResponse, true, "John@carbon.super", "", "carbon.super", SMSOTPConstants
                        .ERROR_PAGE);
        verify(httpServletResponse).sendRedirect(captor.capture());
        Assert.assertTrue(captor.getValue().contains(SMSOTPConstants.AUTHENTICATOR_NAME));
    }

    @Test
    public void testSendOTPDirectlyToMobile() throws Exception {
        mockStatic(SMSOTPUtils.class);
        when(SMSOTPUtils.isSendOTPDirectlyToMobile(context, SMSOTPConstants.AUTHENTICATOR_NAME))
                .thenReturn(true);
        when(SMSOTPUtils.getMobileNumberRequestPage(any(AuthenticationContext.class), anyString())).
                thenReturn("/smsotpauthenticationendpoint/mobile.jsp");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Whitebox.invokeMethod(smsotpAuthenticator, "processSMSOTPFlow", context,
                httpServletRequest, httpServletResponse, false, "John@carbon.super", "", "carbon.super", SMSOTPConstants
                        .ERROR_PAGE);
        verify(httpServletResponse).sendRedirect(captor.capture());
        Assert.assertTrue(captor.getValue().contains(SMSOTPConstants.AUTHENTICATOR_NAME));
    }

    @Test
    public void testProcessSMSOTPDisableFlow() throws Exception {
        mockStatic(SMSOTPUtils.class);
        when(SMSOTPUtils.isSendOTPDirectlyToMobile(context, SMSOTPConstants.AUTHENTICATOR_NAME))
                .thenReturn(false);
        when(SMSOTPUtils.getErrorPageFromXMLFile(any(AuthenticationContext.class), anyString())).
                thenReturn(SMSOTPConstants.ERROR_PAGE);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Whitebox.invokeMethod(smsotpAuthenticator, "processSMSOTPFlow", context,
                httpServletRequest, httpServletResponse, false, "John@carbon.super", "", "carbon.super", SMSOTPConstants
                        .ERROR_PAGE);
        verify(httpServletResponse).sendRedirect(captor.capture());
        Assert.assertTrue(captor.getValue().contains(SMSOTPConstants.SEND_OTP_DIRECTLY_DISABLE));
    }

    @Test
    public void testProcessWithLogoutTrue() throws AuthenticationFailedException, LogoutFailedException {
        when(context.isLogoutRequest()).thenReturn(true);
        AuthenticatorFlowStatus status = smsotpAuthenticator.process(httpServletRequest, httpServletResponse, context);
        Assert.assertEquals(status, AuthenticatorFlowStatus.SUCCESS_COMPLETED);
    }

    @Test
    public void testProcessWithLogoutFalse() throws Exception {
        mockStatic(FederatedAuthenticatorUtil.class);
        mockStatic(SMSOTPUtils.class);
        mockStatic(FrameworkUtils.class);
        when(context.isLogoutRequest()).thenReturn(false);
        when(httpServletRequest.getParameter(SMSOTPConstants.MOBILE_NUMBER)).thenReturn("true");
        context.setTenantDomain("carbon.super");
        when((AuthenticatedUser) context.getProperty(SMSOTPConstants.AUTHENTICATED_USER)).
                thenReturn(AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin"));
        FederatedAuthenticatorUtil.setUsernameFromFirstStep(context);
        when(SMSOTPUtils.isSMSOTPMandatory(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn(true);
        when(SMSOTPUtils.getErrorPageFromXMLFile(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn
                (SMSOTPConstants.ERROR_PAGE);
        when(SMSOTPUtils.isSendOTPDirectlyToMobile(context, SMSOTPConstants.AUTHENTICATOR_NAME))
                .thenReturn(false);
        when(FrameworkUtils.getQueryStringWithFrameworkContextId(context.getQueryParams(),
                context.getCallerSessionKey(), context.getContextIdentifier())).thenReturn(null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Whitebox.invokeMethod(smsotpAuthenticator, "processSMSOTPFlow", context,
                httpServletRequest, httpServletResponse, false, "John@carbon.super", "", "carbon.super", SMSOTPConstants
                        .ERROR_PAGE);
        verify(httpServletResponse).sendRedirect(captor.capture());
        AuthenticatorFlowStatus status = spy.process(httpServletRequest, httpServletResponse, context);
        Assert.assertTrue(captor.getValue().contains(SMSOTPConstants.SEND_OTP_DIRECTLY_DISABLE));
        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    @Test
    public void testProcessWithLogout() throws AuthenticationFailedException, LogoutFailedException {
        mockStatic(FederatedAuthenticatorUtil.class);
        mockStatic(SMSOTPUtils.class);
        mockStatic(FrameworkUtils.class);
        when(context.isLogoutRequest()).thenReturn(false);
        when(httpServletRequest.getParameter(SMSOTPConstants.CODE)).thenReturn("");
        context.setTenantDomain("carbon.super");
        when((AuthenticatedUser) context.getProperty(SMSOTPConstants.AUTHENTICATED_USER)).
                thenReturn(AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin"));
        FederatedAuthenticatorUtil.setUsernameFromFirstStep(context);
        when(SMSOTPUtils.isSMSOTPMandatory(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn(true);
        when(SMSOTPUtils.getErrorPageFromXMLFile(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn
                (SMSOTPConstants.ERROR_PAGE);
        when(SMSOTPUtils.isSendOTPDirectlyToMobile(context, SMSOTPConstants.AUTHENTICATOR_NAME))
                .thenReturn(false);
        when(FrameworkUtils.getQueryStringWithFrameworkContextId(context.getQueryParams(),
                context.getCallerSessionKey(), context.getContextIdentifier())).thenReturn(null);
        when(SMSOTPUtils.getBackupCode(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn("false");
        AuthenticatorFlowStatus status = spy.process(httpServletRequest, httpServletResponse, context);
        Assert.assertEquals(status, AuthenticatorFlowStatus.INCOMPLETE);
    }

    @Test
    public void testInitiateAuthenticationRequestWithSMSOTPMandatory() throws Exception {
        mockStatic(FederatedAuthenticatorUtil.class);
        mockStatic(SMSOTPUtils.class);
        mockStatic(FrameworkUtils.class);
        context.setTenantDomain("carbon.super");
        when((AuthenticatedUser) context.getProperty(SMSOTPConstants.AUTHENTICATED_USER)).
                thenReturn(AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin"));
        FederatedAuthenticatorUtil.setUsernameFromFirstStep(context);
        when(SMSOTPUtils.isSMSOTPMandatory(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn(true);
        when(SMSOTPUtils.getErrorPageFromXMLFile(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn
                (SMSOTPConstants.ERROR_PAGE);
        when(SMSOTPUtils.isSendOTPDirectlyToMobile(context, SMSOTPConstants.AUTHENTICATOR_NAME))
                .thenReturn(false);
        when(SMSOTPUtils.getErrorPageFromXMLFile(any(AuthenticationContext.class), anyString())).
                thenReturn(SMSOTPConstants.ERROR_PAGE);
        when(FrameworkUtils.getQueryStringWithFrameworkContextId(context.getQueryParams(),
                context.getCallerSessionKey(), context.getContextIdentifier())).thenReturn(null);
        when(SMSOTPUtils.getBackupCode(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn("false");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Whitebox.invokeMethod(smsotpAuthenticator, "initiateAuthenticationRequest",
                httpServletRequest, httpServletResponse, context);
        verify(httpServletResponse).sendRedirect(captor.capture());
        Assert.assertTrue(captor.getValue().contains(SMSOTPConstants.SEND_OTP_DIRECTLY_DISABLE));
    }

    @Test
    public void testInitiateAuthenticationRequestWithSMSOTPOptional() throws Exception {
        mockStatic(FederatedAuthenticatorUtil.class);
        mockStatic(SMSOTPUtils.class);
        mockStatic(FrameworkUtils.class);
        context.setTenantDomain("carbon.super");
        context.setProperty(SMSOTPConstants.CODE_MISMATCH, "true");
        when(context.isRetrying()).thenReturn(true);
        when(httpServletRequest.getParameter(SMSOTPConstants.RESEND)).thenReturn("false");
        when((AuthenticatedUser) context.getProperty(SMSOTPConstants.AUTHENTICATED_USER)).
                thenReturn(AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin"));
        FederatedAuthenticatorUtil.setUsernameFromFirstStep(context);
        when(SMSOTPUtils.isSMSOTPMandatory(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn(false);
        when(FederatedAuthenticatorUtil.isUserExistInUserStore(anyString())).thenReturn(true);
        when(SMSOTPUtils.getMobileNumberForUsername(anyString())).thenReturn("0778965320");
        when(SMSOTPUtils.isRetryEnabled(context, SMSOTPConstants.AUTHENTICATOR_NAME))
                .thenReturn(false);
        when(SMSOTPUtils.getLoginPageFromXMLFile(any(AuthenticationContext.class), anyString())).
                thenReturn(SMSOTPConstants.LOGIN_PAGE);
        when(SMSOTPUtils.getErrorPageFromXMLFile(any(AuthenticationContext.class), anyString())).
                thenReturn(SMSOTPConstants.ERROR_PAGE);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Whitebox.invokeMethod(smsotpAuthenticator, "initiateAuthenticationRequest",
                httpServletRequest, httpServletResponse, context);
        verify(httpServletResponse).sendRedirect(captor.capture());
        Assert.assertTrue(captor.getValue().contains(SMSOTPConstants.ERROR_CODE_MISMATCH));
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testInitiateAuthenticationRequestWithoutAuthenticatedUser() throws Exception {
        mockStatic(FederatedAuthenticatorUtil.class);
        mockStatic(SMSOTPUtils.class);
        mockStatic(FrameworkUtils.class);
        context.setTenantDomain("carbon.super");
        FederatedAuthenticatorUtil.setUsernameFromFirstStep(context);
        Whitebox.invokeMethod(smsotpAuthenticator, "initiateAuthenticationRequest",
                httpServletRequest, httpServletResponse, context);
    }

    @Test(expectedExceptions = {InvalidCredentialsException.class})
    public void testProcessAuthenticationResponseWithoutOTPCode() throws Exception {
        when(httpServletRequest.getParameter(SMSOTPConstants.CODE)).thenReturn("");
        Whitebox.invokeMethod(smsotpAuthenticator, "processAuthenticationResponse",
                httpServletRequest, httpServletResponse, context);
    }

    @Test(expectedExceptions = {InvalidCredentialsException.class})
    public void testProcessAuthenticationResponseWithResend() throws Exception {
        when(httpServletRequest.getParameter(SMSOTPConstants.CODE)).thenReturn("123456");
        when(httpServletRequest.getParameter(SMSOTPConstants.RESEND)).thenReturn("true");
        Whitebox.invokeMethod(smsotpAuthenticator, "processAuthenticationResponse",
                httpServletRequest, httpServletResponse, context);
    }

    @Test
    public void testProcessAuthenticationResponse() throws Exception {
        when(httpServletRequest.getParameter(SMSOTPConstants.CODE)).thenReturn("123456");
        context.setProperty(SMSOTPConstants.OTP_TOKEN,"123456");
        when((AuthenticatedUser) context.getProperty(SMSOTPConstants.AUTHENTICATED_USER)).
                thenReturn(AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin"));
        Whitebox.invokeMethod(smsotpAuthenticator, "processAuthenticationResponse",
                httpServletRequest, httpServletResponse, context);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testProcessAuthenticationResponseWithBackupCode() throws Exception {
        mockStatic(IdentityTenantUtil.class);
        mockStatic(SMSOTPUtils.class);
        when(httpServletRequest.getParameter(SMSOTPConstants.CODE)).thenReturn("123456");
        context.setProperty(SMSOTPConstants.OTP_TOKEN,"123");
        context.setProperty(SMSOTPConstants.USER_NAME,"admin");
        when((AuthenticatedUser) context.getProperty(SMSOTPConstants.AUTHENTICATED_USER)).
                thenReturn(AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin"));
        when(SMSOTPUtils.getBackupCode(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn("true");

        when(IdentityTenantUtil.getTenantId("carbon.super")).thenReturn(-1234);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(-1234)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        Whitebox.invokeMethod(smsotpAuthenticator, "processAuthenticationResponse",
                httpServletRequest, httpServletResponse, context);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testProcessAuthenticationResponseWithCodeMismatch() throws Exception {
        mockStatic(SMSOTPUtils.class);
        when(httpServletRequest.getParameter(SMSOTPConstants.CODE)).thenReturn("123456");
        context.setProperty(SMSOTPConstants.OTP_TOKEN,"123");
        context.setProperty(SMSOTPConstants.USER_NAME,"admin");
        when((AuthenticatedUser) context.getProperty(SMSOTPConstants.AUTHENTICATED_USER)).
                thenReturn(AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin"));
        when(SMSOTPUtils.getBackupCode(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn("false");
        Whitebox.invokeMethod(smsotpAuthenticator, "processAuthenticationResponse",
                httpServletRequest, httpServletResponse, context);
    }

    @Test
    public void testCheckWithBackUpCodes() throws Exception {
        mockStatic(IdentityTenantUtil.class);
        context.setProperty(SMSOTPConstants.USER_NAME,"admin");
        when(IdentityTenantUtil.getTenantId("carbon.super")).thenReturn(-1234);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(-1234)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        when((AuthenticatedUser) context.getProperty(SMSOTPConstants.AUTHENTICATED_USER)).
                thenReturn(AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin"));
        when(userRealm.getUserStoreManager()
                .getUserClaimValue(MultitenantUtils.getTenantAwareUsername("admin"),
                        SMSOTPConstants.SAVED_OTP_LIST, null)).thenReturn("12345,4568,1234,7896");
        AuthenticatedUser user = (AuthenticatedUser) context.getProperty(SMSOTPConstants.AUTHENTICATED_USER);
        Whitebox.invokeMethod(smsotpAuthenticator, "checkWithBackUpCodes",
                context,"1234",user);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testCheckWithInvalidBackUpCodes() throws Exception {
        mockStatic(IdentityTenantUtil.class);
        context.setProperty(SMSOTPConstants.USER_NAME,"admin");
        when(IdentityTenantUtil.getTenantId("carbon.super")).thenReturn(-1234);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(-1234)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        when((AuthenticatedUser) context.getProperty(SMSOTPConstants.AUTHENTICATED_USER)).
                thenReturn(AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin"));
        when(userRealm.getUserStoreManager()
                .getUserClaimValue(MultitenantUtils.getTenantAwareUsername("admin"),
                        SMSOTPConstants.SAVED_OTP_LIST, null)).thenReturn("12345,4568,1234,7896");
        AuthenticatedUser user = (AuthenticatedUser) context.getProperty(SMSOTPConstants.AUTHENTICATED_USER);
        Whitebox.invokeMethod(smsotpAuthenticator, "checkWithBackUpCodes",
                context,"45698789",user);
    }

    @Test
    public void testGetScreenAttribute() throws UserStoreException, AuthenticationFailedException {
        mockStatic(IdentityTenantUtil.class);
        mockStatic(SMSOTPUtils.class);
        when(SMSOTPUtils.getScreenUserAttribute(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn
                ("http://wso2.org/claims/mobile");
        when(IdentityTenantUtil.getTenantId("carbon.super")).thenReturn(-1234);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(-1234)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        when(userRealm.getUserStoreManager()
                .getUserClaimValue("admin", "http://wso2.org/claims/mobile", null)).thenReturn("0778965231");
        when(SMSOTPUtils.getNoOfDigits(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn("4");

        // with forward order
        Assert.assertEquals(smsotpAuthenticator.getScreenAttribute(context,userRealm,"admin"),"0778******");

        // with backward order
        when(SMSOTPUtils.getDigitsOrder(context, SMSOTPConstants.AUTHENTICATOR_NAME)).thenReturn("backward");
        Assert.assertEquals(smsotpAuthenticator.getScreenAttribute(context,userRealm,"admin"),"******5231");
    }

    @Test(expectedExceptions = {SMSOTPException.class})
    public void testUpdateMobileNumberForUsername() throws Exception {
        mockStatic(IdentityTenantUtil.class);
        when(IdentityTenantUtil.getTenantId("carbon.super")).thenReturn(-1234);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(-1234)).thenReturn(null);
        Whitebox.invokeMethod(smsotpAuthenticator, "updateMobileNumberForUsername",
                context,httpServletRequest,"admin","carbon.super");
    }

    @Test
    public void testGetConfigurationProperties() {
        List<Property> configProperties = new ArrayList<Property>();
        Property smsUrl = new Property();
        configProperties.add(smsUrl);
        Property httpMethod = new Property();
        configProperties.add(httpMethod);
        Property headers = new Property();
        configProperties.add(headers);
        Property payload = new Property();
        configProperties.add(payload);
        Property httpResponse = new Property();
        configProperties.add(httpResponse);
        Assert.assertEquals(configProperties.size(), smsotpAuthenticator.getConfigurationProperties().size());
    }

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }
}