package ireader.drive.web

import ireader.drive.IInMemPropsDB
import ireader.drive.IDriveApi
import ireader.drive.IPropsDB

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import akka.actor.ActorSystem

trait ISessionState {
    def drive: Future[IDriveApi[File]]
    def props: Future[IPropsDB]

    def set_token(token: String): Future[Unit]
}

class SessionState(actor_system_prov: => ActorSystem)
extends ISessionState
{
    private lazy val actor_system = actor_system_prov

    // import actor_system.dispatcher  // TODO: clean up those implicits

    lazy val drive = new WebDriveApi(batcher)

    lazy val props = new WebFileProps(batcher)
}

