package warped.realms.system.update

import PutComponent
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import ktx.app.gdxError
import ktx.collections.map
import ktx.log.logger
import warped.realms.component.AnimationComponent
import warped.realms.component.AnimationComponent.Companion.NO_ANIMATION
import warped.realms.component.ImageComponent
import System
import Update
import generated.systems.Factories
import generated.systems.Systems
import warped.realms.system.Logger
import warped.realms.system.debug

@System
@Update(1)
@PutComponent(ImageComponent::class, AnimationComponent::class)
class AnimationSystem(
    val systems: Systems,
    val factories: Factories
) {
    private val textureAtlas: TextureAtlas by lazy { RenderSystem.textureAtlas }

    private val animCmps: MutableMap<AnimationComponent, ImageComponent> = mutableMapOf()
    private val cachedAnimations = mutableMapOf<String, Animation<TextureRegionDrawable>>()
    fun Update(deltaTime: Float) {
//        val x = this.javaClass.getAnnotation(Update::class.java)?.priority
//        println("[UPDATE] ${this::class.simpleName} $x")
        animCmps.forEach { animationCmp, imageCmp ->
            if (animationCmp.nextAnimation == NO_ANIMATION) {
                animationCmp.stateTime += deltaTime
            } else {
                animationCmp.animation = animation(animationCmp.nextAnimation)
                animationCmp.stateTime = 0f
                animationCmp.nextAnimation = NO_ANIMATION
            }
            animationCmp.animation.playMode = animationCmp.playMode
            imageCmp.image.drawable = animationCmp.animation.getKeyFrame(animationCmp.stateTime)
        }
    }

    fun PutComponent(imCmp: ImageComponent, animCmp: AnimationComponent) {
        animCmps.put(animCmp, imCmp)
        println("[DEBUG] Put component ${imCmp::class.simpleName} in ${this::class.simpleName}")
    }
    fun DeleteComponent(component: ImageComponent) {
        println("[DEBUG] Delete component ${component::class.simpleName} in ${this::class.simpleName}")
    }
    fun DeleteComponent(component: AnimationComponent) {
        println("[DEBUG] Delete component ${component::class.simpleName} in ${this::class.simpleName}")
    }
    private fun animation(aniKeyPath: String): Animation<TextureRegionDrawable>{
        return cachedAnimations.getOrPut(aniKeyPath){
            Logger.debug { "New animation is created for '$aniKeyPath'" }
            val regions = textureAtlas.findRegions(aniKeyPath)
            if(regions.isEmpty){
                gdxError("There are no texture regions for $aniKeyPath")
            }
            Animation<TextureRegionDrawable>(
                DEFAULT_FRAME_DURATION,
                regions.map { TextureRegionDrawable(it) }
            )
        }
    }
    fun Dispose() {
        println("[UPDATE] ${this::class.simpleName}")
    }
    companion object{
        private const val DEFAULT_FRAME_DURATION = 1/8f
    }
}
