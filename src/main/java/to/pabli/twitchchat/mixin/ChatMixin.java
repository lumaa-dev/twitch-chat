package to.pabli.twitchchat.mixin;

import java.util.Date;
import net.fabricmc.fabric.impl.client.indigo.IndigoMixinConfigPlugin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import to.pabli.twitchchat.TwitchChatMod;
import to.pabli.twitchchat.config.ModConfig;
import to.pabli.twitchchat.twitch_integration.CalculateMinecraftColor;

@Mixin(Screen.class)
public class ChatMixin {
	@Inject(at = @At("HEAD"), method = "sendMessage(Ljava/lang/String;Z)V", cancellable = true)
	private void sendMessage(String text, boolean showInHistory, CallbackInfo info) {
      ModConfig config = ModConfig.getConfig();


      String prefix = config.getPrefix();

      // Allow users to write /twitch commands (such as disabling and enabling the mod) when their prefix is "".
      if (prefix.equals("") && text.startsWith("/twitch")) {
        return; // Don't cancel the message, return execution to the real method
      }

      // If the message is a twitch message
      if (text.startsWith(prefix)) {
        if (TwitchChatMod.bot != null && TwitchChatMod.bot.isConnected()) {
          String textWithoutPrefix = text.substring(text.indexOf(prefix) + prefix.length());
          TwitchChatMod.bot.sendMessage(textWithoutPrefix); // Send the message to the Twitch IRC Chat

          Date currentTime = new Date();
          String formattedTime = TwitchChatMod.formatDateTwitch(currentTime);

          String username = TwitchChatMod.bot.getUsername();
          Formatting userColor;
          if (TwitchChatMod.bot.isFormattingColorCached(username)) {
            userColor = TwitchChatMod.bot.getFormattingColor(username);
          } else {
            userColor = CalculateMinecraftColor.getDefaultUserColor(username);
            TwitchChatMod.bot.putFormattingColor(username, userColor);
          }

          boolean isMeMessage = textWithoutPrefix.startsWith("/me");

          // Add the message to the Minecraft Chat
          TwitchChatMod.addTwitchMessage(formattedTime, username, isMeMessage ? textWithoutPrefix.substring(4) : textWithoutPrefix, userColor, isMeMessage);
          MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(text);
          info.cancel();
        } else {
          TwitchChatMod.addNotification(Text.translatable("text.twitchchat.chat.integration_disabled"));
        }
      }
	}
}
