package com.gaiagps.iburn.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gaiagps.iburn.database.ArtWithUserData
import com.gaiagps.iburn.database.CampWithUserData
import com.gaiagps.iburn.database.DataProvider
import com.gaiagps.iburn.database.EventWithUserData
import com.gaiagps.iburn.database.MapPin
import com.gaiagps.iburn.database.PlayaItem
import com.gaiagps.iburn.databinding.ActivityShareBinding
import com.gaiagps.iburn.util.ShareUrlBuilder
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber

class ShareActivity : AppCompatActivity() {

    companion object {
        // Match viewItemDetail scheme: pass type and a stable playaId
        private const val EXTRA_PLAYA_ITEM_PLAYA_ID = "playa-id"
        private const val EXTRA_MAP_PIN = "map-pin"
        private const val QR_CODE_SIZE = 512

        fun createIntent(context: Context, item: PlayaItem): Intent {
            return Intent(context, ShareActivity::class.java).apply {
                // Reuse PlayaItemViewActivity type constants for consistency
                putExtra(
                    PlayaItemViewActivity.EXTRA_PLAYA_ITEM_TYPE, when (item) {
                        is com.gaiagps.iburn.database.Camp -> PlayaItemViewActivity.EXTRA_PLAYA_ITEM_CAMP
                        is com.gaiagps.iburn.database.Art -> PlayaItemViewActivity.EXTRA_PLAYA_ITEM_ART
                        is com.gaiagps.iburn.database.Event -> PlayaItemViewActivity.EXTRA_PLAYA_ITEM_EVENT
                        else -> null
                    }
                )
                putExtra(EXTRA_PLAYA_ITEM_PLAYA_ID, item.playaId)
            }
        }

        fun createIntent(context: Context, pin: MapPin): Intent {
            return Intent(context, ShareActivity::class.java).apply {
                putExtra(EXTRA_MAP_PIN, pin)
            }
        }
    }

    private lateinit var binding: ActivityShareBinding
    private var shareUrl: Uri? = null
    private var itemName: String? = null
    private var playaItemDisposable: io.reactivex.disposables.Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Share"
        }
        // Load from intent: prefer MapPin if present, otherwise PlayaItem
        if (intent.hasExtra(EXTRA_MAP_PIN)) {
            loadMapPinFromIntent(intent)
        } else {
            // Load the item from intent extras (type + playaId) and then populate UI
            loadPlayaItemFromIntent(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "Share")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, this@ShareActivity.javaClass.simpleName)
        }
    }

    private fun loadPlayaItemFromIntent(i: Intent) {
        val type = i.getStringExtra(PlayaItemViewActivity.EXTRA_PLAYA_ITEM_TYPE)
        val playaId = i.getStringExtra(EXTRA_PLAYA_ITEM_PLAYA_ID)
        if (type == null || playaId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No item to share", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        DataProvider.getInstance(applicationContext)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ provider ->
                playaItemDisposable = when (type) {
                    PlayaItemViewActivity.EXTRA_PLAYA_ITEM_CAMP -> provider.observeCampByPlayaId(
                        playaId
                    )
                        .take(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ itemWithUserData: CampWithUserData ->
                            onPlayaItemLoaded(itemWithUserData.item)
                        }, { throwable -> finishWithError(throwable) })

                    PlayaItemViewActivity.EXTRA_PLAYA_ITEM_ART -> provider.observeArtByPlayaId(
                        playaId
                    )
                        .take(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ itemWithUserData: ArtWithUserData ->
                            onPlayaItemLoaded(itemWithUserData.item)
                        }, { throwable -> finishWithError(throwable) })

                    PlayaItemViewActivity.EXTRA_PLAYA_ITEM_EVENT -> provider.observeEventByPlayaId(
                        playaId
                    )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ itemWithUserData: EventWithUserData ->
                            onPlayaItemLoaded(itemWithUserData.item)
                        }, { throwable -> finishWithError(throwable) })

                    else -> {
                        finishWithError(IllegalArgumentException("Unknown PlayaItem type $type"))
                        null
                    }
                }
            }, { throwable -> finishWithError(throwable) })
    }

    private fun onPlayaItemLoaded(item: PlayaItem) {
        // Generate share URL
        shareUrl = ShareUrlBuilder.buildShareUrl(item)//.withDecodedColons()
        itemName = item.name

        // Display item info
        binding.itemTitle.text = item.name
        binding.itemDescription.text = when {
            item.playaAddress != null -> item.playaAddress
            item.playaAddressUnofficial != null -> item.playaAddressUnofficial
            else -> ""
        }

        // Display URL
        binding.shareUrl.text = shareUrl.toString()

        // Generate and display QR code
        generateQRCode(shareUrl.toString())

        // Set up share button
        binding.shareButton.setOnClickListener {
            shareItem()
        }

        // Set up copy URL button
        binding.copyUrlButton.setOnClickListener {
            copyUrlToClipboard()
        }
    }

    private fun finishWithError(throwable: Throwable) {
        Timber.e(throwable, "Failed to load item for sharing")
        Toast.makeText(this, "Error loading item", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun loadMapPinFromIntent(i: Intent) {
        val pin = i.getParcelableExtra<MapPin>(EXTRA_MAP_PIN)
        if (pin == null) {
            finishWithError(IllegalArgumentException("Missing MapPin"))
            return
        }
        onMapPinLoaded(pin)
    }

    private fun onMapPinLoaded(pin: MapPin) {
        shareUrl = ShareUrlBuilder.buildPinShareUrl(pin)
        itemName = pin.title

        binding.itemTitle.text = pin.title
        binding.itemDescription.text = pin.address ?: (pin.description ?: "")

        binding.shareUrl.text = shareUrl.toString()

        generateQRCode(shareUrl.toString())

        binding.shareButton.setOnClickListener {
            shareItem()
        }

        binding.copyUrlButton.setOnClickListener {
            copyUrlToClipboard()
        }
    }

    private fun generateQRCode(url: String) {
        try {
            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix =
                writer.encode(url, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
            binding.qrCodeImage.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            Timber.e(e, "Error generating QR code")
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareItem() {
        shareUrl?.let { url ->
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "$itemName at Burning Man\n$url")
                putExtra(Intent.EXTRA_SUBJECT, itemName)
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }

    private fun copyUrlToClipboard() {
        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("iBurn Share URL", shareUrl.toString())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        playaItemDisposable?.let { if (!it.isDisposed) it.dispose() }
        super.onDestroy()
    }
}
