package team.creative.solonion.client;

import static team.creative.solonion.lib.Localization.localized;
import static team.creative.solonion.lib.Localization.localizedComponent;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import team.creative.solonion.SOLOnion;
import team.creative.solonion.api.FoodCapability;
import team.creative.solonion.api.SOLOnionAPI;
import team.creative.solonion.client.gui.screen.FoodBookScreen;
import team.creative.solonion.client.gui.screen.FoodContainerScreen;
import team.creative.solonion.item.foodcontainer.FoodContainer;
import team.creative.solonion.lib.Localization;

public class SOLOnionClient {
    
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, SOLOnion.MODID);
    public static final RegistryObject<MenuType<FoodContainer>> FOOD_CONTAINER = MENU_TYPES.register("food_container", () -> IForgeMenuType.create(
        ((windowId, inv, data) -> new FoodContainer(windowId, inv, inv.player))));
    
    public static KeyMapping OPEN_FOOD_BOOK;
    
    public static void load(IEventBus bus) {
        bus.addListener(SOLOnionClient::setupClient);
        bus.addListener(SOLOnionClient::registerKeybinds);
        MinecraftForge.EVENT_BUS.addListener(SOLOnionClient::handleKeypress);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, SOLOnionClient::onItemTooltip);
        MENU_TYPES.register(bus);
    }
    
    public static void setupClient(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(FOOD_CONTAINER.get(), FoodContainerScreen::new));
    }
    
    public static void registerKeybinds(RegisterKeyMappingsEvent event) {
        event.register(OPEN_FOOD_BOOK = new KeyMapping(Localization.localized("key", "open_food_book"), InputConstants.UNKNOWN.getValue(), Localization.localized("key",
            "category")));
    }
    
    public static void handleKeypress(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
            return;
        
        if (OPEN_FOOD_BOOK != null && OPEN_FOOD_BOOK.isDown())
            FoodBookScreen.open(player);
    }
    
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!SOLOnion.CONFIG.isFoodTooltipEnabled)
            return;
        
        Player player = event.getEntity();
        if (player == null)
            return;
        
        ItemStack stack = event.getItemStack();
        if (!stack.isEdible())
            return;
        
        FoodCapability food = SOLOnionAPI.getFoodCapability(player);
        addTooltip(food.simulateEat(stack), food.getLastEaten(stack), stack, event.getToolTip());
    }
    
    public static void addTooltip(double diversity, int lastEaten, ItemStack stack, List<Component> tooltip) {
        boolean isAllowed = SOLOnion.CONFIG.isAllowed(stack);
        
        if (!isAllowed) {
            tooltip.add(localizedComponent("gui", "tooltip.disabled").withStyle(style -> style.applyFormat(ChatFormatting.DARK_GRAY)));
            return;
        }
        
        ChatFormatting color = ChatFormatting.GRAY;
        if (diversity < 0)
            color = ChatFormatting.RED;
        else if (diversity > 0)
            color = ChatFormatting.GREEN;
        tooltip.add(Component.literal(localized("gui", "tooltip.diversity") + ": " + String.format("%.2f", SOLOnion.CONFIG.getDiversity(stack))).withStyle(ChatFormatting.GRAY)
                .append(" (").append(Component.literal(String.format("%.2f", diversity)).withStyle(color)).append(")"));
        if (lastEaten != -1) {
            String last_eaten_path = "tooltip.last_eaten";
            if (lastEaten == 1)
                last_eaten_path = "tooltip.last_eaten_singular";
            tooltip.add(Component.literal(localized("gui", last_eaten_path, lastEaten)).withStyle(ChatFormatting.GRAY));
        }
    }
}