package com.github.zoewithabang;

import com.github.zoewithabang.bot.ZerotubeBot;
import org.slf4j.Logger;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

class BotManager
{
    private static Logger LOGGER = LogUtils.getLogger();
    private static HashMap<String, Properties> botProperties = new HashMap<>();
    private static IDiscordClient clientZerotubeBot;
    
    static void init()
    {
        getProperties();
        run();
    }
    
    static void getProperties()
    {
        try
        {
            InputStream zerotubeBotPropertyStream = BotManager.class.getClassLoader().getResourceAsStream("zerotubebot.properties");
            Properties zerotubeBotProperties = new Properties();
            zerotubeBotProperties.load(zerotubeBotPropertyStream);
            
            botProperties.put("ZerotubeBot", zerotubeBotProperties);
        }
        catch(IOException e)
        {
            LOGGER.error("[BOTMANAGER] IOException on getting ZerotubeBot properties file.", e);
        }
        catch(NullPointerException e)
        {
            LOGGER.error("[BOTMANAGER] NullPointerException on loading ZerotubeBot properties file.", e);
        }
    }
    
    static void run()
    {
        try
        {
            Properties zerotubeBotProperties = botProperties.get("ZerotubeBot");
            
            clientZerotubeBot = new ClientBuilder()
                .withToken(zerotubeBotProperties.getProperty("token"))
                .withRecommendedShardCount()
                .build();
        
            clientZerotubeBot.getDispatcher().registerListener(new ZerotubeBot(clientZerotubeBot, zerotubeBotProperties));
        
            clientZerotubeBot.login();
        }
        catch(DiscordException e)
        {
            LOGGER.error("[BOTMANAGER] DiscordException when creating ZerotubeBot.", e);
        }
    }
}
