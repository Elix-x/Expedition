package wtf.ores.oregenerators;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import wtf.ores.ChunkDividedOreMap;
import wtf.ores.OreGenAbstract;
import wtf.utilities.wrappers.ChunkCoords;
import wtf.utilities.wrappers.ChunkScan;


public class OreGenCaveFloor extends OreGenAbstract{



	private final OreGenAbstract veinType;
	private final List<surface> surfaceList;
	
	public enum surface{
		floor, wall, ceiling
	}
	
	public OreGenCaveFloor(OreGenAbstract vein, IBlockState blockstate, int[] genRange, int[] minmaxPerChunk, boolean denseGen, ArrayList<surface> list) {
		super(blockstate, genRange, minmaxPerChunk, denseGen);
		veinType = vein;
		surfaceList = list;
	}


	
	@Override
	public void doOreGen(World world, ChunkDividedOreMap map, Random random, ChunkCoords coords, ChunkScan chunkscan) throws Exception {
		
		if (chunkscan.caveset.size() > 0){
			
			int blocksPerChunk = this.getBlocksPerChunk(world, coords, random, chunkscan.surfaceAvg);
		
			//I will need to access the size of the vein to do partial generation chances like I am for the other veins- which means I would need to seperate out the veinsize method, and throw it in
			//the abstract
			//System.out.println("gen " + numToGenerate);
			int blockReqs = this.blocksReq();
			while (blocksPerChunk > blockReqs || (blocksPerChunk > 0 && random.nextInt(blockReqs) < blocksPerChunk)){
				surface gen = surfaceList.get(random.nextInt(surfaceList.size()));
				BlockPos pos = null;
				switch (gen){
				case ceiling:
					//System.out.println("ceiling");
					pos = chunkscan.caveset.get(random.nextInt(chunkscan.caveset.size())).getRandomPosition(random).getCeilingPos();
					break;
				case floor:
					//System.out.println("floor");
					pos = chunkscan.caveset.get(random.nextInt(chunkscan.caveset.size())).getRandomPosition(random).getFloorPos();
					break;
				case wall:
					//System.out.println("wall");
					ArrayList<BlockPos> wall = chunkscan.caveset.get(random.nextInt(chunkscan.caveset.size())).wall;
					if (wall.size() > 0){
						
						pos = wall.get(random.nextInt(wall.size()));
					}
					break;
				default:
					break;
				
				}

				if (pos != null){
					blocksPerChunk-=veinType.genVein(world, map, random, chunkscan, pos);
				}
				
			}
			
		}
		
	}

	@Override
	public int genVein(World world, ChunkDividedOreMap map, Random random, ChunkScan scan, BlockPos pos)
			throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public int blocksReq() {
		return veinType.blocksReq();
	}


}
