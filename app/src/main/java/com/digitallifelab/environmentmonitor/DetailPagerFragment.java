package com.digitallifelab.environmentmonitor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class DetailPagerFragment extends Fragment {

    private Adapter     mPageAdapter;
    private long        current_id;

    public DetailPagerFragment() {
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.detail_pager_fragment, container, false);

        Bundle arguments = getArguments();

        if (arguments != null) {
            current_id = arguments.getLong(MainActivity.KEY_POINT_ID);
        }else{
            current_id = -1;
        }

        InitTabInterface(rootView);
        return rootView;
    }

    private void InitTabInterface(View rootView)
    {

        ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.view_pager);

        if (viewPager != null) {

            mPageAdapter = new Adapter(getChildFragmentManager(), getActivity());

            Bundle arguments = new Bundle();
            arguments.putLong(MainActivity.KEY_POINT_ID, current_id);

            //Init fragment pages
            DetailPointActivityFragment fragmentPointDetail = new DetailPointActivityFragment();
            fragmentPointDetail.setArguments(arguments);
            mPageAdapter.addFragment(fragmentPointDetail, getActivity().getString(R.string.tab_detail), R.drawable.ic_details_white_24dp);

            MessagesFragment messagesFragment = new MessagesFragment();
            messagesFragment.setArguments(arguments);
            mPageAdapter.addFragment(messagesFragment, getActivity().getString(R.string.tab_messages), R.drawable.ic_message_white_24dp);

            viewPager.setAdapter(mPageAdapter);

            TabLayout tabLayout = (TabLayout) getActivity().findViewById(R.id.tab_layout);

            if(tabLayout == null){
                tabLayout = (TabLayout) rootView.findViewById(R.id.tab_layout);
            }

            tabLayout.setupWithViewPager(viewPager);

            for (int i = 0; i < tabLayout.getTabCount(); i++) {
                TabLayout.Tab tab = tabLayout.getTabAt(i);
                tab.setCustomView(mPageAdapter.getTabView(i));
            }

        }
    }

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment>    mFragments = new ArrayList<>();
        private final List<String>      mFragmentTitles = new ArrayList<>();
        private final List<Integer>     mIconsArray = new ArrayList<>();

        Context context;
        public Adapter(FragmentManager fm, Context context) {
            super(fm);

            this.context = context;
        }

        public void addFragment(Fragment fragment, String title, int icon_id) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
            mIconsArray.add(icon_id);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            //return Titles[position];
            Drawable image = context.getResources().getDrawable(mIconsArray.get(position));
            image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
            //image.setBounds(0, 0, 64, 64);
            ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
            SpannableString sb = new SpannableString(" ");
            sb.setSpan(imageSpan, 0, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


            return sb;
        }

        public View getTabView(int position) {
            // Given you have a custom layout in `res/layout/custom_tab.xml` with a TextView and ImageView
            View v = LayoutInflater.from(context).inflate(R.layout.custom_tab, null);
            TextView tv = (TextView) v.findViewById(R.id.tabText);
            tv.setText(mFragmentTitles.get(position));
            ImageView img = (ImageView) v.findViewById(R.id.tabImage);
            img.setImageResource(mIconsArray.get(position));
            return v;
        }

    }
}
