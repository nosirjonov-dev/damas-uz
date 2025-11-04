package com.example.damasuz.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import com.example.damasuz.R
import com.example.damasuz.databinding.FragmentRegisterYolocvhiBinding
import com.example.damasuz.models.Yolovchi
import com.example.damasuz.utils.MySharedPrefarance
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*

class RegisterYolocvhiFragment : Fragment() {

    lateinit var binding: FragmentRegisterYolocvhiBinding
    companion object {
        const val GOOGLE_SIGN_IN = 1903
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    lateinit var yList:ArrayList<Yolovchi>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterYolocvhiBinding.inflate(layoutInflater)

        firebaseDatabase = FirebaseDatabase.getInstance()
        referenceYolovchi = firebaseDatabase.getReference("yolovchi")

        binding.linY.visibility = View.GONE
        binding.progress.visibility = View.VISIBLE

        referenceYolovchi.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                yList = ArrayList()
                val children = snapshot.children
                for (child in children) {
                    val value = child.getValue(Yolovchi::class.java)
                    if (value != null) {
                        yList.add(value)
                        if (value.id==auth.uid){
//                            findNavController().popBackStack()
//                            findNavController().navigate(R.id.liniyaListYolovchiFragment, bundleOf("keyYolovchi" to value))
                        }
                    }
                }
                binding.linY.visibility = View.VISIBLE
                binding.progress.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Iltimos internetga qayta ulaning...", Toast.LENGTH_SHORT).show()
            }
        })

        MySharedPrefarance.init(context)
        if (MySharedPrefarance.name!="")
            binding.edtName.setText(MySharedPrefarance.name)
        if (MySharedPrefarance.number!="")
            binding.edtNumber.setText(MySharedPrefarance.number)

        binding.edtName.addTextChangedListener {
            MySharedPrefarance.name = it.toString()
        }
        binding.edtNumber.addTextChangedListener {
            MySharedPrefarance.number = it.toString()
        }

        return binding.root
    }

    lateinit var yolovchi: Yolovchi
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var referenceYolovchi:DatabaseReference

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), getGSO())

        binding.btnNextYol.setOnClickListener {
            var name = binding.edtName.text.toString().trim()
            var number = binding.edtNumber.text.toString().trim()

            if (name!="" && number!=""){
                yolovchi = Yolovchi()
                yolovchi.name = name
                yolovchi.number = number
                Toast.makeText(context, "Google Sign-In boshlanmoqda...", Toast.LENGTH_SHORT).show()
                signIn()
            }else{
                Toast.makeText(context, "Ma'lumotlarni to'liq va to'g'ri kiriting...", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun signIn() {
        try {
            val signInIntent = googleSignInClient.signInIntent
            Toast.makeText(context, "signInIntent ishga tushdi", Toast.LENGTH_SHORT).show()
            startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
        } catch (e: Exception) {
            Toast.makeText(context, "signIn() xato: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("GOOGLE_SIGNIN", "signIn() xato", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN) {
            Toast.makeText(context, "onActivityResult chaqirildi", Toast.LENGTH_SHORT).show()
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Toast.makeText(context, "Account olindi: ${account.email}", Toast.LENGTH_SHORT).show()
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(
                    context,
                    "ApiException: ${e.statusCode} ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("GOOGLE_SIGNIN", "ApiException", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Toast.makeText(context, "Firebase bilan tasdiqlanmoqda...", Toast.LENGTH_SHORT).show()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Firebase auth muvaffaqiyatli ✅", Toast.LENGTH_SHORT).show()
                    yolovchi.id = auth.uid
                    referenceYolovchi.child(yolovchi.id!!).setValue(yolovchi)
                    findNavController().navigate(
                        R.id.liniyaListYolovchiFragment,
                        bundleOf("keyYolovchi" to yolovchi)
                    )
                } else {
                    Toast.makeText(
                        context,
                        "Firebase auth muvaffaqiyatsiz ❌: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("FIREBASE_AUTH", "Error", task.exception)
                }
            }
    }

    private fun getGSO(): GoogleSignInOptions {
        Toast.makeText(context, "GSO yaratildi", Toast.LENGTH_SHORT).show()
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
}