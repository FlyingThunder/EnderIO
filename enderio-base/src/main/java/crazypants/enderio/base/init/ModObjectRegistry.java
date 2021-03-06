package crazypants.enderio.base.init;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.common.util.NNList;
import com.enderio.core.common.util.NullHelper;

import crazypants.enderio.api.IModObject;
import crazypants.enderio.api.IModTileEntity;
import crazypants.enderio.api.addon.IEnderIOAddon;
import crazypants.enderio.base.EnderIO;
import crazypants.enderio.base.Log;
import crazypants.enderio.base.events.EnderIOLifecycleEvent;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

@EventBusSubscriber(modid = EnderIO.MODID)
public class ModObjectRegistry {

  private static final @Nonnull ResourceLocation NAME = new ResourceLocation(EnderIO.DOMAIN, "modobject");
  private static IForgeRegistry<IModObject> REGISTRY = null;

  @SubscribeEvent
  public static void registerRegistry(@Nonnull RegistryEvent.NewRegistry event) {
    REGISTRY = new RegistryBuilder<IModObject>().setName(NAME).setType(IModObject.class).setIDRange(0, 0x00FFFFFF).create();
  }

  @SubscribeEvent
  public static void registerRegistry(@Nonnull EnderIOLifecycleEvent.PreInit event) {
    MinecraftForge.EVENT_BUS.post(new RegisterModObject(NAME, NullHelper.notnullF(REGISTRY, "RegistryBuilder.create()")));
  }

  // ---

  private static final Map<Object, IModObject> reverseMapping = new IdentityHashMap<>();
  private static final NNList<IModTileEntity> tileEntities = new NNList<>();

  public static <T extends Enum<T> & IModTileEntity> void addModTileEntities(Class<T> enumClass) {
    tileEntities.addAll(Arrays.asList(enumClass.getEnumConstants()));
  }

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public static void registerBlocksEarly(@Nonnull RegisterModObject event) {
    event.register(ModObject.class);
  }

  @SubscribeEvent(priority = EventPriority.NORMAL)
  public static void registerBlocks(@Nonnull RegistryEvent.Register<Block> event) {
    for (IModObject mo : REGISTRY) {
      Block block = mo.getBlockCreator().apply(mo);
      if (block != null) {
        mo.setBlock(block);
        event.getRegistry().register(block);
        reverseMapping.put(block, mo);
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOW)
  public static void registerAddonBlocks(@Nonnull RegistryEvent.Register<Block> event) {
    for (ModContainer modContainer : Loader.instance().getModList()) {
      Object mod = modContainer.getMod();
      if (mod instanceof IEnderIOAddon) {
        ((IEnderIOAddon) mod).injectBlocks(NullHelper.notnullF(event.getRegistry(), "RegistryEvent.Register<Block>.getRegistry()"));
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void registerTileEntities(@Nonnull RegistryEvent.Register<Block> event) {
    registerTeClasses(); // Note: Lex says this goes into the block register event
  }

  @SubscribeEvent
  public static void registerItems(@Nonnull RegistryEvent.Register<Item> event) {
    for (IModObject mo : REGISTRY) {
      Item item = mo.getItemCreator().apply(mo, mo.getBlock());
      if (item != null) {
        mo.setItem(item);
        event.getRegistry().register(item);
        reverseMapping.put(item, mo);
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void registerOredict(@Nonnull RegistryEvent.Register<Item> event) {
    // oredict registration goes here
  }

  public static void init(@Nonnull FMLInitializationEvent event) {
    for (IModObject mo : REGISTRY) {
      final Block block = mo.getBlock();
      if (block instanceof IModObject.LifecycleInit) {
        ((IModObject.LifecycleInit) block).init(mo, event);
      }
      Item item = mo.getItem();
      if (item instanceof IModObject.LifecycleInit) {
        ((IModObject.LifecycleInit) item).init(mo, event);
      }
    }
  }

  public static void init(@Nonnull FMLPostInitializationEvent event) {
    for (IModObject mo : REGISTRY) {
      final Block block = mo.getBlock();
      if (block instanceof IModObject.LifecyclePostInit) {
        ((IModObject.LifecyclePostInit) block).init(mo, event);
      }
      Item item = mo.getItem();
      if (item instanceof IModObject.LifecyclePostInit) {
        ((IModObject.LifecyclePostInit) item).init(mo, event);
      }
    }
  }

  private static void registerTeClasses() {
    for (IModTileEntity te : tileEntities) {
      Log.debug("Registering TileEntity " + te.getUnlocalisedName() + " as " + te.getRegistryName().toString());
      GameRegistry.registerTileEntity(te.getTileEntityClass(), te.getRegistryName());
    }
  }

  public static @Nonnull String sanitizeName(@Nonnull String name) {
    return name.replaceAll("([A-Z])", "_$0").replaceFirst("^_", "").toLowerCase(Locale.ENGLISH);
  }

  public static @Nullable IModObject getModObject(@Nonnull Block forBlock) {
    return reverseMapping.get(forBlock);
  }

  public static @Nonnull IModObject getModObjectNN(@Nonnull Block forBlock) {
    return NullHelper.notnull(reverseMapping.get(forBlock), "missing modObject");
  }

  public static @Nullable IModObject getModObject(@Nonnull Item forItem) {
    return reverseMapping.get(forItem);
  }

  public static @Nonnull IModObject getModObjectNN(@Nonnull Item forItem) {
    return NullHelper.notnull(reverseMapping.get(forItem), "missing modObject");
  }

  public static @Nonnull ForgeRegistry<IModObject> getRegistry() {
    return (ForgeRegistry<IModObject>) NullHelper.notnull(REGISTRY, "accessing modobject registry too early");
  }

}
