package ireader.drive.web

import ireader.drive.IInMemPropsDB
import ireader.drive.IDriveApi
import ireader.drive.IPropsDB

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import akka.actor.ActorSystem

trait ISessionState {
    def drive: IDriveApi[File]
    def props: IPropsDB

    //def props2: IPropsDB
}

trait ISessionStateFactory {
    def build(token_box: ITokenContainer): ISessionState
}

class SessionState(token_box: ITokenContainer,
                   drive_factory: IGoogleDriveFactory,
                   actor_system: ActorSystem)
extends ISessionState
{
    import actor_system.dispatcher  // TODO: clean up those implicits

    private lazy val google_drive: Drive =
        drive_factory.build(token_box.token)

    private lazy val batcher = new DriveBatcher(actor_system, google_drive)

    lazy val drive = new WebDriveApi(batcher)

    lazy val drive_io = new WebDriveIOApi(google_drive)

    lazy val props = {
        new PersistentPropsDB(drive_io, new DBFileLocator(drive))
    }

    //lazy val props = new WebFileProps(batcher)
}

class SessionStateFactory(drive_factory: IGoogleDriveFactory,
                          actor_system: ActorSystem)
extends ISessionStateFactory
{
    def build(token_box: ITokenContainer): ISessionState =
        new SessionState(token_box, drive_factory, actor_system)
}

