package com.gaiagps.iburn.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gaiagps.iburn.database.PlayaItem
import com.gaiagps.iburn.databinding.ActivityShareBinding
import com.gaiagps.iburn.util.ShareUrlBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import timber.log.Timber

class ShareActivity : AppCompatActivity() {
    
    companion object {
        private const val EXTRA_PLAYA_ITEM = "playa_item"
        private const val QR_CODE_SIZE = 512
        
        fun createIntent(context: Context, item: PlayaItem): Intent {
            return Intent(context, ShareActivity::class.java).apply {
                putExtra(EXTRA_PLAYA_ITEM, item)
            }
        }
    }
    
    private lateinit var binding: ActivityShareBinding
    private var shareUrl: Uri? = null
    private var itemName: String? = null
    
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
        
        // Get the item from intent
        val item = intent.getParcelableExtra<PlayaItem>(EXTRA_PLAYA_ITEM)
        if (item == null) {
            Toast.makeText(this, "Error: No item to share", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Generate share URL
        shareUrl = ShareUrlBuilder.buildShareUrl(item)
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
    
    private fun generateQRCode(url: String) {
        try {
            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE)
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
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("iBurn Share URL", shareUrl.toString())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}