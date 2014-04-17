package com.cryptocoinpartners.bin;

import com.beust.jcommander.*;
import com.cryptocoinpartners.bin.command.Command;
import com.cryptocoinpartners.util.Config;
import com.cryptocoinpartners.util.ReflectionUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.security.GeneralSecurityException;
import java.util.*;


/**
 * This is the only entry point required into the Cryptocoin Partners executable world.  Depending on parameters passed,
 * this will act as a ticker plant, a strategy backtester, or an online trading system, either paper trading or live.
 */
public class Main
{
    static final String DEFAULT_PROPERTIES_FILENAME = "trader.properties";
    static final String FALLBACK_PROPERTIES_FILENAME = "trader-default.properties";

    static class MainParams {
        @SuppressWarnings("UnusedDeclaration")
        @Parameter(names = {"h","help","-h","-H","-help","--help"}, help = true, description = "Show this usage help")
        boolean help;

        @Parameter(names = {"-f","--properties-file"}, description = "location of the trader.properties config file")
        String propertiesFilename = DEFAULT_PROPERTIES_FILENAME;

        @DynamicParameter( names = {"-D"}, description = "use the -D flag to set configuration properties \"-Ddb.username=dbuser\"" )
        Map<String,String> definitions = new HashMap<String, String>();
    }


    public static void main( String[] args ) throws ConfigurationException, IllegalAccessException, InstantiationException {
        MainParams mainParams = new MainParams();
        JCommander parameterParser = new JCommander(mainParams);
        parameterParser.setProgramName(Main.class.getName());

        // find the commands, register with the parameter parser, and put them into the commandLookup map
        Map<String,Command> commandLookup = new HashMap<String,Command>();
        Set<Class<? extends Command>> commands = ReflectionUtil.getSubtypesOf(Command.class);
        for( Class<? extends Command> commandType : commands ) {
            Command command = commandType.newInstance();
            Parameters annotation = command.getClass().getAnnotation(Parameters.class);
            if( annotation == null ) {
                System.err.println("The command class "+ command.getClass()+" must have the com.beust.jcommander.Parameters annotation.");
                System.exit(1);
            }
            for( String commandName : annotation.commandNames() ) {
                commandLookup.put(commandName,command);
            }
            parameterParser.addCommand(command);
        }

        // now parse the commandline
        parameterParser.parse(args);
        String commandName = parameterParser.getParsedCommand();
        // find the command, if any
        Command command = commandLookup.get(commandName);
        if( command == null || mainParams.help ) {
            parameterParser.usage();
            System.exit(7001);
        }
        try {
            Config.init(mainParams.propertiesFilename,mainParams.definitions);
        }
        catch( ConfigurationException e ) {
            if( !mainParams.propertiesFilename.equals(DEFAULT_PROPERTIES_FILENAME) )
                throw e;
            LoggerFactory.getLogger(Main.class).info(DEFAULT_PROPERTIES_FILENAME+" not found.  Using "+FALLBACK_PROPERTIES_FILENAME+" instead.");
            Config.init(FALLBACK_PROPERTIES_FILENAME,mainParams.definitions);
        }
        command.run();
    }


}