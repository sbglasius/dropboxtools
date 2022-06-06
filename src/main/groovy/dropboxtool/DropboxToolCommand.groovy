package dropboxtool

import io.micronaut.configuration.picocli.PicocliRunner
import picocli.CommandLine.Command

@Command(name = 'dropboxtool', description = 'Tiny command line wrapper for Dropbox',
        mixinStandardHelpOptions = true, subcommands = [ AuthCommand , PutCommand, GetCommand ])
class DropboxToolCommand implements Runnable {

    static void main(String[] args) throws Exception {
        PicocliRunner.run(DropboxToolCommand.class, args)
    }

    void run() {
    }
}
