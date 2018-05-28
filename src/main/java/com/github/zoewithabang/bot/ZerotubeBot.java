package com.github.zoewithabang.bot;

import com.github.zoewithabang.command.GetUrl;
import com.github.zoewithabang.command.ICommand;
import org.apache.commons.io.input.ReversedLinesFileReader;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.RequestBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ZerotubeBot implements IBot
{
    private IDiscordClient client;
    private Properties properties;
    private Map<String, ICommand> commands;
    private Runnable updateCheck;
    private ScheduledExecutorService executor;
    private String nowPlaying = "";
    
    public ZerotubeBot(IDiscordClient client, Properties properties)
    {
        this.client = client;
        this.properties = properties;
        commands = new HashMap<>();
        
        //add bot commands to Map
        commands.put("url", new GetUrl(this, properties.getProperty("url")));
        
        //initialise log update checker
        updateCheck = this::checkLogForUpdate;
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(updateCheck, 10, 2, TimeUnit.SECONDS);
    }
    
    @Override
    public void sendMessage(IChannel channel, String message)
    {
        RequestBuffer.request(() ->
            {
                try
                {
                    LOGGER.debug("[ZEROTUBEBOT] Sending message '{}' to channel '{}'.", message, channel.getName());
                    channel.sendMessage(message);
                }
                catch(DiscordException e)
                {
                    LOGGER.error("[ZEROTUBEBOT] Failed to send message to channel '{}'.", channel.getName(), e);
                }
            }
        );
    }
    
    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event)
    {
        String prefix = properties.getProperty("prefix");
        
        //separate message by spaces, args[0] will have the command, if this is a message for the bot
        String[] args = event.getMessage().getContent().split(" ");
        
        //if a message doesn't start with the bot's prefix, ignore it
        if(args.length == 0
            || !args[0].startsWith(prefix))
        {
            return;
        }
        
        //get the actual command, minus the bot's prefix
        String command = args[0].substring(prefix.length());
        
        //put the args into an ArrayList, removing the command
        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        argsList.remove(0);
        
        //execute command (if known)
        if(commands.containsKey(command))
        {
            LOGGER.debug("[ZEROTUBEBOT] Received command, running '{}'.", command);
            commands.get(command).execute(event, argsList);
        }
        else
        {
            LOGGER.warn("[ZEROTUBEBOT] Received unknown command '{}'.", command);
        }
    }
    
    private void checkLogForUpdate()
    {
        try(ReversedLinesFileReader reader = new ReversedLinesFileReader(new File(properties.getProperty("chanloglocation")), StandardCharsets.UTF_8))
        {
            String line;
            final int YOUTUBE_SUFFIX = 17;
            
            //reading over log
            while((line = reader.readLine()) != null)
            {
                LOGGER.debug("[ZEROTUBEBOT] Current log line: {}", line);
                String[] lineSplitOnPlaylistTag = line.split(Pattern.quote("[playlist] Now playing: "));
                //if this line has a "now playing" entry
                if(lineSplitOnPlaylistTag.length > 1
                    && lineSplitOnPlaylistTag[1] != null
                    && !lineSplitOnPlaylistTag[1].equals(""))
                {
                    String title = lineSplitOnPlaylistTag[1].substring(0, lineSplitOnPlaylistTag[1].length() - YOUTUBE_SUFFIX);
                    LOGGER.debug("[ZEROTUBEBOT] After N/P: {}", lineSplitOnPlaylistTag[1]);
                    LOGGER.debug("[ZEROTUBEBOT] Title: {}", title);
                    if(!nowPlaying.equals(title))
                    {
                        nowPlaying = title;
                        updatePresence();
                    }
                    
                    //either presence is updated or doesn't need to be updated, end
                    return;
                }
            }
        }
        catch(IOException e)
        {
            LOGGER.error("[ZEROTUBEBOT] Could not find the channel log location '{}'.", properties.getProperty("chanloglocation"), e);
        }
    }
    
    private void updatePresence()
    {
        LOGGER.debug("[ZEROTUBEBOT] Updating bot presence to '{}'.", nowPlaying);
        client.changePresence(StatusType.ONLINE, ActivityType.PLAYING, nowPlaying);
    }
}
