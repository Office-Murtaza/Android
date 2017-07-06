package com.smartmux.videodownloader.fragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.smartmux.videodownloader.R;

public class BaseContainerFragment extends Fragment {

	 public void replaceFragment(Fragment fragment, boolean addToBackStack) {
	  FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
	  if (addToBackStack) {
	     transaction.addToBackStack(null);
	  }
	  transaction.replace(R.id.container_framelayout, fragment);
	  transaction.commit();
	  getChildFragmentManager().executePendingTransactions();
	 }
	 
	 public boolean popFragment() {
	  Log.e("Ritesh", "pop fragment: " + getChildFragmentManager().getBackStackEntryCount());
	  boolean isPop = false;
	  if (getChildFragmentManager().getBackStackEntryCount() > 0) {
	   isPop = true;
	   getChildFragmentManager().popBackStack();
	  }
	  return isPop;
	 }
	 
	}
