package com.example.damasuz.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import com.example.damasuz.R
import com.example.damasuz.databinding.FragmentRegisterShopirBinding
import com.example.damasuz.models.SHopir
import com.example.damasuz.utils.MySharedPrefarance
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.database.*
import java.util.concurrent.TimeUnit

class RegisterShopirFragment : Fragment() {
    lateinit var binding: FragmentRegisterShopirBinding

    lateinit var auth: FirebaseAuth
    private val TAG = "PhoneActivity"
    lateinit var storedVerificationId:String
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var referenceHaydovchi:DatabaseReference
    lateinit var referenceAdmin:DatabaseReference
    var shopir: SHopir? = null
    lateinit var shList:ArrayList<SHopir>
    var hasAdminNumber:String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterShopirBinding.inflate(layoutInflater)

        auth = FirebaseAuth.getInstance()
        auth.setLanguageCode("uz")

        binding.lin1.visibility = View.INVISIBLE
        binding.progress.visibility = View.VISIBLE

        firebaseDatabase = FirebaseDatabase.getInstance()
        referenceHaydovchi = firebaseDatabase.getReference("shopir")
        referenceAdmin = firebaseDatabase.getReference("admin")

        MySharedPrefarance.init(context)
        if (MySharedPrefarance.number!="")
            binding.edtPhoneNumber.setText(MySharedPrefarance.number)

        binding.edtPhoneNumber.addTextChangedListener {
            MySharedPrefarance.number = it.toString()
        }

        referenceAdmin.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val children = snapshot.children
                for (child in children) {
                    if (child.key == "number")
                        hasAdminNumber = child.value as String
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        referenceHaydovchi.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val children = snapshot.children
                shList = ArrayList()
                for (child in children) {
                    val value = child.getValue(SHopir::class.java)
                    if (value!=null){
                        shList.add(value)
                    }
                }
                binding.progress.visibility = View.GONE
                binding.lin1.visibility = View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Iltimos internetga qayta ulaning...", Toast.LENGTH_SHORT).show()
            }
        })

        binding.btnNextSh.setOnClickListener {
            val phoneNumber = binding.edtPhoneNumber.text.toString()
            for (sHopir in shList) {
                if (sHopir.phoneNumber==phoneNumber){
                    shopir = sHopir
                }
            }
            if (shopir!=null){
                if (phoneNumber==hasAdminNumber){
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.liniyaListShopirFragment, bundleOf("keyShopir" to shopir))
                }
                if (auth.currentUser!=null && MySharedPrefarance.code!!){
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.liniyaListShopirFragment, bundleOf("keyShopir" to shopir))
                }else {
                    sendVerificationCode(phoneNumber)
                    binding.tvInfoSms.visibility = View.VISIBLE
                    binding.edtCode.visibility = View.VISIBLE
                    binding.tvInfoSms.text =
                        "${shopir?.name} \n $phoneNumber raqamiga SMS xabari yuborildi. Kodni kiriting va avtomatik ro'yhatdan o'tishni kutib turing"
                }
            }else{
                Toast.makeText(context, "Siz $phoneNumber orqali hali hech qaysi liniyaga qo'shilmagansiz", Toast.LENGTH_LONG).show()
            }
        }

        // kelgan sms codni avtomatik o'qib olish
        binding.edtCode.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                verifiyCode()
            }
            true
        }

        //codni qo'lda yozish
        binding.edtCode.addTextChangedListener {
            if (it.toString().length == 6){
                verifiyCode()
            }
        }

        return binding.root
    }


    private fun verifiyCode() {
        val code = binding.edtCode.text.toString()
        if (code.length == 6){
            val credential = PhoneAuthProvider.getCredential(storedVerificationId, code)
            signInWithPhoneAuthCredential(credential)
        }
    }
    fun sendVerificationCode(phoneNumber:String){
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(requireActivity())                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d(TAG, "onVerificationCompleted:$credential")
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.w(TAG, "onVerificationFailed", e)

            if (e is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
            } else if (e is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
            }

            // Show a message and update the UI
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d(TAG, "onCodeSent:$verificationId")

            // Save verification ID and resending token so we can use them later
            storedVerificationId = verificationId
            resendToken = token
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")

                    referenceHaydovchi
                    findNavController().popBackStack()
                    MySharedPrefarance.code = true
                    Toast.makeText(context, "Muvaffaqiyatli", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.liniyaListShopirFragment, bundleOf("keyShopir" to shopir))

                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(context, "Muvaffaqiyatsiz!!!", Toast.LENGTH_SHORT).show()
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        Toast.makeText(context, "Kod xato kiritildi", Toast.LENGTH_SHORT).show()
                    }
                    // Update UI
                }
            }
    }

}