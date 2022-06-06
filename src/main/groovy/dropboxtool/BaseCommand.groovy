package dropboxtool

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import groovy.json.JsonSlurper
import picocli.CommandLine

import java.util.concurrent.Callable

abstract class BaseCommand implements Callable<Integer> {

    @CommandLine.Option(names = ['-v', '--verbose'], description = 'Be verbose')
    boolean verbose

    @CommandLine.Option(names = ['-c', '--credentials'], arity = "1", description = "Dropbox credentials file (default: '~/.dropbox-tool-auth.json')")
    String credentials = '~/.dropbox-tool-auth.json'

    protected static File getHomeBasedFile(String name) {
        if (name.startsWith('~/')) {
            def home = new File(System.getProperty('user.home'))
            return new File(home, name[2..-1])
        }
        return new File(name)
    }
    
    protected static String sanitizeDropboxFolderName(String folder) {
        return (folder.startsWith('/') ? '' : '/') + (folder.endsWith('/') ? folder : folder + '/')
    }

    protected getDropboxClient() {
        File settingsFile = getHomeBasedFile(credentials)

        if (!settingsFile.exists()) {
            error "Settings file ${settingsFile.absolutePath} does not exist"
            System.exit(-1)
        }

        Map<String, String> settings = new JsonSlurper().parse(settingsFile) as Map<String, String>
        DbxRequestConfig config = new DbxRequestConfig("Dropbox-Tool")

        new DbxClientV2(config, settings.access_token)

    }
    
}
