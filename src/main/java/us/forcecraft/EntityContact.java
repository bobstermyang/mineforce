package us.forcecraft;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowGolem;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAIVillagerMate;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntityContact extends EntityVillager {
	public static Map<String, EntityContact> contactMap = new HashMap<String, EntityContact>();
	private static Random rand = new Random();
	private static final Class[] unwantedAI = { 
		EntityAIVillagerMate.class,
		EntityAIFollowGolem.class
	};
	String id;
	long oppyCloseTime = 0;

	public EntityContact(World par1World) {
		this(par1World, null);
	}

	public EntityContact(World par1World, String id) {
		super(par1World, rand.nextInt(6)); // 6 villager professions
		
		overrideAI();
		
		this.tasks.addTask(0, new EntityAIWaitOnOppyClose(this));
		
		this.id = id;
		addToContactMap();
	}
	
	private EntityAIBase findTask(Class taskClass) {
		for (Object o : tasks.taskEntries) {
			EntityAITasks.EntityAITaskEntry taskEntry = (EntityAITasks.EntityAITaskEntry)o;
			if (taskEntry.action.getClass().equals(taskClass)) {
				return taskEntry.action;
			}
		}
		
		return null;
	}
	
	private void overrideAI() {
		for (Class taskClass : unwantedAI) {
			EntityAIBase task = findTask(taskClass);
			if (task != null) {
				tasks.removeTask(task);
			}			
		}
	}

	private void addToContactMap() {
		contactMap.put(id, this);		
	}

	private void removeFromContactMap() {
		contactMap.remove(id);	
	}

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeEntityToNBT(par1NBTTagCompound);
        par1NBTTagCompound.setString("Id", this.id);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readEntityFromNBT(par1NBTTagCompound);
        this.id = par1NBTTagCompound.getString("Id");
        addToContactMap();
    }
    
	private static Item[] treasureItems = {
		Items.coal,		      // 1
		Items.iron_ingot,     // 10 
		Items.gold_ingot,     // 100
		Items.redstone,       // 1000
		Items.glowstone_dust, // 10000
		Items.emerald,        // 100000
		Items.diamond         // 1000000
	};
	
	public static List<Object[]> getTreasure(double amount) {
		int a = (int)amount, n;
		List<Object[]> l = new ArrayList<Object[]>();
		
		for (int i = 0; (i < (treasureItems.length - 1) && a > 0); i++) {
			n = a % 10;
			if (n > 0) {
				l.add(new Object[]{treasureItems[i], n});
			}
			a /= 10;
		}
		
		if (a > 0) {
			l.add(new Object[]{treasureItems[treasureItems.length - 1], a});
		}
		
		return l;
	}
	
    /**
     * Called when a player interacts with a mob. e.g. gets milk from a cow, gets into the saddle on a pig.
     */
    public boolean interact(EntityPlayer par1EntityPlayer)
    {
        if (this.isEntityAlive() && !par1EntityPlayer.isSneaking())
        {
            if (!this.worldObj.isRemote)
            {
            	GuiChatter.displayChatterGUI((EntityPlayerMP)par1EntityPlayer, id, getCustomNameTag());
            }

            return true;
        }
        else
        {
        	return false;
        }
    }

	public long getOppyCloseTime() {
		return this.oppyCloseTime;
	}
	
	public void setOppyCloseTime(long time) {
		this.oppyCloseTime = time;
	}
	
	@Override
    public void setDead()
    {
        if (!this.worldObj.isRemote)
        {
        	removeFromContactMap();
        	try {
        		// NOTE - this can fail if the Contact is associated with Cases etc
        		// TODO: Need to figure out how to handle failure - reanimate Contact? 
        		Forcecraft.instance.client.delete("Contact", id);
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
    	
        super.setDead();
    }
}
