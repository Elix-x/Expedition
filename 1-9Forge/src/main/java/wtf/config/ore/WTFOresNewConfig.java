package wtf.config.ore;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;

import wtf.Core;
import wtf.blocks.BlockDenseOre;
import wtf.blocks.BlockDenseOreFalling;
import wtf.blocks.redstone.DenseRedstoneOre;
import wtf.config.ConfigMaster;
import wtf.init.BlockSets;
import wtf.init.WTFBlocks;
import wtf.ores.OreGenAbstract;
import wtf.ores.OreGenerator;
import wtf.ores.OreReplacer;
import wtf.ores.VanillOreGenCatcher;
import wtf.ores.oregenerators.OreGenCaveFloor;
import wtf.ores.oregenerators.OreGenCloud;
import wtf.ores.oregenerators.OreGenCluster;
import wtf.ores.oregenerators.OreGenSingle;
import wtf.ores.oregenerators.OreGenUnderWater;
import wtf.ores.oregenerators.OreGenVanilla;
import wtf.ores.oregenerators.OreGenVein;
import wtf.utilities.BlockstateWriter;
import wtf.utilities.UBCCompat;
import wtf.utilities.wrappers.StoneAndOre;

public class WTFOresNewConfig extends ConfigMaster{
	public static boolean rotate180only;

	public static HashSet<IBlockState> cancelOres = new HashSet<IBlockState>(); 

	public static boolean simplexGen;

	public static enum GENTYPE{
		vanilla,
		vein,
		cloud,
		cluster,
		single,
		cave,
		underwater;
	}

	public static void loadConfig() throws Exception {

		Configuration config = new Configuration(new File(configPath+"WTFOresConfigV2.cfg"));

		config.load();

		simplexGen = config.get("0 General Options", "Use simplex noise instead of random for ore generation", true).getBoolean();
		rotate180only = config.get("0 Blockstate Options", "When generating new blockstates, create variants with 180 rotation only, setting to false enables 90 adn 270 degree rotations.  Requires generating new blockstates, and placing them in a resource pack", true).getBoolean();

		String[] defOres = {"minecraft:coal_ore@0 #vein", "minecraft:iron_ore@0 #vein", "minecraft:gold_ore@0 #cloud", "minecraft:lapis_ore@0 #cluster", "minecraft:redstone_ore@0 #vein", "minecraft:emerald_ore@0 #single", "minecraft:diamond_ore@0 #single",
				"wtfcore:nitre_ore@0 #cave&single", "wtfcore:oreSandGold@0 #underwater&single", "wtfcore:stone0DecoStatic@2 #vein"};

		String[] oreSet = config.get("1 Master Ore List", "Master list of all ores generated, add ore blocks to this list, then run minecraft, in order to generate their config options", defOres).getStringList();

		HashMap<String, OreDefReg> defPresets = OreConfigHelper.getPresets();

		for (String oreGenString : oreSet){

			String oreString = oreGenString.split("#")[0].replaceAll("\\s","");
			String genString = oreGenString.split("#")[1].replaceAll("\\s","");

			IBlockState oreState = getBlockState(oreString);

			if (oreState == null){
				config.get("Config for " +oreGenString, "Block not found", "unable to find block for "+oreGenString);
				break;
			}

			cancelOres.add(oreState);
			OreDefReg preset = defPresets.get(oreString);

			if (preset == null){
				preset = defPresets.get("minecraft:iron_ore@0");
			}

			//Background stones
			String stoneStringList = config.get("Config for " +oreGenString, "0 List of background stones", preset.stoneList).getString().replaceAll("\\s","");

			ArrayList<IBlockState> stoneArray = getBlockStateArray(stoneStringList);

			int[] genRange = config.get("Config for " +oreGenString, "2 Generation height range (min % surface height, max % surface height)", preset.genRange).getIntList();
			int[] orePerChunk = config.get("Config for " +oreGenString, "1 Amount of ore to attempt to generate per chunk (min, max)", preset.orePerChunk).getIntList();
			boolean denseBlock = config.get("Config for " +oreGenString, "4 Use dense versions of this ore block", preset.denseBlock).getBoolean();

			OreGenAbstract generator = getGenerator(oreGenString, preset, config, genString, oreState, genRange, orePerChunk, denseBlock);

			String textureLoc = config.get("Config for " +oreGenString, "5 Ore texture", preset.textureLoc == null ? oreState.getBlock().getRegistryName().toString().split(":")[1] : preset.textureLoc).getString();

			int[] overworld = {0};
			int[] dimensionIDs = config.get("Config for " +oreGenString, "3 Dimensions to spawn in", preset.dimensionIDs==null ? overworld : preset.dimensionIDs).getIntList();
			for (int ID : dimensionIDs){
				generator.dimension.add(ID);
			}

			float density = config.get("Config for " +oreGenString, "6 Vein percent density (chance each block will generate or not)", preset.density).getInt()/100F;
			generator.setVeinDensity(density);

			String[] biomeModTags = config.get("Config for " +oreGenString, "7 Percent ore generation in biome type", preset.biomeTags).getString().replaceAll("\\s","").toLowerCase().split(",");

			generator.biomeModifier = new HashMap<BiomeDictionary.Type, Float>();

			if (biomeModTags[0] != "" && biomeModTags.length > 0){
				for (String biomestring : biomeModTags){
					if (biomestring.length() > 0){
						float f = Integer.parseInt(biomestring.split("@")[1])/100F;
						generator.biomeModifier.put(getBiomeTypeFromString(biomestring.split("@")[0]), f);
					}
				}
			}

			String[] reqBiomes = config.get("Config for " +oreGenString, "8 Required Biome types", "").getString().replaceAll("\\s","").toLowerCase().split(",");
			if (reqBiomes[0] != "" && reqBiomes.length > 0){

				for (String biomestring : reqBiomes){
					if (biomestring.length() > 0){
						generator.reqBiomeTypes.add(getBiomeTypeFromString(biomestring));
					}
				}
			}



			for (IBlockState stone : stoneArray){
				if (denseBlock){

					Block block;
					Block stoneblock = stone.getBlock();
					int meta = stoneblock.getMetaFromState(stone);
					String stoneName = stoneblock.getRegistryName().toString().split(":")[1]+meta;

					String blockName = stoneName+ oreString.split(":")[1].split("@")[0] + Integer.parseInt(oreString.split(":")[1].split("@")[1]);

					//if (Block.getBlockFromName(dense_"+blockName) == null){
					if (oreState.getBlock() != Blocks.REDSTONE_ORE){
						if (stone.getBlock() instanceof BlockFalling){
							block = WTFBlocks.registerBlock(new BlockDenseOreFalling(stone, oreState), "dense_"+blockName);
							BlockstateWriter.writeDenseOreBlockstate(stone, "dense_"+blockName, textureLoc, stoneName);
						}
						else {
							block = WTFBlocks.registerBlock(new BlockDenseOre(stone, oreState), "dense_"+blockName);
							BlockstateWriter.writeDenseOreBlockstate(stone, "dense_"+blockName, textureLoc, stoneName);
						}
					}
					else {

						block = WTFBlocks.registerBlock(new DenseRedstoneOre(false), "dense_"+blockName);
						BlockstateWriter.writeDenseOreBlockstate(stone, "dense_"+blockName, textureLoc, stoneName);
						DenseRedstoneOre.denseRedstone_off = block;
						DenseRedstoneOre.denseRedstone_on = WTFBlocks.registerBlock(new DenseRedstoneOre(true), "dense_"+blockName+"_on");
						BlockstateWriter.writeDenseOreBlockstate(stone, "dense_"+blockName+"_on", textureLoc, stoneName);
					}



					BlockSets.stoneAndOre.put(new StoneAndOre(stone, oreState), block.getDefaultState());
					//Ore ubification/densification of existing ores- removed because it was crashing
					//new OreReplacer(blockstate.getBlock());
				}
				else {
					BlockSets.stoneAndOre.put(new StoneAndOre(stone, oreState), oreState);
					new OreReplacer(oreState.getBlock());
				}
			}

			OreGenerator.oreGenRegister.add(generator);
			VanillOreGenCatcher.vanillaCanceler(oreState);
			Core.coreLog.info("Ore Generator Added for " + oreGenString);
		}



		config.save();
	}

	
	public static OreGenAbstract getGenerator (String oreString, OreDefReg preset, Configuration config, String genString, IBlockState oreState, int[] genRange, int[] orePerChunk, boolean denseBlock) throws Exception{

		//Parse the gen type string
		String[] genTypeArray = genString.split("&");
		GENTYPE gentype;

		try {
			gentype = GENTYPE.valueOf(genTypeArray[0].toLowerCase());
		}
		catch (IllegalArgumentException e){
			GENTYPE[] types = GENTYPE.values();
			String gentypestring = "";
			for (GENTYPE type : types){
				gentypestring += type.toString() + ", " ;
			}
			throw new Exception("Ore Config Parsing Exception while trying to parse config for " + oreString  + " ***** "  + genString +" is not a recognised generation type.  Accepted generation types are: " + gentypestring );
		}

		OreGenAbstract secondaryGen = null;

		//Handle the gen type string in a switch
		switch (gentype){
		case cave:
			//Get the surfaces for gen
			ArrayList<OreGenCaveFloor.surface> surfacelist = new ArrayList<OreGenCaveFloor.surface>();
			String[] surfacestrings = config.get("Config for " +oreString, "Surfaces in which to generates: floor, ceiling, wall", preset.surfaces).getString().split("&");
			for (String string : surfacestrings){
				try {
					surfacelist.add(OreGenCaveFloor.surface.valueOf(string.toLowerCase()));
				}
				catch (IllegalArgumentException e){
					OreGenCaveFloor.surface[] types = OreGenCaveFloor.surface.values();
					String gentypestring = "";
					for (OreGenCaveFloor.surface type : types){
						gentypestring += type.toString() + ", " ;
					}
					throw new Exception("Ore Config Parsing Exception while trying to parse  : " + oreString + " ***** "  + string+" is not a recognised surface type.  Accepted surface types are: " + gentypestring );
				}
			}

			//Get the secondary generation type
			secondaryGen =getGenerator(oreString, preset, config, genTypeArray[1], oreState, genRange, orePerChunk, denseBlock); 
			return new OreGenCaveFloor(secondaryGen, oreState, genRange, orePerChunk, denseBlock, surfacelist);

		case cloud:

			int cloudDiameter = config.get("Config for " +oreString, "Cloud diameter", preset.cloudDiameter).getInt();
			return new OreGenCloud(oreState, genRange, orePerChunk, denseBlock, cloudDiameter);

		case cluster:
			return new OreGenCluster(oreState, genRange, orePerChunk, denseBlock);

		case single:
			return new OreGenSingle(oreState, genRange, orePerChunk, denseBlock);

		case underwater:
			secondaryGen =getGenerator("Config for " +oreString, preset, config, genTypeArray[1], oreState, genRange, orePerChunk, denseBlock); 
			return new OreGenUnderWater(secondaryGen, oreState, genRange, orePerChunk, denseBlock);

		case vanilla:
			int blocksPerCluster = config.get("Config for " +oreString, "Blocks per cluster", preset.vanillaBlocksPerCluster).getInt();
			return new OreGenVanilla(oreState, genRange, orePerChunk, denseBlock, blocksPerCluster);

		case vein:
			int[] dimensions = config.get("Config for " +oreString, "Vein dimensions (length,width,vertical thickness)", preset.veinDimensions).getIntList();
			float pitch = (float)config.get("Config for " +oreString, "Average vein pitch (o for horizontal, 1.5 for vertical)", preset.veinPitch).getDouble();
			return new OreGenVein(oreState, genRange, orePerChunk, dimensions, pitch, denseBlock);

		}
		return null;
	}

	


}
