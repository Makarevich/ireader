package ireader.drive

import com.google.api.client.auth.oauth2.TokenResponse
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow

trait IGoogleDriveFactory {
    def build(token: String): Drive
}

trait IAuthFlowProvider {
    def flow: GoogleAuthorizationCodeFlow
}

class GoogleAuthDriveFactory(flow_prov: IAuthFlowProvider)
        extends IGoogleDriveFactory
{
    def build(token: String): Drive = {
        val token_response = new TokenResponse
        token_response.setAccessToken(token)
        val flow = flow_prov.flow
        val creds = flow.createAndStoreCredential(token_response, null)
        new Drive.Builder(new NetHttpTransport,
                          new JacksonFactory,
                          creds).build
    }
}

class AuthFlowProvider extends IAuthFlowProvider {
    import collection.JavaConversions._

    private val CLIENT_ID = "1033390415538-tfko6f392unt8drju50i763vfc5sr6v5.apps.googleusercontent.com"
    private val CLIENT_SECRET = "VuGj-ju_qaYYQECyqgvaBBXj"

    def flow =
        new GoogleAuthorizationCodeFlow.Builder(
            new NetHttpTransport,
            new JacksonFactory,
            CLIENT_ID,
            CLIENT_SECRET,
            List(DriveScopes.DRIVE)).build
}

