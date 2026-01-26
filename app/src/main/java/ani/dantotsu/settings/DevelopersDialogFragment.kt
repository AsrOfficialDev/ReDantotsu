package ani.dantotsu.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.R
import ani.dantotsu.connections.github.Contributors
import ani.dantotsu.databinding.BottomSheetDevelopersBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DevelopersDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetDevelopersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDevelopersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.devsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        viewLifecycleOwner.lifecycleScope.launch {
            val sections = withContext(Dispatchers.IO) {
                Contributors().getContributorSections()
            }
            
            // Build the items list with sections
            val items = mutableListOf<DeveloperItem>()
            
            // Add ReDantotsu section
            items.add(DeveloperItem.Section(getString(R.string.redantotsu_section)))
            sections.redantotsuDevs.forEach {
                items.add(DeveloperItem.Dev(it))
            }
            
            // Add Dantotsu section
            items.add(DeveloperItem.Section(getString(R.string.dantotsu_section)))
            sections.dantotsuDevs.forEach {
                items.add(DeveloperItem.Dev(it))
            }
            
            binding.devsRecyclerView.adapter = DevelopersAdapter(items)
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
