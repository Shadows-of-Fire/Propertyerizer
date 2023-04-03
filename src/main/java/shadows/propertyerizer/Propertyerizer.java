package shadows.propertyerizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.placebo.config.Configuration;
import shadows.placebo.util.RunnableReloader;

@Mod(Propertyerizer.MODID)
public class Propertyerizer {

	public static final String MODID = "propertyerizer";
	public static final Logger LOGGER = LogManager.getLogger(MODID);
	public static float globalMaxSpeed = 29.999F;

	public Propertyerizer() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::load);
		MinecraftForge.EVENT_BUS.register(this);
	}

	public static void loadAndApply() {
		Configuration cfg = new Configuration(MODID);
		String[] unparsed = cfg.getStringList("Property Overrides", "general", new String[0], "A list of property overrides, in the form <regname>,<hardness>,<blast resistance>,<requires tool>.  Types are <string>,<float>,<float>,<boolean>.  You can use Z to ignore a value.");
		for (String s : unparsed) {
			try {
				String[] split = s.split(",");
				Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(split[0]));
				float destroySpeed = split[1].equals("Z") ? Float.MIN_VALUE : Float.parseFloat(split[1]); //speed or min value, if not setting
				float resistance = split[2].equals("Z") ? Float.MIN_VALUE : Float.parseFloat(split[2]); //res or min value, if not setting
				int needsTool = split[3].equals("Z") ? -1 : Boolean.parseBoolean(split[3]) ? 1 : 0; //1 if forcing true, 0 if forcing false, -1 for not setting
				if (resistance != Float.MIN_VALUE) ObfuscationReflectionHelper.setPrivateValue(BlockBehaviour.class, b, resistance, "f_60444_"); // BlockBehaviour#explosionResistance
				b.getStateDefinition().getPossibleStates().forEach(state -> {
					if (destroySpeed != Float.MIN_VALUE) ObfuscationReflectionHelper.setPrivateValue(BlockStateBase.class, state, destroySpeed, "f_60599_"); // BlockStateBase#destroySpeed
					if (needsTool != -1) ObfuscationReflectionHelper.setPrivateValue(BlockStateBase.class, state, needsTool == 1, "f_60600_"); // BlockStateBase#requiresCorrectToolForDrops
				});
			} catch (Exception ex) {
				LOGGER.info("Skipping invalid config entry {}.", s);
			}
		}
		globalMaxSpeed = cfg.getFloat("Global Max Speed", "general", globalMaxSpeed, 0, 100, "Global maximum break speed, in units of... something. 30 == instant");
		if (cfg.hasChanged()) cfg.save();
	}

	public void load(FMLCommonSetupEvent e) {
		loadAndApply();
	}

	@SubscribeEvent
	public void reload(AddReloadListenerEvent e) {
		e.addListener(new RunnableReloader(Propertyerizer::loadAndApply));
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void breakSpeed(BreakSpeed e) {
		e.setNewSpeed(Math.min(globalMaxSpeed, e.getNewSpeed()));
	}

}
