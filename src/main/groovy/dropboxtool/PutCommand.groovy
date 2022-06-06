package dropboxtool

import com.dropbox.core.DbxUploader
import com.dropbox.core.util.IOUtil
import com.dropbox.core.v2.files.DbxUserFilesRequests
import com.dropbox.core.v2.files.UploadErrorException
import com.dropbox.core.v2.files.WriteMode
import groovy.util.logging.Slf4j
import picocli.CommandLine

@CommandLine.Command(name = 'put')
@Slf4j
class PutCommand extends BaseCommand {

    @CommandLine.Parameters(description = "One or more files to upload", split = ",", paramLabel = "<file>")
    List<String> filesToUpload

    @CommandLine.Option(names = ['-f', '--folder'], description = "Dropbox destination folder (default: '/')")
    String dropboxFolder = '/'


    @Override
    Integer call() throws Exception {
        String folderName = sanitizeDropboxFolderName(dropboxFolder)

        DbxUserFilesRequests files = dropboxClient.files()

        for (fileName in filesToUpload) {
            File sourceFile = getHomeBasedFile(fileName)
            if (!sourceFile.exists()) {
                log.warn("${sourceFile.absolutePath} does not exists. Skipping.")
                continue
            }
            String dropboxFile = "${folderName}${sourceFile.name}"
            log.info "Uploading file ${sourceFile.path} to Dropbox: $dropboxFile"

            try {
                DbxUploader uploader = files.uploadBuilder(dropboxFile).withMode(WriteMode.OVERWRITE).start()
                sourceFile.withInputStream { input ->
                    uploader.uploadAndFinish(input)
                }
            } catch (UploadErrorException uploadException) {
                uploadException.errorValue.pathValue.reason.with {
                    if (conflict) {
                        log.error "File exists on DropBox: ${dropboxFile}"
                    } else {
                        log.info uploadException.errorValue.toStringMultiline()
                    }
                }
            }
        }
        return 0
    }


}
