package crazypants.enderio.machine.ranged;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.enderio.core.common.util.BlockCoord;

public class RangeEntity extends Entity {

  int totalLife = 20;
  int lifeSpan = totalLife;
  float range;  
  private IRanged spawnGuard;

  public RangeEntity(IRanged sg) {
    super(sg.getRangeWorldObj());
    spawnGuard = sg;
    BlockCoord bc = spawnGuard.getLocation();
    setPosition(bc.x + 0.5, bc.y + 0.5, bc.z + 0.5);
    ignoreFrustumCheck = true;
    range = sg.getRange() + 0.5f;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public boolean isInRangeToRender3d(double p_145770_1_, double p_145770_3_, double p_145770_5_) {
    return true;
  }

  @Override
  protected void entityInit() {
  }

  @Override
  protected boolean canTriggerWalking() {    
    return false;
  }

  private final AxisAlignedBB noBB = new AxisAlignedBB(0, -1, 0, 0, -1, 0);

  @Override
  public AxisAlignedBB getEntityBoundingBox() {
    return noBB;
  }

  @Override
  public void onUpdate() {
    super.onUpdate();
    lifeSpan--;
    BlockCoord bc = spawnGuard.getLocation();
    if(!(worldObj.getTileEntity(bc.getBlockPos()) instanceof IRanged)) {
      setDead();
    }
    if(!spawnGuard.isShowingRange()) {
      setDead();
    }
  }

  @Override
  protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {
  }

  @Override
  protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {
  }
}
