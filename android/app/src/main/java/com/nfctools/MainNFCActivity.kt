package com.nfctools

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.ReaderCallback
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.github.devnied.emvnfccard.model.EmvCard
import com.github.devnied.emvnfccard.parser.EmvTemplate
import com.nfctools.databinding.ActivityMainNfcactivityBinding
import net.sf.scuba.util.Hex.toHexString
import java.security.AccessController.getContext


class MainNFCActivity : AppCompatActivity(), ReaderCallback {

    lateinit var activity: Activity
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainNfcactivityBinding
    lateinit var mNfcAdapter: NfcAdapter
    var tag: WritableTag? = null
    var tagId: String? = null

    lateinit var nfcManager: com.nfctools.NfcManager
    lateinit var pm: PackageManager

    @SuppressLint("ServiceCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainNfcactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activity = this
        //setSupportActionBar(binding.toolbar)

        pm = packageManager

        nfcManager = NfcManager(this, this)

        val nfcManager = getSystemService(Context.NFC_SERVICE) as android.nfc.NfcManager


        if (pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {

            mNfcAdapter = nfcManager.defaultAdapter

            if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {

                //Yes NFC available
            } else if (mNfcAdapter != null && !mNfcAdapter.isEnabled()) {

                //NFC is not enabled.Need to enable by the user.
                ReadCardByNFC.getInstance().getNfcResultCallbackImpl()
                    .onStatusChanged("NFCNotEnabled")
            } else {
                //NFC is not supported
                ReadCardByNFC.getInstance().getNfcResultCallbackImpl()
                    .onStatusChanged("NFCNotSupported")
            }

        }


    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun enableNfcForegroundDispatch() {

        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            ReadCardByNFC.getInstance().getNfcResultCallbackImpl()
                .onStatusChanged("NFCNotSupported")
            return
        }


        try {
            if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
                val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val nfcPendingIntent =
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                mNfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, null, null)
            } else if (mNfcAdapter != null && !mNfcAdapter.isEnabled()) {

                //NFC is not enabled.Need to enable by the user.
                ReadCardByNFC.getInstance().getNfcResultCallbackImpl()
                    .onStatusChanged("NFCNotEnabled")
            } else {
                //NFC is not supported
                ReadCardByNFC.getInstance().getNfcResultCallbackImpl()
                    .onStatusChanged("NFCNotSupported")
            }

        } catch (ex: IllegalStateException) {
            Log.e("getTag", "Error enabling NFC foreground dispatch", ex)
        }
    }

    private fun disableNfcForegroundDispatch() {

        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            ReadCardByNFC.getInstance().getNfcResultCallbackImpl()
                .onStatusChanged("NFCNotSupported")
            return
        }

        try {

            if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
                mNfcAdapter?.disableForegroundDispatch(this)
            } else if (mNfcAdapter != null && !mNfcAdapter.isEnabled()) {

                //NFC is not enabled.Need to enable by the user.
                ReadCardByNFC.getInstance().getNfcResultCallbackImpl()
                    .onStatusChanged("NFCNotEnabled")
            } else {
                //NFC is not supported
                ReadCardByNFC.getInstance().getNfcResultCallbackImpl()
                    .onStatusChanged("NFCNotSupported")
            }
        } catch (ex: IllegalStateException) {
            Log.e("mNfcAdapter", "Error disabling NFC foreground dispatch", ex)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
        nfcManager.onResume()
    }

    override fun onPause() {
        disableNfcForegroundDispatch()
        nfcManager.onPause()
        super.onPause()
    }

    override fun onTagDiscovered(tag: Tag?) {
        // get IsoDep handle and run xcvr thread
        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            showToast("error")
        } else {

            isoDep.connect()
            val mProvider = MyProvider()
            // Define config
            mProvider.setmTagCom(isoDep)
            val config: EmvTemplate.Config = EmvTemplate.Config()
                .setContactLess(true) // Enable contact less reading (default: true)
                .setReadAllAids(true) // Read all aids in card (default: true)
                .setReadTransactions(true) // Read all transactions (default: true)
                .setReadCplc(false) // Read and extract CPCLC data (default: false)
                .setRemoveDefaultParsers(false) // Remove default parsers for GeldKarte and EmvCard (default: false)
                .setReadAt(true) // Read and extract ATR/ATS and description


            // Create Parser

            val parser = EmvTemplate.Builder() //
                .setProvider(mProvider) // Define provider
                .setConfig(config) // Define config
                //.setTerminal(terminal) (optional) you can define a custom terminal implementation to create APDU
                .build()

            // Read card

            val card: EmvCard = parser.readEmvCard()

            var cardType = "UNKNOWN"
            var cardNumber = "UNKNOWN"
            var expiry = "UNKNOWN"
            var pinStatus = "UNKNOWN"
            var serviceStatus = "UNKNOWN"
            var nfcState = "UNKNOWN"
            var cardAID = "UNKNOWN"

            if (card.type != null) {
                cardType = card.type.toString()
                cardAID = card.type.aid[0]
            }

            if (card.cardNumber != null)
                cardNumber = card.cardNumber

            if (card.track2 != null) {
                val cardExpireDate = toHexString(card.track2.raw).split("D")[1]
                expiry = cardExpireDate.substring(2, 4) + "/" + cardExpireDate.substring(0, 2)
                pinStatus = card.track2.service.serviceCode3.pinRequirements
                serviceStatus = card.track2.service.serviceCode3.allowedServices
            }

            if (card.state != null)
                nfcState = card.state.name


            //val cardHolderName= card.holderFirstname + card.holderLastname


            Log.e("card track2 number", cardNumber)
            Log.e("card expiry", expiry)
            //Log.e("card raw", toHexString(card.track2.raw))
            //Log.e("card service code 1", card.track2.service.serviceCode1.technology)
/*            Log.e(
               "card service code 2", card.track2.service.serviceCode2.authorizationProcessing +
                         card.track2.service.serviceCode2.name
            )*/
            Log.e("card service 3", pinStatus)
            Log.e("card service 3", serviceStatus)
            Log.e("cardnfc State", nfcState)

            var appendData =
                "Card Type: $cardType\nCard Number: $cardNumber \nExpire Date: $expiry\nPIN Status: $pinStatus\nService Status: $serviceStatus\nAID: $cardAID\n" +
                        "CardNfc State: $nfcState"
            ReadCardByNFC.getInstance().getNfcResultCallbackImpl().onStatusChanged(appendData)
            activity.runOnUiThread {
                binding.tvCardDetails.text = appendData
            }
            if (appendData.isNotEmpty()) {
                appendData = ""
                finish()
            }

        }
    }
}