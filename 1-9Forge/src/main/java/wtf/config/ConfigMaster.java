package wtf.config;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import wtf.utilities.UBCCompat;

public class ConfigMaster {

	protected final static String configPath = "config/WTF-Expedition/";
	
	
	public static BiomeDictionary.Type getBiomeTypeFromString(String biomestring) throws Exception{
		try {
			return BiomeDictionary.Type.valueOf(biomestring.toUpperCase());
		} catch (IllegalArgumentException e){
			Type[] types = BiomeDictionary.Type.values();
			String string = "";
			for (Type type : types){
				string += type.toString() + ", " ;
			}
			throw new Exception("Ore Config Parsing Exception while trying to parse Biome Percent modifier : --" + string + "-- "  + "Unrecognised Forge BiomeDictionary BiomeType, the available biome types are : " + string);
		}
	}
	
	public static Block getBlockFromString(String string){
		
			Block block = Block.getBlockFromName(string);
			if (block == null){
				try {
					throw new Exception ("Unable to find block for " + string);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return block;
	}
	
	public static IBlockState getBlockState(String string){
		if (string == null || !string.contains("@")){
			return null;
		}
		String[] stringArray = string.split("@");
		Block block = Block.getBlockFromName(stringArray[0]);

		if (block == null){
			try {
				throw new Exception("Unable to find block for " + stringArray[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (stringArray.length < 1){
			try {
				throw new Exception("Unable to find metadata argument for :" + string +".");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		IBlockState state = block.getStateFromMeta(Integer.parseInt(stringArray[1]));
		if (state == null){
			try {
				throw new Exception("Invalid blockstate for block and meta " + string);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return state; 
	}

	public static ArrayList<IBlockState> getBlockStateArray(String string){
		ArrayList<IBlockState> list = new ArrayList<IBlockState>();
		String[] stringArray = string.split(",");
		for (String substring :stringArray){

			if (substring.contains("igneous")){
				list.addAll(Arrays.asList(UBCCompat.IgneousStone));
			}
			else if (substring.contains("metamorphic")){
				list.addAll(Arrays.asList(UBCCompat.MetamorphicStone));
			}
			else if (substring.contains("sedimentary")){
				list.addAll(Arrays.asList(UBCCompat.SedimentaryStone));
			}
			else {
				list.add(getBlockState(substring));
			}
		}
		return list;
	}

}
