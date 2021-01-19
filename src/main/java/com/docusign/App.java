package com.docusign;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.client.Configuration;
import com.docusign.esign.client.auth.OAuth;
import com.docusign.esign.client.auth.OAuth.UserInfo;
import com.docusign.esign.model.Document;
import com.docusign.esign.model.EnvelopeDefinition;
import com.docusign.esign.model.EnvelopeSummary;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.Signer;
import com.migcomponents.migbase64.Base64;


public class App 
{
//	CONFIGURACAO DA API:
//	1-ACESSAR https://admindemo.docusign.com/api-integrator-key
//	2-CLICA EM ADD APP & INTEGRATION KEY
//	3-CRIA O APP, GERA UMA CHAVE RSA, COPIA O CONTEUDO DO CERTIFICADO E COLOQUE EM UM ARQUIVO INFORMANDO O CAMINHO DELE
//	NO privateKeyFullPath.
//	4-ADICIONA "https://docusign.com" NO REDIRECT URLs
//	5-ATUALIZA OS DADOS DO IntegratorKey, UserId
//	6-ACESSAR PELO BROWSER https://account-d.docusign.com/oauth/auth?response_type=code&scope=signature%20impersonation&client_id=CLIENT_ID&redirect_uri=https://docusign.com
//	SUBSTITUINDO "CLIENT_ID" PELO VALOR DA IntegratorKey
	private static final String IntegratorKey = "3399dfc4-ffe8-4fe9-a749-2fb16b91d18e";
	private static final String UserId = "42b79735-a350-411c-a524-9c4dc47b848f";
	private static final String privateKeyFullPath = "C:/Users/Victor/Downloads/rsa.txt";
	
	
	private static final String Recipient = "victorhugogoncalves2010@gmail.com";
	private static final String SignTest1File = "C:/Users/Victor/Documents/teste.pdf";
//	private static final String BaseUrl = "https://demo.docusign.net";
	private static final String BaseUrl = "https://demo.docusign.net";
	public static void main(String[] args) {

		System.out.println("\nRequestASignatureTest:\n" + "===========================================");
		byte[] fileBytes = null;
		try {
			// String currentDir = new java.io.File(".").getCononicalPath();

			String currentDir = System.getProperty("user.dir");

			Path path = Paths.get(SignTest1File);
			fileBytes = Files.readAllBytes(path);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// create an envelope to be signed
		EnvelopeDefinition envDef = new EnvelopeDefinition();
		envDef.setEmailSubject("Por favor, assine este documento");
		envDef.setEmailBlurb("Olá, Por favor, assine este documento.");

		// add a document to the envelope
		Document doc = new Document();
		String base64Doc = Base64.encodeToString(fileBytes, false);
		doc.setDocumentBase64(base64Doc);
		doc.setName("teste.pdf");
		doc.setDocumentId("1");

		List<Document> docs = new ArrayList<Document>();
		docs.add(doc);
		envDef.setDocuments(docs);

		// Add a recipient to sign the document
		Signer signer = new Signer();
		signer.setEmail(Recipient);
		signer.setName("victor");
		signer.setRecipientId("1");

	

		// Above causes issue
		envDef.setRecipients(new Recipients());
		envDef.getRecipients().setSigners(new ArrayList<Signer>());
		envDef.getRecipients().getSigners().add(signer);

		// send the envelope (otherwise it will be "created" in the Draft folder
		envDef.setStatus("sent");

		ApiClient apiClient = new ApiClient(BaseUrl);
		// String currentDir = System.getProperty("user.dir");

		try {
			// IMPORTANT NOTE:
			// the first time you ask for a JWT access token, you should grant access by
			// making the following call
			// get DocuSign OAuth authorization url:
			// String oauthLoginUrl = apiClient.getJWTUri(IntegratorKey, RedirectURI,
			// OAuthBaseUrl);
			// open DocuSign OAuth authorization url in the browser, login and grant access
			// Desktop.getDesktop().browse(URI.create(oauthLoginUrl));
			// END OF NOTE

			byte[] privateKeyBytes = null;
			try {
				privateKeyBytes = Files.readAllBytes(Paths.get(privateKeyFullPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (privateKeyBytes == null)
				return;

			java.util.List<String> scopes = new ArrayList<String>();
			scopes.add(OAuth.Scope_SIGNATURE);
			scopes.add(OAuth.Scope_IMPERSONATION);

			OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(IntegratorKey, UserId, scopes, privateKeyBytes,
					3600);
			// now that the API client has an OAuth token, let's use it in all
			// DocuSign APIs
			apiClient.setAccessToken(oAuthToken.getAccessToken(), oAuthToken.getExpiresIn());
			UserInfo userInfo = apiClient.getUserInfo(oAuthToken.getAccessToken());

			System.out.println("UserInfo: " + userInfo);
			// parse first account's baseUrl
			// below code required for production, no effect in demo (same
			// domain)
			apiClient.setBasePath(userInfo.getAccounts().get(0).getBaseUri() + "/restapi");
			Configuration.setDefaultApiClient(apiClient);
			String accountId = userInfo.getAccounts().get(0).getAccountId();

			EnvelopesApi envelopesApi = new EnvelopesApi();

			EnvelopeSummary envelopeSummary = envelopesApi.createEnvelope(accountId, envDef);

			System.out.println("EnvelopeSummary: " + envelopeSummary);

			
		} catch (ApiException ex) {
			System.out.println("Exception: " + ex);
			ex.printStackTrace();
//			ex.getMessage();
		} catch (Exception e) {
			System.out.println("Exception: " + e.getLocalizedMessage());
		}

	}
}
