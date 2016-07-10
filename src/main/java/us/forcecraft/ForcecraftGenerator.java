package us.forcecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.logging.log4j.Level;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import argo.jdom.JsonNode;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLEventChannel;

public class ForcecraftGenerator implements IWorldGenerator {
	interface IBlockReceiver {
		public void setBlock(int x, int y, int z, Block block, int metadata, int flags);

		public void setStageEntity(int x, int y, int z, String stringValue,
				String stringValue2);

		public void setSignEntity(int x, int y, int z, String[] splitIntoLines);

		public void setChatterSignEntity(int x, int y, int z,
				String[] splitIntoLines, String id, String name);
	}

	static class DefaultBlockReceiver implements IBlockReceiver {
		World world;

		public DefaultBlockReceiver(World world) {
			this.world = world;
		}

		@Override
		public void setBlock(int x, int y, int z, Block block, int metadata, int flags) {
			world.setBlock(x, y, z, block, metadata, flags);
		}

		@Override
		public void setStageEntity(int x, int y, int z, String oppyId, String stage) {
			TileEntityStageBlock tileentitystageblock = (TileEntityStageBlock)world.getTileEntity(x, y, z);
			tileentitystageblock.setOpportunityStage(oppyId, stage);
		}

		@Override
		public void setSignEntity(int x, int y, int z, String[] text) {
			TileEntitySign tileentitysign = (TileEntitySign)world.getTileEntity(x, y, z);
			tileentitysign.signText = text;
			tileentitysign.markDirty();
			world.markBlockForUpdate(x, y, z);
		}

		@Override
		public void setChatterSignEntity(int x, int y, int z,
				String[] text, String id, String name) {
			TileEntityChatterSign tileentitychattersign = (TileEntityChatterSign)world.getTileEntity(x, y, z);
			tileentitychattersign.signText = text;
			tileentitychattersign.accountId = id;
			tileentitychattersign.accountName = name;
			tileentitychattersign.markDirty();
			world.markBlockForUpdate(x, y, z);
		}
	}

	static class BlockCollector implements IBlockReceiver {
		class Record {			
			int x;
			int y;
			int z;

			public Record(int x, int y, int z){
				this.x = x;
				this.y = y;
				this.z = z;				
			}
		}

		class BlockRecord extends Record {
			Block block;
			int metadata;
			int flags;

			public BlockRecord(int x, int y, int z, Block block, int metadata, int flags) {
				super(x, y, z);
				this.block = block;
				this.metadata = metadata;
				this.flags = flags;
			}

			public void setBlock() {
				world.setBlock(x, y, z, block, metadata, flags);
			}
		}

		class StageRecord extends Record {
			String oppyId;
			String stage;

			public StageRecord(int x, int y, int z, String oppyId, String stage) {
				super(x, y, z);
				this.oppyId = oppyId;
				this.stage = stage;
			}

			public void setStage() {
				TileEntityStageBlock tileentitystageblock = (TileEntityStageBlock)world.getTileEntity(x, y, z);
				tileentitystageblock.setOpportunityStage(oppyId, stage);
			}
		}

		class SignRecord extends Record {
			String[] text;

			public SignRecord(int x, int y, int z, String[] text) {
				super(x, y, z);
				this.text = text;
			}

			public void setSign() {
				TileEntitySign tileentitysign = (TileEntitySign)world.getTileEntity(x, y, z);
				tileentitysign.signText = text;
				tileentitysign.markDirty();
				world.markBlockForUpdate(x, y, z);
			}
		}

		class ChatterSignRecord extends SignRecord {
			String accountId;
			String accountName;

			public ChatterSignRecord(int x, int y, int z, String[] text, String accountId, String accountName) {
				super(x, y, z, text);
				this.accountId = accountId;
				this.accountName = accountName;
			}

			public void setSign() {
				TileEntityChatterSign tileentitychattersign = (TileEntityChatterSign)world.getTileEntity(x, y, z);
				tileentitychattersign.signText = text;
				tileentitychattersign.accountId = accountId;
				tileentitychattersign.accountName = accountName;
				tileentitychattersign.markDirty();
				world.markBlockForUpdate(x, y, z);
			}
		}

		World world;
		public List<BlockRecord> blocks;
		public List<StageRecord> stages;
		public List<SignRecord> signs;

		public BlockCollector(World world) {
			this.world = world;
			this.blocks = new ArrayList<BlockRecord>();
			this.stages = new ArrayList<StageRecord>();
			this.signs = new ArrayList<SignRecord>();
		}

		@Override
		public void setBlock(int x, int y, int z, Block block, int metadata, int flags) {
			blocks.add(new BlockRecord(x, y, z, block, metadata, flags));
		}

		@Override
		public void setStageEntity(int x, int y, int z, String oppyId, String stage) {
			stages.add(new StageRecord(x, y, z, oppyId, stage));
		}

		@Override
		public void setSignEntity(int x, int y, int z, String[] text) {
			signs.add(new SignRecord(x, y, z, text));
		}

		public void shuffle() {
			Random rnd = new Random();
			for (int i = blocks.size() - 1; i > 0; i--)
			{
				int index = rnd.nextInt(i + 1);
				// Simple swap
				BlockRecord a = blocks.get(index);
				blocks.set(index, blocks.get(i));
				blocks.set(i, a);
			}			
		}

		public void replayBlocks(int n) {
			n = Math.min(n, blocks.size());
			for (int i = 0; i < n; i++) {
				blocks.remove(0).setBlock();
			}
		}

		public void replayEntities() {
			for (int i = 0; i < signs.size(); i++) {
				signs.get(i).setSign();
			}
			signs.clear();
			for (int i = 0; i < stages.size(); i++) {
				stages.get(i).setStage();
			}
			stages.clear();
		}

		@Override
		public void setChatterSignEntity(int x, int y, int z,
				String[] text, String id, String name) {
			signs.add(new ChatterSignRecord(x, y, z, text, id, name));
		}		
	}

	private static final int STORY_HEIGHT = 5;

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world,
			IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
		if (world.provider.dimensionId == Forcecraft.dimensionId) {
			generateSurface(world, random, chunkX, chunkZ);
		}
	}

	private void generateSurface(World world, Random rand, int chunkX, int chunkZ) {    	
		List<JsonNode> records = Forcecraft.instance.accounts.getNode("records").getElements();

		generateBuilding(world, rand, records, chunkX, chunkZ);
	}

	// How far round a discrete spiral is the given co-ordinate?
	// Worked backwards from http://stackoverflow.com/a/19287714/33905 to get this!
	private static int getNDiscreteSpiral(int x, int y) {
		// Radius
		int r = Math.max(Math.abs(x), Math.abs(y));

		// How many points inside this loop
		int p = (8 * r * (r - 1)) / 2;

		// How many points on each side of the loop
		int en = r * 2;

		// How far round the loop are we?
		int a;

		if (x == -1 * r) { // left 
			a = (en * 4) - (r + y);
		} else if (y == r) { // top
			a = (en * 3) - (r + x);
		} else if (x == r) { // right
			a = (en * 2) - (r - y);
		} else { // bottom
			a = en - (r - x);
		}

		return a + p;
	}

	// What is the co-ordinate of the nth point on a discrete spiral?
	// From http://stackoverflow.com/a/19287714/33905
	public static int[] getPointDiscreteSpiral(int n) {
		int[] pos = new int[2];

		if (n == 0) {
			pos[0] = 0;
			pos[1] = 0;
			return pos;
		}

		n--;

		int r = (int)(Math.floor((Math.sqrt(n + 1) - 1.0) / 2.0) + 1.0);

		int p = (8 * r * (r - 1)) / 2;

		int en = r * 2;

		int a = (1 + n - p) % (r * 8);

		switch ((int)Math.floor(a / (r * 2))) {
		// find the face : 0 top, 1 right, 2, bottom, 3 left
		case 0:
		{
			pos[0] = a - r;
			pos[1] = -r;
		}
		break;
		case 1:
		{
			pos[0] = r;
			pos[1] = (a % en) - r;

		}
		break;
		case 2:
		{
			pos[0] = r - (a % en);
			pos[1] = r;
		}
		break;
		case 3:
		{
			pos[0] = -r;
			pos[1] = r - (a % en);
		}
		break;
		}

		return pos;
	}


	private void generateBuilding(World world, Random rand, List<JsonNode> records, int chunkX,
			int chunkZ) {
		int n = getNDiscreteSpiral(chunkX, chunkZ);
		if (n < records.size()) {
			JsonNode acct = records.get(n);

			if (!acct.getBooleanValue("IsDeleted")) {
				FMLLog.log(Forcecraft.FORCECRAFT, Level.INFO, "Generating building for %s at (%d, %d)", acct.getStringValue("Name"), chunkX, chunkZ);
				List<JsonNode> oppys = null;
				int height = 1;
				int maxHeight = (255 - Forcecraft.groundLevel) / STORY_HEIGHT; // Can't build above y = 255
				try {
					oppys = acct.getNode("Opportunities", "records").getElements();
					height = Math.max(1, oppys.size());
					height = Math.min(height, maxHeight);
				} catch (IllegalArgumentException iae) {
					// No data
				}

				DefaultBlockReceiver receiver = new DefaultBlockReceiver(world);
				for (int l = 0; l < height; l++) {
					generateLevel(chunkX, chunkZ, l, acct, 
							((oppys != null) && (l < oppys.size())) ? oppys.get(l) : null, 
									Forcecraft.instance.stages, receiver);
				}
				generateRoof(chunkX, chunkZ, height, receiver);
				generateContacts(world, n, chunkX, chunkZ, height, records);
			}	    	
		}
	}

	BlockCollector collector = null;

	@SubscribeEvent
	public void onTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			FMLLog.log(Forcecraft.FORCECRAFT, Level.INFO, "Server tick start");
			if (collector.blocks.size() > 0) {
				FMLLog.log(Forcecraft.FORCECRAFT, Level.INFO, "replayBlocks");
				collector.replayBlocks(20);
			} else {
				FMLLog.log(Forcecraft.FORCECRAFT, Level.INFO, "replayEntities");
				collector.replayEntities();
				FMLCommonHandler.instance().bus().unregister(this);
				collector = null;
			}				
		}
	}

	public void addNewLevel(World world, JsonNode acct, JsonNode oppy, int index, int chunkX, int chunkZ) {
		collector = new BlockCollector(world);
		generateLevel(chunkX, chunkZ, index, acct, oppy, Forcecraft.instance.stages, collector);
		generateRoof(chunkX, chunkZ, index + 1, collector);
		collector.shuffle();
		FMLCommonHandler.instance().bus().register(this);
	}

	private void generateContacts(World world, int n, int chunkX, int chunkZ, int height, List<JsonNode> records) {
		// x 9, height, z 10
		try {
			List<JsonNode> contacts = records.get(n).getNode("Contacts", "records").getElements();
			int nContacts = Math.min(contacts.size(), (7 * height * 8));
			for (int i = 0; i < contacts.size(); i++) {
				JsonNode contact = contacts.get(i);
				String id = contact.getStringValue("Id");
				EntityContact entityContact = new EntityContact(world, id);
				double x = (double)((chunkX * 16) + ((i / STORY_HEIGHT) % 7) + 6) + 0.5D, 
						y = (double)Forcecraft.groundLevel + ((i % height) * STORY_HEIGHT) + 1.0D, 
						z = (double)((chunkZ * 16) + ((i / (STORY_HEIGHT * 7)) % 8) + 6) + 0.5D;
				entityContact.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
				entityContact.setCustomNameTag(contact.getStringValue("Name"));
				FMLLog.log(Forcecraft.FORCECRAFT, Level.INFO, "Spawning contact %s at (%f, %f, %f)", entityContact.getCustomNameTag(), x, y, z);
				world.spawnEntityInWorld(entityContact);
			}
		} catch (IllegalArgumentException iae) {
			// No data
		}
	}

	private void generateLevel(int chunkX, int chunkZ, int l,
			JsonNode acct, JsonNode oppy, List<JsonNode> stages, IBlockReceiver receiver) {
		// i, j, k are world co-ordinates of lower front right corner of floor
		int i = (chunkX * 16) + 4,
				j = Forcecraft.groundLevel + (l*STORY_HEIGHT),
				k = (chunkZ * 16) + 4,
				p, q, r;
		double amount;
		try {
			amount = Double.valueOf(oppy.getNumberValue("Amount"));
		} catch (Exception e) {
			amount = 0.0;
		}

		Block wallBlock = (amount >= 100000) ? Blocks.stonebrick: Blocks.stone;
		int nstages = stages.size();
		int leverstart = (10 - nstages) / 2; // Center levers on wall - ASSUMPTION - 10 or fewer stages!
		int leverend = leverstart + nstages + 1;

		// x, y, x are relative to lower front right corner of floor
		for (int x = 0; x < 12; x++) {
			for (int y = 0; y < STORY_HEIGHT; y++) {
				for (int z = 0; z < 12; z++) {
					int metadata = 0;

					p = i+x; q = j+y; r = k+z;

					int stage = leverend - z - 1;

					if (x == 0 || x == 11 || z == 0 || z == 11) { // Walls
						if ((y == 2 || y == 3) && // Window height
								((x == 2 || x == 3 || x == 8 || x == 9) || // side windows 
										((l > 0 || z == 11 || y == 3) && (x == 5 || x == 6)))) { // front/back windows
							receiver.setBlock(p, q, r, Blocks.glass, 0, 2);
						} else if ((l == 0 && z == 0) && (x == 5 || x == 6) && (y == 1 || y == 2)) {
							if (y == 2) {
								// top half
								metadata = 0x8; 
								if (x == 5) {
									metadata |= 0x1; // hinge on left, else hinge on right
								}
							} else {
								// bottom half
								metadata = 0x1; // face north when closed
							}
							receiver.setBlock(p, q, r, Blocks.wooden_door, metadata, 2);
						} else if (oppy != null && y==3 && x == 0 && z > leverstart && z < leverend) { // Stage Block
							String label = stages.get(stage).getStringValue("label");
							if (oppy.getStringValue("StageName").equals(label)) {
								metadata = 0x1; // Block is 'on'
							}
							receiver.setBlock(p, q, r, Forcecraft.stageBlock, metadata, 2);
							receiver.setStageEntity(p, q, r, oppy.getStringValue("Id"), label);
						} else {
							receiver.setBlock(p, q, r, wallBlock, 0, 2);
						}
					} else if (y == 0 && (x < 10 || (z < 4 || z > 8))) { // ceiling
						Block block = Blocks.quartz_block;
						if ((x == 2 || x == 4 || x == 6 || x == 8) && z > 1 && z < 8) {
							block = Blocks.glowstone; // 'lights'
						}
						receiver.setBlock(p, q, r, block, 0, 2);
					} else if (x == 10 && ((y > 0 && z == (y + 3)) || (l > 0 && y == 0 && z == 8))) { // stairs
						receiver.setBlock(p, q, r, Blocks.oak_stairs, 0x2, 2);
					} else if (y == 1 && (l == 0 || x < 10 || (z < 4 || z > 8))) { // floor
						if ((x + z) % 2 == 0) {
							metadata = 11; // blue carpet, otherwise white carpet
						}
						receiver.setBlock(p, q, r, Blocks.carpet, metadata, 2);
					} else if (oppy != null && y==2 && x == 1 && z > leverstart && z < leverend) { // Stage sign
						receiver.setBlock(p, q, r, Blocks.wall_sign, 5 /* Face east */, 2);
						receiver.setSignEntity(p, q, r, splitIntoLines(stages.get(stage).getStringValue("label"), 15, 4));
					} else if (oppy != null && y==3 && x == 1 && z > leverstart && z < leverend) { // Stage lever
						metadata = 0x1; // Face east
						if (oppy.getStringValue("StageName").equals(stages.get(stage).getStringValue("label"))) {
							metadata |= 0x8; // Lever is 'on'
						}
						receiver.setBlock(p, q, r, Blocks.lever, metadata, 2);
					}
				}
			}
		}

		// Opportunity Name
		if (oppy != null) {
			p = i+1; q = j+4; r = k+5;
			receiver.setBlock(p, q, r, Forcecraft.chatterSignBlock, 5 /* Face east */, 2);
			String name = oppy.getStringValue("Name");
			receiver.setChatterSignEntity(p, q, r, splitIntoLines(name, 15, 4), oppy.getStringValue("Id"), name);
		}

		if (l == 0) {
			// Account Name
			p = i+4; q = j+2; r = k-1;
			receiver.setBlock(p, q, r, Forcecraft.chatterSignBlock, 2 /* Face north */, 2);
			String name = acct.getStringValue("Name");
			receiver.setChatterSignEntity(p, q, r, splitIntoLines(name, 15, 4), acct.getStringValue("Id"), name);
		}
	}

	private void generateRoof(int chunkX, int chunkZ, int height, IBlockReceiver receiver) {
		// i, j, k are world co-ordinates of lower front right corner of floor
		int i = (chunkX * 16) + 4,
				j = Forcecraft.groundLevel + (height*STORY_HEIGHT),
				k = (chunkZ * 16) + 4;

		// x, y, x are relative to lower front right corner of floor
		for (int x = 0; x < 12; x++) {
			for (int y = 0; y < 2; y++) {
				for (int z = 0; z < 12; z++) {
					int p = i+x, q = j+y, r = k+z;

					if (x == 0 || x == 11 || z == 0 || z == 11) { // Walls
						receiver.setBlock(p, q, r, Blocks.stone, 0, 2);
					} else if (y == 0 && (x < 10 || (z < 4 || z > 8))) { // ceiling
						Block block = Blocks.quartz_block;
						if ((x == 3 || x == 8) && z > 1 && z < 10) {
							block = Blocks.glowstone; // 'lights'
						}
						receiver.setBlock(p, q, r, block, 0, 2);
					} else if (y == 0 && z == 8) { // stairs
						receiver.setBlock(p, q, r, Blocks.oak_stairs, 0x2, 2);
					} else if (y == 1 && (x < 10 || (z < 4 || z > 8))) { // floor
						receiver.setBlock(p, q, r, Blocks.stone_slab, 0, 2);
					}
				}
			}
		}
	}

	public static String[] splitIntoLines(String input, int maxCharInLine, int maxLines){
		StringTokenizer tok = new StringTokenizer(input, " ");
		StringBuilder output = new StringBuilder(input.length());
		int lineLen = 0;
		String[] lines = new String[maxLines];
		int line = 0;

		while (tok.hasMoreTokens()) {
			String word = tok.nextToken();

			while(word.length() > maxCharInLine){
				output.append(word.substring(0, maxCharInLine-lineLen));
				lines[line] = output.toString();
				line++;
				if (line == maxLines) {
					return lines;
				}
				output.setLength(0);
				word = word.substring(maxCharInLine-lineLen);
				lineLen = 0;
			}

			if (lineLen + 1 + word.length() > maxCharInLine) {
				lines[line] = output.toString();
				line++;
				if (line == maxLines) {
					return lines;
				}
				output.setLength(0);
				lineLen = 0;
			}

			if (lineLen > 0) {
				output.append(" ");
				lineLen++;
			}

			output.append(word);
			lineLen += word.length();
		}
		lines[line] = output.toString();
		line++;

		for (int n = line; n < lines.length; n++) {
			if (lines[n] == null) {
				lines[n] = "";
			}
		}

		return lines;
	}
}