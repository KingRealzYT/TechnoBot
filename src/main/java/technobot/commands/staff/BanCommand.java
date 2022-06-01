package technobot.commands.staff;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import technobot.TechnoBot;
import technobot.commands.Category;
import technobot.commands.Command;
import technobot.data.GuildData;
import technobot.util.EmbedColor;
import technobot.util.EmbedUtils;

import java.util.Date;

/**
 * Command that bans a user from the guild.
 *
 * @author TechnoVision
 */
public class BanCommand extends Command {

    public BanCommand(TechnoBot bot) {
        super(bot);
        this.name = "ban";
        this.description = "Bans a user from your server.";
        this.category = Category.STAFF;
        this.args.add(new OptionData(OptionType.USER, "user", "The user to ban", true));
        this.args.add(new OptionData(OptionType.INTEGER, "days", "Time duration for the ban in days").setMinValue(1).setMaxValue(1825));
        this.args.add(new OptionData(OptionType.STRING, "reason", "Reason for the ban"));
        this.permission = Permission.BAN_MEMBERS;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        // Get command and member data
        User user = event.getOption("user").getAsUser();
        if (user.getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("Did you seriously expect me to ban myself?")).queue();
            return;
        }
        OptionMapping reasonOption = event.getOption("reason");
        String reason = reasonOption != null ? reasonOption.getAsString() : "Unspecified";

        OptionMapping daysOption = event.getOption("days");
        String duration = daysOption != null ? daysOption.getAsInt()+" Days" : "Forever";

        // Start unban timer if temp ban specified
        Guild guild = event.getGuild();
        GuildData data = GuildData.get(guild);
        OptionMapping timeOption = event.getOption("days");
        String content = user.getAsTag() + " has been banned";
        if (timeOption != null) {
            int days = timeOption.getAsInt();
            content += " for " + days + " day";
            if (days > 1) content += "s";
            data.moderationHandler.scheduleUnban(guild, user,  days);
        }
        else if (data.moderationHandler.hasTimedBan(user.getId())) {
            // Remove timed ban in favor of permanent ban
            data.moderationHandler.removeBan(user);
        }

        // Ban user from guild
        user.openPrivateChannel().queue(privateChannel -> {
            // Private message user with reason for Ban
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(EmbedColor.ERROR.color)
                    .setTitle(EmbedUtils.RED_X + " You were banned!")
                    .addField("Server", guild.getName(), false)
                    .addField("Duration", duration, false)
                    .addField("Reason", reason, false)
                    .setTimestamp(new Date().toInstant());
            privateChannel.sendMessageEmbeds(embed.build()).queue(
                    message -> guild.ban(user, 7, reason).queue(),
                    failure -> guild.ban(user, 7, reason).queue());
            }
        );

        // Send confirmation message
        event.getHook().sendMessageEmbeds(new EmbedBuilder()
                .setAuthor(content, null, user.getEffectiveAvatarUrl())
                .setDescription("**Reason:** " + reason)
                .setColor(EmbedColor.DEFAULT.color)
                .build()
        ).queue();
    }
}
