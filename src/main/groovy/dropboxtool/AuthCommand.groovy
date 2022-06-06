package dropboxtool

import com.dropbox.core.DbxAppInfo
import com.dropbox.core.DbxAuthFinish
import com.dropbox.core.DbxAuthInfo
import com.dropbox.core.DbxException
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxWebAuth
import com.dropbox.core.json.JsonReader
import groovy.util.logging.Slf4j
import picocli.CommandLine

@Slf4j
@CommandLine.Command(name = 'auth')
class AuthCommand extends BaseCommand {

    @CommandLine.Option(names = ['--app-info'], arity = "1", description = "Dropbox application info file (default: '~/.dropbox-tool.json')")
    String appInfo = '~/.dropbox-tool.json'

    @CommandLine.Option(names = ['--key'], arity = "1", description = "Dropbox key for application info file (default: not set)")
    String dropboxAuthKey 

    @CommandLine.Option(names = ['--secret'], arity = "1", description = "Dropbox secret for application info file (default: not set)")
    String dropboxAuthSecret 

    @Override
    Integer call() throws Exception {

        def appInfoFile = getHomeBasedFile(appInfo)
        def argAuthFile = getHomeBasedFile(credentials)

        if (!appInfoFile.exists() && !dropboxAuthKey) {
            println """\
                Remember to create $appInfoFile.path with 
                with information about your API app.  Example:
                {
                    "key"   : "Your Dropbox API app key...",
                    "secret": "Your Dropbox API app secret..."
                }   
                    
                Get an API app key by registering with Dropbox:
                https://dropbox.com/developers/apps
                    
                If authorization is successful, the resulting API access token 
                will be saved to this file".
                
                A placeholder file was created.
                
                Or use --key=yourKey --secret=yourSecret on the commandline
                """.stripIndent()
            appInfoFile.text = """\
                {
                    "key"   : "Your Dropbox API app key...",
                    "secret": "Your Dropbox API app secret..."
                }   
                """.stripIndent()

            return 1

        }
        
        if(dropboxAuthKey && dropboxAuthSecret) {
            appInfoFile.text = """\
                {
                    "key"   : "$dropboxAuthKey",
                    "secret": "$dropboxAuthSecret"
                }   
                """.stripIndent()
        }

        // Read app info file (contains app key and app secret)

        DbxAppInfo appInfo
        try {
            appInfo = DbxAppInfo.Reader.readFromFile(appInfoFile)
        } catch (JsonReader.FileLoadException ex) {
            log.error("Error reading $appInfoFile.absolutePath: $ex.message")
            return 1
        }

        // Run through Dropbox API authorization process
        DbxRequestConfig requestConfig = new DbxRequestConfig("DropboxTool")
        DbxWebAuth webAuth = new DbxWebAuth(requestConfig, appInfo)
        DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
                .withNoRedirect()
                .build()

        String authorizeUrl = webAuth.authorize(webAuthRequest)
        String code = System.console().readLine("""\
            1. Go to $authorizeUrl
            2. Click "Allow" (you might have to log in first).
            3. Copy the authorization code.
            Enter the authorization code here: """.stripIndent())

        if (code == null) {
            return 1
        }
        code = code.trim()

        DbxAuthFinish authFinish
        try {
            authFinish = webAuth.finishFromCode(code)
        } catch (DbxException ex) {
            log.error("Error in DbxWebAuth.authorize: $ex.message")
            return 1
        }

        log.info("""\
        Authorization complete.
        - User ID     : ${authFinish.userId}
        - Access Token: ${authFinish.accessToken}""".stripIndent())

        // Save auth information to output file.
        DbxAuthInfo authInfo = new DbxAuthInfo(authFinish.accessToken, appInfo.host)
        try {
            DbxAuthInfo.Writer.writeToFile(authInfo, argAuthFile)
            log.info("""Saved authorization information to "$argAuthFile.canonicalPath".""")
        } catch (IOException ex) {
            log.error("Error saving to $argAuthFile.absolutePath: $ex.message\n${DbxAuthInfo.Writer.writeToString(authInfo)}")
            return 1
        }

        return 0
    }
}
