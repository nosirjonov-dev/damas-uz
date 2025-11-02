package com.example.damasuz.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.example.damasuz.R
import com.example.damasuz.adapters.LiniyaAdapter
import com.example.damasuz.databinding.FragmentLiniyaListYolovchiBinding
import com.example.damasuz.models.Liniya
import com.example.damasuz.models.SHopir
import com.example.damasuz.models.Yolovchi
import com.google.firebase.database.*

class LiniyaListYolovchiFragment : Fragment() {
    lateinit var binding: FragmentLiniyaListYolovchiBinding

    lateinit var liniyaList: ArrayList<Liniya>
    lateinit var yolovchi: Yolovchi

    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var referenceLiniya: DatabaseReference
    lateinit var referenceShopir: DatabaseReference


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLiniyaListYolovchiBinding.inflate(layoutInflater)

        firebaseDatabase = FirebaseDatabase.getInstance()
        referenceLiniya = firebaseDatabase.getReference("liniya")
        referenceShopir = firebaseDatabase.getReference("shopir")

        binding.progressLiniya.visibility = View.VISIBLE
        yolovchi = arguments?.getSerializable("keyYolovchi") as Yolovchi

        referenceLiniya.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                liniyaList = ArrayList<Liniya>()

                val children = snapshot.children
                for (child in children) {
                    val value = child.getValue(Liniya::class.java)
                    if (value != null) {
                        liniyaList.add(value)
                    }
                }
                binding.progressLiniya.visibility = View.GONE
                if (liniyaList.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                }

                referenceShopir.addValueEventListener(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val shList = ArrayList<SHopir>()
                        val children1 = snapshot.children
                        for (ch in children1) {
                            val value = ch.getValue(SHopir::class.java)
                            if (value!=null){
                                shList.add(value)
                            }
                        }
                        val liniyaAdapter = LiniyaAdapter(liniyaList, shList, object : LiniyaAdapter.OnCLick{
                            override fun rootCLick(liniya: Liniya) {
                                findNavController().navigate(R.id.mapYolovchiFragment, bundleOf("keyYolovchi" to yolovchi, "keyLiniya" to liniya))
                            }
                        })
                        binding.rvLiniya.adapter = liniyaAdapter
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Iltimos internetga qayta ulaning...", Toast.LENGTH_SHORT).show()
            }
        })
        return binding.root
    }
}