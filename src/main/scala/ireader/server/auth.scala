package ireader.server

import ireader.server.linker.AuthFlow

class AuthSvlt extends JsonSvlt {
    private def redirect_url = fullUrl("/")

    get("/") {
        params.get("code") match {
        case Some(auth_code) =>
            val token_response = AuthFlow.flow.newTokenRequest(auth_code)
                                              .setRedirectUri(redirect_url)
                                              .execute
            val access_token = token_response.getAccessToken
            info(s"Setting new access token: ${access_token}")
            update_session(access_token)
            redirect(url(""))
        case None =>
            val is_ok = {
                val is_force = params.get("force") match {
                    case Some("true") => true
                    case _ => false
                }
                //val drive_opt = sess.drive.getOption      # TODO: fix that
                //is_force == false && !drive_opt.isEmpty
                false
            }

            if(is_ok) redirect(url("")) else {
                val auth_url = AuthFlow.flow.newAuthorizationUrl
                                            .setRedirectUri(redirect_url)
                                            .build
                redirect(auth_url)
            }
        }
    }
}

object AuthSvlt {

}
