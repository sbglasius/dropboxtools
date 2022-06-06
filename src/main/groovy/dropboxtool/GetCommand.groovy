package dropboxtool

import com.dropbox.core.DbxDownloader
import groovy.util.logging.Slf4j
import picocli.CommandLine

@Slf4j
@CommandLine.Command(name = 'get')
class GetCommand extends BaseCommand {

    @CommandLine.Parameters(description = "One or more files to download", split = ",", paramLabel = "<file>")
    List<String> filesToDownload

    @CommandLine.Option(names = ['-f', '--folder'], description = "Dropbox source folder (default: '/')")
    String dropboxFolder = '/'

    @CommandLine.Option(names = ['-d', '--destination'], description = "Destination directory (default: '.')")
    String destination = '.'

    @Override
    Integer call() throws Exception {
        File destinationDir = getHomeBasedFile(destination)
        if (!destinationDir.exists()) {
            log.info("Creating new directory ${destinationDir.absolutePath}")
            destinationDir.mkdirs()
        }
        if (!destinationDir.isDirectory()) {
            log.warn("Destination directory ${destinationDir.absolutePath} is not a directory")
            return 1
        }

        String folderName = sanitizeDropboxFolderName(dropboxFolder)
        for (dropboxFileName in filesToDownload) {
            String dropboxFile = "${folderName}${dropboxFileName}"
            log.info "Downloading file from Dropbox:  $dropboxFile to ${destinationDir.absolutePath}"
            DbxDownloader downloader = dropboxClient.files().download(dropboxFile)
            File destinationFile = new File(destinationDir, dropboxFileName)

            destinationFile.withOutputStream { output -> output << downloader.inputStream
            }
        }
        return 0
    }

}
