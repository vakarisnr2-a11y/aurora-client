package dev.aurora.client.mixin;

import dev.aurora.client.chat.ChatPipeline;
import dev.aurora.client.chat.ChatSearchState;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Shadow
    public abstract void addMessage(Text message, MessageSignatureData signature, MessageIndicator indicator);

    /**
     * Every chat line (addMessage(Text) delegates here, checked with javap against 1.21.1). A changed message is
     * re-added once with the pipeline bypassed, so vanilla still logs, wraps and stores it.
     */
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"), cancellable = true)
    private void aurora$process(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        if (ChatPipeline.busy()) return;
        ChatPipeline.Result result = ChatPipeline.process((ChatHud) (Object) this, message, indicator);
        if (result == null) return;
        ci.cancel();
        Text text = result.text;
        MessageIndicator ind = result.indicator;
        ChatPipeline.begin();
        try {
            addMessage(text, signature, ind);
        } finally {
            ChatPipeline.end();
        }
    }

    /** The 100-entry caps of the message list, the wrapped lines and the sent-message history (one each). */
    @ModifyConstant(method = {"addVisibleMessage", "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", "addToMessageHistory"},
        constant = @Constant(intValue = 100))
    private int aurora$historySize(int original) {
        return ChatPipeline.historySize(original);
    }

    /** Chat search: messages that do not match the query get no wrapped lines (so they are not shown). */
    @Inject(method = "addVisibleMessage", at = @At("HEAD"), cancellable = true)
    private void aurora$search(ChatHudLine message, CallbackInfo ci) {
        if (ChatSearchState.isFiltering() && !ChatSearchState.matches(message.content().getString())) ci.cancel();
    }
}
