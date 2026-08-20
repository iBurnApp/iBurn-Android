package com.gaiagps.iburn.fragment

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AutoCompleteTextView
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.gaiagps.iburn.DateUtil
import com.gaiagps.iburn.R
import com.gaiagps.iburn.database.Camp
import com.gaiagps.iburn.database.CampWithUserData
import com.gaiagps.iburn.database.DataProvider
import com.gaiagps.iburn.database.DataProvider.Companion.getInstance
import com.gaiagps.iburn.database.Embargo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.IOException
import kotlin.coroutines.CoroutineContext

class WelcomeFragment : Fragment(), SurfaceTextureListener {
    // Welcome 1 - Show video
    private var mediaPlayer: MediaPlayer? = null
    private var textureView: TextureView? = null
    private var surface: Surface? = null

    // Welcome 3 - Set Home
    private var campSearchView: AutoCompleteTextView? = null

    private var performedEntranceAnimation = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireArguments()
        val rootView =
            inflater.inflate(args.getInt(LAYOUT_ID, -1), container, false) as ViewGroup

        if (args.getInt(LAYOUT_ID, -1) == R.layout.welcome_fragment1) {
            // Intro video
            textureView = rootView.findViewById<TextureView>(R.id.video)
            textureView!!.setSurfaceTextureListener(this)
        } else if (args.getInt(LAYOUT_ID, -1) == R.layout.welcome_fragment2) {
            run {
                val dayFormatter = DateUtil.getPlayaTimeFormat("EEEE MMMM d")
                dayFormatter.setTimeZone(DateUtil.PLAYA_TIME_ZONE)
                val embargoDate = dayFormatter.format(Embargo.LOCATION_EMBARGO_DATE)
                (rootView.findViewById<View?>(R.id.content) as TextView).setText(
                    getString(
                        R.string.location_data_notice,
                        embargoDate
                    )
                )
            }
        }
        if (requireArguments().getInt(LAYOUT_ID, -1) == R.layout.welcome_fragment3) {
            // Set Home location
            val brcView = rootView.findViewById<ImageView>(R.id.parallax0)
            val brcMap = brcView.getDrawable().mutate()
            brcMap.setTint(rootView.getContext().getColor(R.color.regular_text))
            brcView.setImageDrawable(brcMap)
            campSearchView = rootView.findViewById<AutoCompleteTextView>(R.id.campNameSearch)
            campSearchView!!.threshold = 1  // Start filtering after 1 character
            campSearchView!!.setAdapter<CampAutoCompleteAdapter?>(
                CampAutoCompleteAdapter(
                    requireActivity()
                )
            )
            campSearchView!!.onItemClickListener =
                OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                    val selectedCamp =
                        (campSearchView!!.adapter.getItem(position) as CampWithUserData)
                    if (!selectedCamp.item.hasLocation() && !selectedCamp.item.hasUnofficialLocation()) {
                        rootView.findViewById<View?>(R.id.error).setVisibility(View.VISIBLE)
                        campSearchView!!.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        if (getActivity() is HomeCampSelectionListener) {
                            (getActivity() as HomeCampSelectionListener).onHomeCampSelected(null)
                        }
                        return@OnItemClickListener
                    } else {
                        rootView.findViewById<View?>(R.id.error).setVisibility(View.GONE)
                        campSearchView!!.setCompoundDrawablesWithIntrinsicBounds(
                            0,
                            0,
                            R.drawable.ic_check_green_24dp,
                            0
                        )
                    }

                    campSearchView!!.setTag(selectedCamp)
                    Timber.d("Item selected %s", campSearchView!!.getText().toString())

                    val inputManager =
                        requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(
                        campSearchView!!.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS
                    )
                    if (getActivity() is HomeCampSelectionListener) {
                        (getActivity() as HomeCampSelectionListener).onHomeCampSelected(selectedCamp)
                    }
                }
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()

        val isWelcome1 = requireArguments().getInt(LAYOUT_ID, -1) == R.layout.welcome_fragment1
        if (isWelcome1 && !performedEntranceAnimation) {
            val heading = requireView().findViewById<View>(R.id.heading)
            heading.setAlpha(0f)
            val fadeIn = ValueAnimator.ofFloat(0f, 1f)
            fadeIn.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator? ->
                heading.setAlpha(
                    (animation!!.getAnimatedValue() as kotlin.Float?)!!
                )
            })
            fadeIn.setStartDelay(1000)
            fadeIn.setDuration((1 * 1000).toLong())
            fadeIn.start()

            val yearReveal = requireView().findViewById<ImageView>(R.id.year_reveal)
            yearReveal.postDelayed({
                (yearReveal.drawable as? Animatable)?.start()
            }, 2000L)
            performedEntranceAnimation = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
        }

        if (surface != null) {
            surface!!.release()
        }
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        val surface = Surface(surfaceTexture)

        try {
            val descriptor = requireActivity().getAssets().openFd("mp4/onboarding_loop_final.mp4")
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setDataSource(
                descriptor.getFileDescriptor(),
                descriptor.getStartOffset(),
                descriptor.getLength()
            )
            mediaPlayer!!.setSurface(surface)
            mediaPlayer!!.prepare()
            scaleTextureView(textureView!!)
            mediaPlayer!!.start()
            mediaPlayer!!.setLooping(true)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Error preparing video")
        } catch (e: SecurityException) {
            Timber.e(e, "Error preparing video")
        } catch (e: IOException) {
            Timber.e(e, "Error preparing video")
        } catch (e: IllegalStateException) {
            Timber.e(e, "Error preparing video")
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        //unused
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        //unused
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        //unused
    }

    private fun scaleTextureView(textureView: TextureView) {
        // TODO : Do this properly. We're assuming a ~9:16 portrait screen ratio
        textureView.setScaleX(1.78f)
        textureView.requestLayout()
        textureView.invalidate()
    }

    private inner class CampAutoCompleteAdapter(context: Context) : BaseAdapter(), Filterable {
        private var camps: MutableList<CampWithUserData>? = null
        private val dataProvider: DataProvider
        private var filter: CampNameFilter? = null
        var inflater: LayoutInflater?

        init {
            inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
            this.dataProvider = getInstance(context.getApplicationContext())
        }

        fun changeData(camps: MutableList<CampWithUserData>?) {
            this.camps = camps
        }

        override fun getCount(): Int {
            return if (camps == null) 0 else camps!!.size
        }

        override fun getItem(position: Int): CampWithUserData? {
            if (camps == null) return null
            return camps!!.get(position)
        }

        override fun getItemId(position: Int): Long {
            if (camps == null) return -1
            return camps!!.get(position).item.id.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var convertView = convertView
            if (convertView == null) {
                convertView = TextView(getActivity())
                convertView.setPadding(16, 16, 16, 16)
                convertView.setTextSize(16f)
                convertView.setTextAppearance(getActivity(), R.style.PlayaTextItem)
            }

            if (camps != null) {
                val camp = camps!!.get(position)
                (convertView as TextView).setText(camp.item.name)
            }

            return convertView
        }

        override fun getFilter(): Filter {
            if (filter == null) {
                filter = CampNameFilter()
            }
            return filter!!
        }

        private inner class CampNameFilter : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                Timber.d(
                    "Perform filtering with constraint %s",
                    if (constraint == null) "None" else constraint.toString()
                )

                val r = FilterResults()

                if (constraint != null) {
                    val query = constraint.toString() // '%' + constraint.toString() + '%';
                    val camps = runBlocking {
                        dataProvider.observeCampsByName(query).first().also {
                            Timber.d("Got ${it.size} camps for query $query")
                        }
                    }

                    r.values = camps
                    r.count = camps.size
                }
                return r
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                Timber.d(
                    "Publish %d result for %s",
                    if (results.values == null) 0 else (results.values as MutableList<Camp?>).size,
                    if (constraint == null) "None" else constraint.toString()
                )

                if (results.values == null || results.count > 0) {
                    Timber.d("Publishing results to adapter")
                    changeData(results.values as MutableList<CampWithUserData>?)
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }

            override fun convertResultToString(result: Any?): CharSequence? {
                if (result is CampWithUserData) {
                    return result.item.name
                }
                return super.convertResultToString(result)
            }
        }
    }

    interface HomeCampSelectionListener {
        fun onHomeCampSelected(homeCamp: CampWithUserData?)
    }

    companion object {
        const val LAYOUT_ID: String = "layoutid"

        @JvmStatic
        fun newInstance(layoutId: Int): WelcomeFragment {
            val pane = WelcomeFragment()
            val args = Bundle()
            args.putInt(LAYOUT_ID, layoutId)
            pane.setArguments(args)
            return pane
        }
    }
}
