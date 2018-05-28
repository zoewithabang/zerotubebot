package com.github.zoewithabang.command;

import com.github.zoewithabang.bot.IBot;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import java.util.List;

public class GetUrl implements ICommand
{
    private IBot bot;
    private String url;
    
    public GetUrl(IBot bot, String url)
    {
        this.bot = bot;
        this.url = url;
    }
    
    @Override
    public void execute(MessageReceivedEvent event, List<String> args)
    {
        bot.sendMessage(event.getChannel(), "\uD83C\uDFB5 " + url + " \uD83C\uDFB5");
    }
}
