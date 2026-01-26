package ani.dantotsu.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.databinding.BottomSheetForksBinding

class ForksDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetForksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetForksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val items = listOf(
            DeveloperItem.Dev(
                Developer(
                    "Awery",
                    "https://avatars.githubusercontent.com/u/92123190?v=4",
                    "MrBoomDeveloper",
                    "https://github.com/MrBoomDeveloper/Awery"
                )
            )
        )
        
        binding.forksRecyclerView.adapter = DevelopersAdapter(items)
        binding.forksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
