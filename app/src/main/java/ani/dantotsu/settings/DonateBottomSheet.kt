package ani.dantotsu.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.DialogDonateBinding
import ani.dantotsu.snackString

class DonateBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogDonateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDonateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Become a Supporter - opens web checkout
        binding.becomeSupporterCard.setOnClickListener {
            openCheckout()
        }
    }

    private fun openCheckout() {
        // Get the current user's Anilist ID
        val userId = Anilist.userid?.toString() ?: ""
        
        // Build checkout URL with UID so the server knows who is paying
        val checkoutUrl = "https://redantotsu-checkout.vercel.app/pricing?uid=$userId"
        
        // Open in system browser (Chrome/Safari)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))
        startActivity(browserIntent)
        
        snackString(getString(R.string.opening_checkout_page))
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): DonateBottomSheet {
            return DonateBottomSheet()
        }
    }
}
