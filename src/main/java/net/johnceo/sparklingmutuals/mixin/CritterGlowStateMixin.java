package net.johnceo.sparklingmutuals.mixin;

import net.johnceo.sparklingmutuals.safari.esp.CritterGlowState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class CritterGlowStateMixin implements CritterGlowState {
    @Unique private boolean sm$critterGlow;
    public boolean sm$isCritterGlow() { return sm$critterGlow; }
    public void sm$setCritterGlow(boolean value) { sm$critterGlow = value; }
}
