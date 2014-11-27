package ireader.drive.web

import ireader.drive.IInMemPropsDB
import ireader.drive.IDriveApi
import ireader.drive.IPropsDB

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File

trait ISessionState {
    def drive: IDriveApi[File]
    def props: IPropsDB
}

trait ISessionStateFactory {
    def build(token_box: ITokenContainer): ISessionState
}

class SessionState(token_box: ITokenContainer,
                   drive_factory: IGoogleDriveFactory)
extends ISessionState
{
    private lazy val google_drive: Drive =
        drive_factory.build(token_box.token)

    lazy val drive = new WebDriveApi(google_drive)

    lazy val drive_io = new WebDriveIOApi(google_drive)

    lazy val props = {
        import scala.concurrent.ExecutionContext.Implicits.global
        val drive2 = new WebDriveApi(google_drive)
        new PersistentPropsDB(drive_io, new DBFileLocator(drive2))
    }
}

class SessionStateFactory(drive_factory: IGoogleDriveFactory)
extends ISessionStateFactory
{
    def build(token_box: ITokenContainer): ISessionState =
        new SessionState(token_box, drive_factory)
}

