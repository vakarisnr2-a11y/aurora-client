package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.chat.ChatSearchState;
import dev.aurora.client.module.modules.misc.ChatCopy;
import dev.aurora.client.module.modules.misc.ChatSearch;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Shadow
    protected TextFieldWidget chatField;

    @Unique
    private TextFieldWidget aurora$search;

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    /** Chat Search's field, bottom right above the chat input (re-created on resize, keeping the query). */
    @Inject(method = "init", at = @At("TAIL"))
    private void aurora$addSearch(CallbackInfo ci) {
        aurora$search = null;
        ChatSearch search = AuroraClient.module(ChatSearch.class);
        if (search == null || !search.isEnabled()) return;
        TextFieldWidget field = new TextFieldWidget(textRenderer, width - 126, height - 28, 122, 12, Text.literal("Search chat"));
        field.setMaxLength(64);
        field.setPlaceholder(Text.literal("Search chat… (Ctrl+F)").formatted(Formatting.DARK_GRAY));
        field.setText(ChatSearchState.query());
        field.setChangedListener(ChatSearch::setQuery);
        aurora$search = addDrawableChild(field);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void aurora$clearSearch(CallbackInfo ci) {
        if (ChatSearchState.isFiltering()) ChatSearch.setQuery("");
    }

    /** Ctrl+F focuses the search field; Enter or Tab in it goes back to the chat input. */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void aurora$searchKeys(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (aurora$search == null) return;
        if (keyCode == GLFW.GLFW_KEY_F && hasControlDown()) {
            setFocused(aurora$search);
            cir.setReturnValue(true);
        } else if (getFocused() == aurora$search && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
            || keyCode == GLFW.GLFW_KEY_TAB)) {
            setFocused(chatField);
            cir.setReturnValue(true);
        }
    }

    /** Chat Copy: the configured click on a message copies it. */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void aurora$copyMessage(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        ChatCopy copy = AuroraClient.module(ChatCopy.class);
        if (copy == null || !copy.isEnabled() || !copy.matches(button) || client == null) return;
        if (aurora$search != null && aurora$search.isMouseOver(mouseX, mouseY)) return;
        if (copy.copyAt(client.inGameHud.getChatHud(), mouseX, mouseY)) cir.setReturnValue(true);
    }
}
