package com.example.damasuz.fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.damasuz.R
import com.example.damasuz.databinding.FragmentSignBinding


class Youtube{
    var link:String? = null
}

class SignFragment : Fragment() {
    lateinit var binding: FragmentSignBinding

    lateinit var firebaseFirestore: FirebaseFirestore

    var youtube:Youtube? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignBinding.inflate(layoutInflater)

        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle("Ogohlantirish")
        alertDialog.setMessage("Assalomu alaykum! Iltimos dastur to'g'ri ishlashi uchun GPS va internetni yoqishingizni so'raymiz... Dastur Qosimjon Nosirjonov tomonidan ishlab chiqildi va test rejimiga qo'yildi. Murojaat uchun @nosirjonov_v tegeram orqali")
        alertDialog.setPositiveButton("Ok", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                Toast.makeText(context, "Iltimos GPS va internetni aniq ishlayotganiga ishonch hosil qiling, agar dastur to'g'ri ishlamasa", Toast.LENGTH_SHORT).show()
            }
        })
        alertDialog.show()
        firebaseFirestore = FirebaseFirestore.getInstance()

        firebaseFirestore.collection("youtube")
            .get()
            .addOnCompleteListener{
                if (it.isSuccessful){
                    val result = it.result
                    result?.forEach {queryDocumentSnapshot ->
                        youtube = queryDocumentSnapshot.toObject(Youtube::class.java)
                    }
                }
            }

        binding.tvMore.setOnClickListener {
            if (youtube!=null){
                val url = youtube?.link
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }else{
                Toast.makeText(context, "Internetga qayta ulaning va yana bir bosib ko'ring", Toast.LENGTH_SHORT).show()
            }

        }

        binding.btnNext1.setOnClickListener {

            when (binding.radG1.checkedRadioButtonId) {
                R.id.rad_yolovchi -> {
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.registerYolocvhiFragment)
                }
                R.id.rad_haydovchi-> {
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.registerShopirFragment)
                }
            }
        }

        return binding.root
    }
}