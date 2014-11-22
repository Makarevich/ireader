package ireader.drive

import com.google.api.services.drive.Drive

import ireader.drive.web.WebDriveApi


class SessionState(token_box: ITokenContainer,
                   drive_factory: IGoogleDriveFactory) {
    private lazy val google_drive: Drive =
        drive_factory.build(token_box.token)

    lazy val drive = new WebDriveApi(google_drive)
}

