package com.nsicyber.blockviewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.richpath.RichPath
import com.richpath.RichPathView
import com.richpathanimator.RichPathAnimator


/**
 * This view shows an Iran map svg that able to animate and interact with user touches.
 * @author Mohammad Rezaei
 * @see <a href="https://github.com/MohammadRezaei92">Github</a>
 */
class BlockView @JvmOverloads constructor(var blockList:ArrayList<String>,var svgString: String,
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * Default background color of provinces
     */
    var defaultBackgroundColor = Color.parseColor("#F2F3F7")
    /**
     * Background color of provinces when they are active
     */
    var provinceActiveColor = Color.CYAN
    /**
     * Stroke color of provinces
     */
    var provinceStrokeColor = Color.WHITE
    /**
     * Make provinces clickable
     */
    var provinceSelectByClick = true
        set(value) {
            field = value
            if (value)
                path.setOnPathClickListener {
                    activeProvince(blockList,it, withAnimate = true)
                } else {
                path.setOnPathClickListener(null)
            }
        }
    /**
     * If true multi province can be selected
     * If false on province can be selected
     */
    var provinceCanMultiSelect = false
    /**
     * Animation duration for map animations and provinces animations
     */
    var mapAnimationDuration = 200L
    /**
     * If true map's provinces appear with an animation in first time
     */
    var mapAppearWithAnimation = false
    var mapAdjustViewBound = false
    private val paint = Paint()
    private lateinit var surfaceView: SurfaceView
    private lateinit var path: RichPathView

    /**
     * Give list of selected provinces. see[Province]
     */
    var selectedBlock: MutableList<String> = mutableListOf()
    private val titles: MutableMap<RichPath?, String?> = mutableMapOf()

    init {
        addPathView(svgString)
        addSurfaceView()
        handleAttr(context, attrs)
    }

    private fun addPathView(svgString:String) {
        val svgBytes = svgString.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(svgBytes, 0, svgBytes.size)
        val bitmapDrawable = BitmapDrawable(resources, bitmap)


        //Show a preview of map in edit mode
        if(isInEditMode){
            val imageView = ImageView(context)
            imageView.layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT
                , ViewGroup.LayoutParams.MATCH_PARENT
            )


            imageView.setImageDrawable(bitmapDrawable)
            imageView.adjustViewBounds = mapAdjustViewBound
            addView(imageView)
            return
        }
        //Add rich path view for show map svg
        path = RichPathView(context)
        path.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT
            , ViewGroup.LayoutParams.MATCH_PARENT
        )
        path.adjustViewBounds = mapAdjustViewBound
        path.setImageDrawable(bitmapDrawable)
        addView(path)
    }

    private fun addSurfaceView() {
        //Add surface view for draw titles on map
        surfaceView = SurfaceView(context)
        surfaceView.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT
            , ViewGroup.LayoutParams.MATCH_PARENT
        )
        addView(surfaceView)
    }

    private fun handleAttr(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BlockView, 0, 0)

        try {
            defaultBackgroundColor =
                typedArray.getColor(R.styleable.BlockView_imProvinceBackgroundColor, Color.BLACK)
            provinceActiveColor = typedArray.getColor(
                R.styleable.BlockView_imProvinceActiveBackgroundColor,
                Color.CYAN
            )
            provinceStrokeColor =
                typedArray.getColor(R.styleable.BlockView_imProvinceStrokeColor, Color.WHITE)
            provinceSelectByClick =
                typedArray.getBoolean(R.styleable.BlockView_imProvinceSelectByClick, true)
            provinceCanMultiSelect =
                typedArray.getBoolean(R.styleable.BlockView_imProvinceMultiSelect, false)
            mapAnimationDuration =
                typedArray.getInt(R.styleable.BlockView_imAnimationDuration, 200).toLong()
            mapAppearWithAnimation =
                typedArray.getBoolean(R.styleable.BlockView_imMapAppearWithAnimation, false)
            mapAdjustViewBound = typedArray.getBoolean(R.styleable.BlockView_imAdjustViewBound, false)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            typedArray.recycle()
        }

    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        initMap()
    }

    private fun initMap() {
        //Apply default attrs to all provinces
        path.findAllRichPaths().forEach {
            it.fillColor = defaultBackgroundColor
            it.strokeColor = provinceStrokeColor
            //Ready provinces for play animation
            if (mapAppearWithAnimation) {
                it.scaleX = 5f
                it.scaleY = 5f
                it.fillAlpha = 0f
                it.strokeAlpha = 0f
            }
        }


    }




     fun activeProvince(blockList:ArrayList<String>,
        provincePath: RichPath?
        , withBackgroundColor: Int? = null
        , withStrokeColor: Int? = null
        , withAnimate: Boolean = false
    ) {

        //Deactivate selected provinces in single select mode
        if (!provinceCanMultiSelect)
            blockList.filter { it != provincePath?.name }.forEach {
                deActiveProvince(it)
            }

        provincePath?.let {
            //If province is active now, deactivate it.
            if (selectedBlock.contains(it.name)) {
                deActiveProvince(it.name, withAnimate)
            } else {//Activate province
                RichPathAnimator.animate(it)
                    .interpolator(AccelerateDecelerateInterpolator())
                    .duration(if (withAnimate) mapAnimationDuration else 0)
                    .scale(1.1f, 1f)
                    .fillColor(it.fillColor, withBackgroundColor ?: provinceActiveColor)
                    .strokeColor(it.strokeColor, withStrokeColor ?: provinceStrokeColor)
                    .start()
                selectedBlock.add(it.name)
            }
        }
    }

    private fun deActiveProvince(blockName: String?, withAnimate: Boolean = false) {

        val provincePath = path.findRichPathByName(blockName)



        provincePath?.let {
            RichPathAnimator.animate(it)
                .interpolator(AccelerateDecelerateInterpolator())
                .duration(if (withAnimate) mapAnimationDuration else 0)
                .scale(1.1f, 1f)
                .fillColor(defaultBackgroundColor)
                .strokeColor(provinceStrokeColor)
                .start()
            selectedBlock.remove(it.name)
        }
    }

    /**
     * Add a title to a province by given properties
     * @param [province] enum of iran provinces. see[Province]
     * @param [title] text you want to add to province
     * @param [typeface] type face of title
     * @param [textColor] text color of title
     */
    fun addTitle(
        blockName: String,
        title: String?,
        typeface: Typeface = Typeface.DEFAULT,
        textColor: Int? = null
    ) {
        if (title == null)
            return

        //Init paint for draw title
        paint.color = textColor ?: Color.BLACK
        paint.textSize = 30f
        paint.typeface = typeface

        val provincePath = path.findRichPathByName(blockName)
        //Add title to title list if not exist
        if (!titles.contains(provincePath))
            titles[provincePath] = title

        //Refresh view to draw new titles
        surfaceView.invalidate()

    }



    inner class SurfaceView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {
        init {
            //Force layout to call onDraw method
            setWillNotDraw(false)
        }

        @SuppressLint("DrawAllocation")
        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            titles.forEach {
                val provinceBounds = RectF()
                val textBounds = Rect()
                //Find title bounds
                paint.getTextBounds(it.value ?: "", 0, it.value?.length ?: 0, textBounds)
                //Find province bounds
                it.key?.computeBounds(provinceBounds, true)
                //Draw text on center of province
                canvas?.drawText(
                    it.value ?: ""
                    , provinceBounds.centerX().minus(textBounds.width().div(2))
                    , provinceBounds.centerY(), paint
                )

            }
        }
    }

}