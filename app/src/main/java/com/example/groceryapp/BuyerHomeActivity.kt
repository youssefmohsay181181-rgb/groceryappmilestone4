package com.example.groceryapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.groceryapp.databinding.ActivityBuyerHomeBinding

/**
 * Buyer home: hosts the buyer fragments behind a bottom navigation bar.
 *
 *  - Shops tab   -> [ShopsFragment] (grid of shops); tapping a shop opens [ProductsFragment] on top of it
 *  - Cart tab    -> [CartFragment]
 *  - Orders tab  -> [MyOrdersFragment] (cancel with dialog + undo Snackbar)
 *  - Profile tab -> opens [ProfileActivity]
 */
class BuyerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuyerHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuyerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_shops -> { showRoot(ShopsFragment()); true }
                R.id.nav_cart -> { showRoot(CartFragment()); true }
                R.id.nav_orders -> { showRoot(MyOrdersFragment()); true }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false // keep the previous tab highlighted
                }
                else -> false
            }
        }
        // Tapping "Shops" while inside a shop goes back to the shop list.
        binding.bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_shops) {
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        }

        // "Shops" is already the selected tab, so selecting it again would not fire the listener:
        // show the first screen directly.
        if (savedInstanceState == null) showRoot(ShopsFragment())

        // Android 13+: ask once so the "Order is on the way" notification can be shown.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    /** Replaces the current screen with a top-level tab fragment. */
    private fun showRoot(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /** Opens a shop's products page on top of the shops list (Back returns to the list). */
    fun openShop(shopId: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ProductsFragment.newInstance(shopId))
            .addToBackStack("shop")
            .commit()
    }

    /** Switches to a bottom-navigation tab (R.id.nav_shops / R.id.nav_cart). */
    fun selectTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }
}
