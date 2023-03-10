package com.ashlikun.vlayout.simple;

import static com.ashlikun.vlayout.layout.FixLayoutHelper.TOP_RIGHT;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.ashlikun.vlayout.DelegateAdapter;
import com.ashlikun.vlayout.DelegateAdapter.Adapter;
import com.ashlikun.vlayout.LayoutHelper;
import com.ashlikun.vlayout.VirtualLayoutManager;
import com.ashlikun.vlayout.layout.FixLayoutHelper;
import com.ashlikun.vlayout.layout.GridLayoutHelper;
import com.ashlikun.vlayout.layout.LinearLayoutHelper;
import com.ashlikun.vlayout.layout.StickyLayoutHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by longerian on 2017/11/14.
 *
 * @author longerian
 * @date 2017/11/14
 */

public class DebugActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.main_view);
        VirtualLayoutManager virtualLayoutManager = new VirtualLayoutManager(this);
        DelegateAdapter delegateAdapter = new DelegateAdapter(virtualLayoutManager);
        List<Adapter> adapterList = new ArrayList<>();
        adapterList.add(new SubAdapter(new LinearLayoutHelper(20), 20));
        adapterList.add(new SubAdapter(new StickyLayoutHelper(true), 1));
        adapterList.add(new SubAdapter(new LinearLayoutHelper(20), 20));
        adapterList.add(new SubAdapter(new GridLayoutHelper(4), 80));
        // adapterList.add(new SubAdapter(new FixLayoutHelper(0, 0), 1));
        adapterList.add(new SubAdapter(new FixLayoutHelper(TOP_RIGHT, 0, 0), 1));
        delegateAdapter.addAdapters(adapterList);
        recyclerView.setLayoutManager(virtualLayoutManager);
        recyclerView.setAdapter(delegateAdapter);
    }

    private static class SubAdapter extends Adapter<SubViewHolder> {

        private LayoutHelper mLayoutHelper;
        private int mItemCount;

        private SubAdapter(LayoutHelper layoutHelper, int itemCount) {
            mLayoutHelper = layoutHelper;
            mItemCount = itemCount;
        }

        @Override
        public LayoutHelper onCreateLayoutHelper() {
            return mLayoutHelper;
        }

        @Override
        public SubViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new SubViewHolder(inflater.inflate(R.layout.item, parent, false));
        }

        @Override
        public void onBindViewHolder(SubViewHolder holder, int position) {
            // do nothing
        }

        @Override
        protected void onBindViewHolderWithOffset(SubViewHolder holder, int position, int offsetTotal) {
            super.onBindViewHolderWithOffset(holder, position, offsetTotal);
            holder.setText(String.valueOf(offsetTotal));
            holder.itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }

    }

    private static class SubViewHolder extends RecyclerView.ViewHolder {

        public static volatile int existing = 0;
        public static int createdTimes = 0;

        public SubViewHolder(View itemView) {
            super(itemView);
            createdTimes++;
            existing++;
        }

        public void setText(String title) {
            ((TextView) itemView.findViewById(R.id.title)).setText(title);
        }

        @Override
        protected void finalize() throws Throwable {
            existing--;
            super.finalize();
        }
    }


}
